package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import com.blackduck.integration.detectable.detectables.maven.resolver.exception.ArtifactDownloadException;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaRepository;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages parallel downloads of Maven artifacts with controlled concurrency.
 * Single Responsibility: Orchestrate concurrent artifact downloads safely.
 */
public class ParallelDownloadManager {

    private static final Logger logger = LoggerFactory.getLogger(ParallelDownloadManager.class);
    private static final int DEFAULT_THREAD_COUNT = 5;
    private static final int MAX_THREAD_COUNT = 20;
    private static final int MIN_THREAD_COUNT = 1;

    private final ExecutorService executorService;
    private final LocalRepositoryChecker localChecker;
    private final HttpArtifactDownloader httpDownloader;
    private final int threadCount;

    /**
     * Result of a parallel download operation.
     */
    public static class ParallelDownloadResult {
        private final int totalArtifacts;
        private final int successCount;
        private final int failureCount;
        private final int skippedCount;
        private final List<DownloadOutcome> outcomes;
        private final long totalTimeMs;

        public ParallelDownloadResult(int totalArtifacts, int successCount, int failureCount,
                                     int skippedCount, List<DownloadOutcome> outcomes, long totalTimeMs) {
            this.totalArtifacts = totalArtifacts;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.skippedCount = skippedCount;
            this.outcomes = new ArrayList<>(outcomes);
            this.totalTimeMs = totalTimeMs;
        }

        public boolean isCompleteSuccess() {
            return failureCount == 0;
        }

        public int getTotalArtifacts() { return totalArtifacts; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public int getSkippedCount() { return skippedCount; }
        public List<DownloadOutcome> getOutcomes() { return new ArrayList<>(outcomes); }
        public long getTotalTimeMs() { return totalTimeMs; }
    }

    /**
     * Outcome of a single artifact download.
     */
    public static class DownloadOutcome {
        private final ArtifactCoordinate coordinate;
        private final DownloadStatus status;
        private final String source;
        private final Path path;
        private final String errorMessage;
        private final ArtifactDownloadException exception;

        public enum DownloadStatus {
            SUCCESS,
            FAILED,
            SKIPPED_LOCAL,
            SKIPPED_NOT_FOUND
        }

        private DownloadOutcome(ArtifactCoordinate coordinate, DownloadStatus status,
                               String source, Path path, String errorMessage, ArtifactDownloadException exception) {
            this.coordinate = coordinate;
            this.status = status;
            this.source = source;
            this.path = path;
            this.errorMessage = errorMessage;
            this.exception = exception;
        }

        public static DownloadOutcome success(ArtifactCoordinate coordinate, String source, Path path) {
            return new DownloadOutcome(coordinate, DownloadStatus.SUCCESS, source, path, null, null);
        }

        public static DownloadOutcome failed(ArtifactCoordinate coordinate, String errorMessage, ArtifactDownloadException exception) {
            return new DownloadOutcome(coordinate, DownloadStatus.FAILED, null, null, errorMessage, exception);
        }

        public static DownloadOutcome skippedLocal(ArtifactCoordinate coordinate, String source, Path path) {
            return new DownloadOutcome(coordinate, DownloadStatus.SKIPPED_LOCAL, source, path, null, null);
        }

        public static DownloadOutcome skippedNotFound(ArtifactCoordinate coordinate, String message) {
            return new DownloadOutcome(coordinate, DownloadStatus.SKIPPED_NOT_FOUND, null, null, message, null);
        }

        public ArtifactCoordinate getCoordinate() { return coordinate; }
        public DownloadStatus getStatus() { return status; }
        public String getSource() { return source; }
        public Path getPath() { return path; }
        public String getErrorMessage() { return errorMessage; }
        public ArtifactDownloadException getException() { return exception; }
    }

    /**
     * Creates a parallel download manager with specified concurrency.
     *
     * @param threadCount Number of concurrent download threads (null for default)
     * @param localChecker Local repository checker
     * @param httpDownloader HTTP downloader implementation
     */
    public ParallelDownloadManager(Integer threadCount, LocalRepositoryChecker localChecker,
                                  HttpArtifactDownloader httpDownloader) {
        this.threadCount = validateThreadCount(threadCount);
        this.localChecker = localChecker;
        this.httpDownloader = httpDownloader;

        // Create thread pool with custom thread factory for better naming
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "maven-download-" + threadNumber.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };

        this.executorService = new ThreadPoolExecutor(
            this.threadCount,
            this.threadCount,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        logger.info("Parallel download manager initialized with {} threads", this.threadCount);
    }

    /**
     * Downloads artifacts in parallel with controlled concurrency (backward compatibility).
     * Uses only Tier-1 (local) and Tier-3 (Maven Central) resolution.
     *
     * @param dependencies List of dependencies to download
     * @param customRepoPath Custom repository path (optional)
     * @param defaultRepoPath Default repository path
     * @param downloadConfig Download configuration
     * @return Result containing download outcomes
     */
    public ParallelDownloadResult downloadArtifacts(
            List<Dependency> dependencies,
            Path customRepoPath,
            Path defaultRepoPath,
            DownloadConfiguration downloadConfig) {
        return downloadArtifacts(dependencies, customRepoPath, null, defaultRepoPath,
                                new ArrayList<>(), downloadConfig);
    }

    /**
     * Downloads artifacts in parallel with 3-tier resolution support.
     * Resolution order:
     *   Tier-1: Local repositories (custom, then .m2)
     *   Tier-2: POM-declared remote repositories
     *   Tier-3: Maven Central as fallback
     *
     * @param dependencies List of dependencies to download
     * @param customRepoPath Custom repository path (optional)
     * @param homeM2RepoPath User's home ~/.m2/repository path (optional)
     * @param defaultRepoPath Default repository path (download cache)
     * @param pomRepositories List of POM-declared repositories (Tier-2)
     * @param downloadConfig Download configuration
     * @return Result containing download outcomes
     */
    public ParallelDownloadResult downloadArtifacts(
            List<Dependency> dependencies,
            Path customRepoPath,
            Path homeM2RepoPath,
            Path defaultRepoPath,
            List<JavaRepository> pomRepositories,
            DownloadConfiguration downloadConfig) {

        Map<String, HttpArtifactDownloader> downloaders = new LinkedHashMap<>();
        if (pomRepositories != null) {
            for (JavaRepository repo : pomRepositories) {
                if (repo.getUrl() != null && !repo.getUrl().isEmpty()) {
                    downloaders.put(repo.getId(), new RemoteRepositoryDownloader(repo.getUrl()));
                }
            }
        }
        return downloadArtifacts(dependencies, customRepoPath, homeM2RepoPath, defaultRepoPath,
                                 downloaders, downloadConfig);
    }

    /**
     * Downloads artifacts in parallel with pre-built, mirror-aware downloaders for Tier-2 repos.
     *
     * @param dependencies List of dependencies to download
     * @param customRepoPath Custom repository path (optional)
     * @param homeM2RepoPath User's home ~/.m2/repository path (optional)
     * @param defaultRepoPath Default repository path (download cache)
     * @param pomRepoDownloaders Pre-built downloaders keyed by repo label (already mirror-redirected)
     * @param downloadConfig Download configuration
     * @return Result containing download outcomes
     */
    public ParallelDownloadResult downloadArtifacts(
            List<Dependency> dependencies,
            Path customRepoPath,
            Path homeM2RepoPath,
            Path defaultRepoPath,
            Map<String, HttpArtifactDownloader> pomRepoDownloaders,
            DownloadConfiguration downloadConfig) {

        if (dependencies == null || dependencies.isEmpty()) {
            return new ParallelDownloadResult(0, 0, 0, 0, new ArrayList<>(), 0);
        }

        logger.info("========================================");
        logger.info("STARTING PARALLEL ARTIFACT DOWNLOAD");
        logger.info("========================================");
        logger.info("Total artifacts: {}", dependencies.size());
        logger.info("Concurrent threads: {}", threadCount);
        logger.info("POM repositories: {}", pomRepoDownloaders != null ? pomRepoDownloaders.size() : 0);

        long startTime = System.currentTimeMillis();
        List<DownloadOutcome> outcomes = new CopyOnWriteArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);
        AtomicBoolean cancelFlag = new AtomicBoolean(false);

        // Create futures for all downloads
        List<CompletableFuture<DownloadOutcome>> futures = new ArrayList<>();

        for (Dependency dependency : dependencies) {
            CompletableFuture<DownloadOutcome> future = CompletableFuture.supplyAsync(() -> {
                // Check cancellation flag
                if (cancelFlag.get()) {
                    ArtifactCoordinate coord = ArtifactCoordinate.fromAetherArtifact(dependency.getArtifact());
                    return DownloadOutcome.failed(coord, "Download cancelled", null);
                }

                return downloadSingleArtifact(
                    dependency.getArtifact(),
                    customRepoPath,
                    homeM2RepoPath,
                    defaultRepoPath,
                    pomRepoDownloaders,
                    downloadConfig,
                    cancelFlag
                );
            }, executorService);

            // Handle outcome
            future = future.whenComplete((outcome, throwable) -> {
                if (throwable != null) {
                    logger.error("Unexpected error in download task", throwable);
                    ArtifactCoordinate coord = ArtifactCoordinate.fromAetherArtifact(dependency.getArtifact());
                    outcome = DownloadOutcome.failed(coord, "Unexpected error: " + throwable.getMessage(), null);
                }

                if (outcome != null) {
                    outcomes.add(outcome);

                    switch (outcome.getStatus()) {
                        case SUCCESS:
                            successCount.incrementAndGet();
                            logger.info("[{}/{}] SUCCESS: {}",
                                successCount.get() + failureCount.get() + skippedCount.get(),
                                dependencies.size(),
                                outcome.getCoordinate());
                            break;

                        case FAILED:
                            failureCount.incrementAndGet();
                            // Log failure details and continue — do not cancel remaining downloads.
                            // A single artifact missing from a repo should not stop other JARs from being scanned.
                            String failureReason = (outcome.getException() != null)
                                ? "[" + outcome.getException().getCategory() + "] " + outcome.getException().getSanitizedMessage()
                                : (outcome.getErrorMessage() != null ? outcome.getErrorMessage() : "Unknown error");
                            logger.warn("[{}/{}] DOWNLOAD FAILED (continuing with remaining): {} - Reason: {}",
                                successCount.get() + failureCount.get() + skippedCount.get(),
                                dependencies.size(),
                                outcome.getCoordinate(),
                                failureReason);
                            if (outcome.getException() != null) {
                                logger.debug("Failure details for {}:", outcome.getCoordinate(), outcome.getException());
                            }
                            break;

                        case SKIPPED_LOCAL:
                        case SKIPPED_NOT_FOUND:
                            skippedCount.incrementAndGet();
                            logger.info("[{}/{}] SKIPPED: {} - {}",
                                successCount.get() + failureCount.get() + skippedCount.get(),
                                dependencies.size(),
                                outcome.getCoordinate(),
                                outcome.getStatus());
                            break;
                    }
                }
            });

            futures.add(future);
        }

        // Wait for all downloads to complete or fail
        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );

            // Wait with timeout to prevent hanging
            allFutures.get(30, TimeUnit.MINUTES);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Download interrupted");
            cancelFlag.set(true);
            cancelRemainingTasks(futures);
        } catch (ExecutionException e) {
            logger.error("Download execution failed", e.getCause());
            cancelFlag.set(true);
            cancelRemainingTasks(futures);
        } catch (TimeoutException e) {
            logger.error("Download timeout after 30 minutes");
            cancelFlag.set(true);
            cancelRemainingTasks(futures);
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        // Generate result
        ParallelDownloadResult result = new ParallelDownloadResult(
            dependencies.size(),
            successCount.get(),
            failureCount.get(),
            skippedCount.get(),
            outcomes,
            elapsedTime
        );

        // Log summary
        logger.info("PARALLEL DOWNLOAD SUMMARY");
        logger.info("Total processed: {}", result.getTotalArtifacts());
        logger.info("  Successfully downloaded: {}", result.getSuccessCount());
        logger.info("  Skipped: {}", result.getSkippedCount());
        logger.info("  Failed: {}", result.getFailureCount());
        logger.info("Time elapsed: {} ms ({} ms/artifact avg)",
            result.getTotalTimeMs(),
            result.getTotalArtifacts() > 0 ? result.getTotalTimeMs() / result.getTotalArtifacts() : 0);
        return result;
    }

    /**
     * Downloads a single artifact using 3-tier resolution with log buffering.
     * All artifact-specific logs are buffered and flushed atomically to prevent
     * interleaving of log output from concurrent download threads.
     */
    private DownloadOutcome downloadSingleArtifact(
            Artifact artifact,
            Path customRepoPath,
            Path homeM2RepoPath,
            Path defaultRepoPath,
            Map<String, HttpArtifactDownloader> pomRepoDownloaders,
            DownloadConfiguration downloadConfig,
            AtomicBoolean cancelFlag) {

        List<String> messageBuffer = new ArrayList<>();
        DownloadOutcome outcome = doDownloadSingleArtifact(
            artifact, customRepoPath, homeM2RepoPath, defaultRepoPath,
            pomRepoDownloaders, downloadConfig,
            cancelFlag, messageBuffer);

        // Flush buffer atomically to prevent log interleaving between threads
        if (!messageBuffer.isEmpty()) {
            synchronized (logger) {
                for (String line : messageBuffer) {
                    logger.info("[DOWNLOAD] {}", line);
                }
            }
        }

        return outcome;
    }

    /**
     * Internal implementation of single artifact download with buffered logging.
     * Resolution order:
     *   Tier-1: Local repositories (custom, then .m2)
     *   Tier-2: POM-declared remote repositories (pre-built, mirror-aware)
     *   Tier-3: Fallback downloader (Maven Central or mirror)
     */
    private DownloadOutcome doDownloadSingleArtifact(
            Artifact artifact,
            Path customRepoPath,
            Path homeM2RepoPath,
            Path defaultRepoPath,
            Map<String, HttpArtifactDownloader> pomRepoDownloaders,
            DownloadConfiguration downloadConfig,
            AtomicBoolean cancelFlag,
            List<String> messageBuffer) {

        ArtifactCoordinate coordinate = ArtifactCoordinate.fromAetherArtifact(artifact);
        String threadName = Thread.currentThread().getName();

        try {
            // Check cancellation
            if (cancelFlag.get()) {
                return DownloadOutcome.failed(coordinate, "Cancelled", null);
            }

            messageBuffer.add(String.format("[%s] Processing: %s", threadName, coordinate));

            // Check custom repository
            if (customRepoPath != null) {
                Path customJar = localChecker.checkCustomRepository(artifact, customRepoPath);
                if (customJar != null) {
                    messageBuffer.add(String.format("[%s] Found in custom repository: %s", threadName, coordinate));
                    return DownloadOutcome.skippedLocal(coordinate, "custom-repository", customJar);
                }
            }

            // Check home ~/.m2/repository
            if (homeM2RepoPath != null) {
                Path homeJar = localChecker.checkDefaultRepository(artifact, homeM2RepoPath);
                if (homeJar != null) {
                    messageBuffer.add(String.format("[%s] Found in home .m2 repository: %s", threadName, coordinate));
                    return DownloadOutcome.skippedLocal(coordinate, "home-m2-repository", homeJar);
                }
            }

            // Check download cache
            Path defaultJar = localChecker.checkDefaultRepository(artifact, defaultRepoPath);
            if (defaultJar != null) {
                messageBuffer.add(String.format("[%s] Found in download cache: %s", threadName, coordinate));
                return DownloadOutcome.skippedLocal(coordinate, "download-cache", defaultJar);
            }

            // Check cancellation before download
            if (cancelFlag.get()) {
                return DownloadOutcome.failed(coordinate, "Cancelled before download", null);
            }

            Path targetPath = buildRepositoryJarPath(artifact, defaultRepoPath);

            // ========== TIER-2: POM-DECLARED REPOSITORIES ==========
            if (pomRepoDownloaders != null && !pomRepoDownloaders.isEmpty()) {
                messageBuffer.add(String.format("[%s] Checking %d POM repositories for: %s",
                    threadName, pomRepoDownloaders.size(), coordinate));

                for (Map.Entry<String, HttpArtifactDownloader> entry : pomRepoDownloaders.entrySet()) {
                    if (cancelFlag.get()) {
                        return DownloadOutcome.failed(coordinate, "Cancelled during POM repository checks", null);
                    }

                    String repoLabel = entry.getKey();
                    HttpArtifactDownloader remoteDownloader = entry.getValue();

                    try {
                        messageBuffer.add(String.format("[%s] Trying POM repository: %s for %s",
                            threadName, repoLabel, coordinate));

                        DownloadResult pomResult = remoteDownloader.download(
                            artifact, targetPath, downloadConfig
                        );

                        if (pomResult.isSuccess()) {
                            messageBuffer.add(String.format("[%s] Found in POM repository %s: %s",
                                threadName, repoLabel, coordinate));
                            return DownloadOutcome.success(coordinate,
                                "pom-repository:" + repoLabel, pomResult.getDownloadedPath());
                        }
                    } catch (ArtifactDownloadException e) {
                        if (e.getMessage() != null && e.getMessage().contains("404")) {
                            messageBuffer.add(String.format("[%s] Not found in %s: %s", threadName, repoLabel, coordinate));
                        } else {
                            messageBuffer.add(String.format("[%s] Error downloading from %s: %s",
                                threadName, repoLabel, e.getSanitizedMessage()));
                        }
                    } catch (Exception e) {
                        messageBuffer.add(String.format("[%s] Unexpected error with repository %s: %s",
                            threadName, repoLabel, e.getMessage()));
                    }
                }

                messageBuffer.add(String.format("[%s] Not found in any POM repository, falling back to fallback downloader: %s",
                    threadName, coordinate));
            }

            // ========== TIER-3: FALLBACK (MAVEN CENTRAL OR MIRROR) ==========
            messageBuffer.add(String.format("[%s] Downloading from fallback repository: %s", threadName, coordinate));
            DownloadResult result = httpDownloader.download(
                artifact, targetPath, downloadConfig
            );

            if (result.isSuccess()) {
                messageBuffer.add(String.format("[%s] Downloaded successfully: %s", threadName, coordinate));
                return DownloadOutcome.success(coordinate, result.getSource(), result.getDownloadedPath());
            } else {
                if (result.isNotFound()) {
                    return DownloadOutcome.skippedNotFound(coordinate, result.getErrorMessage());
                }
                return DownloadOutcome.failed(coordinate, result.getErrorMessage(), null);
            }

        } catch (ArtifactDownloadException e) {
            messageBuffer.add(String.format("[%s] Download failed for %s: %s", threadName, coordinate, e.getSanitizedMessage()));
            return DownloadOutcome.failed(coordinate, e.getSanitizedMessage(), e);
        } catch (Exception e) {
            messageBuffer.add(String.format("[%s] Unexpected error for %s: %s", threadName, coordinate, e.getMessage()));
            return DownloadOutcome.failed(coordinate, "Unexpected error: " + e.getMessage(), null);
        }
    }

    /**
     * Cancels remaining tasks.
     */
    private void cancelRemainingTasks(List<CompletableFuture<DownloadOutcome>> futures) {
        for (CompletableFuture<DownloadOutcome> future : futures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }

    /**
     * Shuts down the thread pool gracefully.
     */
    public void shutdown() {
        logger.info("Shutting down parallel download manager");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn("Thread pool did not terminate in 60 seconds, forcing shutdown");
                executorService.shutdownNow();
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.error("Thread pool did not terminate after forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Shutdown interrupted", e);
            executorService.shutdownNow();
        }
    }

    private int validateThreadCount(Integer threadCount) {
        if (threadCount == null) {
            return DEFAULT_THREAD_COUNT;
        }

        if (threadCount < MIN_THREAD_COUNT) {
            logger.warn("Thread count {} is below minimum {}, using minimum",
                threadCount, MIN_THREAD_COUNT);
            return MIN_THREAD_COUNT;
        }

        if (threadCount > MAX_THREAD_COUNT) {
            logger.warn("Thread count {} exceeds maximum {}, using maximum",
                threadCount, MAX_THREAD_COUNT);
            return MAX_THREAD_COUNT;
        }

        return threadCount;
    }

    private Path buildRepositoryJarPath(Artifact artifact, Path repositoryPath) {
        ArtifactCoordinate coordinate = ArtifactCoordinate.fromAetherArtifact(artifact);
        String[] pathParts = coordinate.toRepositoryPath().split("/");

        Path result = repositoryPath;
        for (String part : pathParts) {
            result = result.resolve(part);
        }

        return result;
    }
}