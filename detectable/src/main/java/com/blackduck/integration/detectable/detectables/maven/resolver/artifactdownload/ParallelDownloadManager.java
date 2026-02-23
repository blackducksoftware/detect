package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import com.blackduck.integration.detectable.detectables.maven.resolver.exception.ArtifactDownloadException;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaRepository;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
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
            HttpArtifactDownloader.DownloadConfiguration downloadConfig) {
        // Delegate to new method with empty POM repositories
        return downloadArtifacts(dependencies, customRepoPath, defaultRepoPath,
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
     * @param defaultRepoPath Default repository path
     * @param pomRepositories List of POM-declared repositories (Tier-2)
     * @param downloadConfig Download configuration
     * @return Result containing download outcomes
     */
    public ParallelDownloadResult downloadArtifacts(
            List<Dependency> dependencies,
            Path customRepoPath,
            Path defaultRepoPath,
            List<JavaRepository> pomRepositories,
            HttpArtifactDownloader.DownloadConfiguration downloadConfig) {

        if (dependencies == null || dependencies.isEmpty()) {
            return new ParallelDownloadResult(0, 0, 0, 0, new ArrayList<>(), 0);
        }

        // Initialize remote repository downloaders for Tier-2
        Map<String, HttpArtifactDownloader> remoteDownloaders = new HashMap<>();
        if (pomRepositories != null && !pomRepositories.isEmpty()) {
            logger.info("Initializing {} POM repository downloaders for parallel execution", pomRepositories.size());
            for (JavaRepository repo : pomRepositories) {
                if (repo.getUrl() != null && !repo.getUrl().isEmpty()) {
                    remoteDownloaders.put(repo.getUrl(), new RemoteRepositoryDownloader(repo.getUrl()));
                    logger.debug("  Created downloader for: {} ({})", repo.getId(), repo.getUrl());
                }
            }
        }

        logger.info("========================================");
        logger.info("STARTING PARALLEL ARTIFACT DOWNLOAD");
        logger.info("========================================");
        logger.info("Total artifacts: {}", dependencies.size());
        logger.info("Concurrent threads: {}", threadCount);
        logger.info("POM repositories: {}", pomRepositories != null ? pomRepositories.size() : 0);

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
                    defaultRepoPath,
                    pomRepositories,
                    remoteDownloaders,
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
                            logger.info("[{}/{}] ✓ SUCCESS: {}",
                                successCount.get() + failureCount.get() + skippedCount.get(),
                                dependencies.size(),
                                outcome.getCoordinate());
                            break;

                        case FAILED:
                            int failures = failureCount.incrementAndGet();
                            logger.error("[{}/{}] ✗ FAILED: {} - {}",
                                successCount.get() + failures + skippedCount.get(),
                                dependencies.size(),
                                outcome.getCoordinate(),
                                outcome.getErrorMessage());

                            // Cancel remaining tasks on first failure (fail-fast)
                            if (failures == 1) {
                                logger.warn("Cancelling remaining downloads due to failure");
                                cancelFlag.set(true);
                            }
                            break;

                        case SKIPPED_LOCAL:
                        case SKIPPED_NOT_FOUND:
                            skippedCount.incrementAndGet();
                            logger.info("[{}/{}] ⚠ SKIPPED: {} - {}",
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
        logger.info("========================================");
        logger.info("PARALLEL DOWNLOAD SUMMARY");
        logger.info("========================================");
        logger.info("Total processed: {}", result.getTotalArtifacts());
        logger.info("  ✓ Successfully downloaded: {}", result.getSuccessCount());
        logger.info("  ⚠ Skipped: {}", result.getSkippedCount());
        logger.info("  ✗ Failed: {}", result.getFailureCount());
        logger.info("Time elapsed: {} ms ({} ms/artifact avg)",
            result.getTotalTimeMs(),
            result.getTotalArtifacts() > 0 ? result.getTotalTimeMs() / result.getTotalArtifacts() : 0);
        logger.info("========================================");

        return result;
    }

    /**
     * Downloads a single artifact using 3-tier resolution.
     * Resolution order:
     *   Tier-1: Local repositories (custom, then .m2)
     *   Tier-2: POM-declared remote repositories
     *   Tier-3: Maven Central as fallback
     */
    private DownloadOutcome downloadSingleArtifact(
            Artifact artifact,
            Path customRepoPath,
            Path defaultRepoPath,
            List<JavaRepository> pomRepositories,
            Map<String, HttpArtifactDownloader> remoteDownloaders,
            HttpArtifactDownloader.DownloadConfiguration downloadConfig,
            AtomicBoolean cancelFlag) {

        ArtifactCoordinate coordinate = ArtifactCoordinate.fromAetherArtifact(artifact);
        String threadName = Thread.currentThread().getName();

        try {
            // Check cancellation
            if (cancelFlag.get()) {
                return DownloadOutcome.failed(coordinate, "Cancelled", null);
            }

            logger.debug("[{}] Processing: {}", threadName, coordinate);

            // Check custom repository
            if (customRepoPath != null) {
                Path customJar = localChecker.checkCustomRepository(artifact, customRepoPath);
                if (customJar != null) {
                    logger.debug("[{}] Found in custom repository: {}", threadName, coordinate);
                    return DownloadOutcome.skippedLocal(coordinate, "custom-repository", customJar);
                }
            }

            // Check default repository
            Path defaultJar = localChecker.checkDefaultRepository(artifact, defaultRepoPath);
            if (defaultJar != null) {
                logger.debug("[{}] Found in default repository: {}", threadName, coordinate);
                return DownloadOutcome.skippedLocal(coordinate, "m2-repository", defaultJar);
            }

            // Check cancellation before download
            if (cancelFlag.get()) {
                return DownloadOutcome.failed(coordinate, "Cancelled before download", null);
            }

            Path targetPath = buildRepositoryJarPath(artifact, defaultRepoPath);

            // ========== TIER-2: POM-DECLARED REPOSITORIES ==========
            // Try POM repositories before falling back to Maven Central
            if (pomRepositories != null && !pomRepositories.isEmpty()) {
                logger.debug("[{}] Checking {} POM repositories for: {}",
                    threadName, pomRepositories.size(), coordinate);

                for (JavaRepository repo : pomRepositories) {
                    // Check cancellation between repositories
                    if (cancelFlag.get()) {
                        return DownloadOutcome.failed(coordinate, "Cancelled during POM repository checks", null);
                    }

                    try {
                        HttpArtifactDownloader remoteDownloader = remoteDownloaders.get(repo.getUrl());
                        if (remoteDownloader != null) {
                            logger.debug("[{}] Trying POM repository: {} for {}",
                                threadName, repo.getId(), coordinate);

                            HttpArtifactDownloader.DownloadResult pomResult = remoteDownloader.download(
                                artifact, targetPath, downloadConfig
                            );

                            if (pomResult.isSuccess()) {
                                logger.debug("[{}] Found in POM repository {}: {}",
                                    threadName, repo.getId(), coordinate);
                                return DownloadOutcome.success(coordinate,
                                    "pom-repository:" + repo.getId(), pomResult.getPath());
                            }
                        }
                    } catch (ArtifactDownloadException e) {
                        // Log but continue to next repository
                        if (e.getMessage() != null && e.getMessage().contains("404")) {
                            logger.debug("[{}] Not found in {}: {}", threadName, repo.getId(), coordinate);
                        } else {
                            logger.debug("[{}] Error downloading from {}: {}",
                                threadName, repo.getId(), e.getSanitizedMessage());
                        }
                    } catch (Exception e) {
                        logger.debug("[{}] Unexpected error with repository {}: {}",
                            threadName, repo.getId(), e.getMessage());
                    }
                }

                logger.debug("[{}] Not found in any POM repository, falling back to Maven Central: {}",
                    threadName, coordinate);
            }

            // ========== TIER-3: MAVEN CENTRAL (FALLBACK) ==========
            // Download from Maven Central as last resort
            logger.debug("[{}] Downloading from Maven Central: {}", threadName, coordinate);
            HttpArtifactDownloader.DownloadResult result = httpDownloader.download(
                artifact, targetPath, downloadConfig
            );

            if (result.isSuccess()) {
                logger.debug("[{}] Downloaded successfully: {}", threadName, coordinate);
                return DownloadOutcome.success(coordinate, result.getSource(), result.getPath());
            } else {
                if (result.getErrorMessage() != null && result.getErrorMessage().contains("404")) {
                    return DownloadOutcome.skippedNotFound(coordinate, result.getErrorMessage());
                }
                return DownloadOutcome.failed(coordinate, result.getErrorMessage(), null);
            }

        } catch (ArtifactDownloadException e) {
            logger.error("[{}] Download failed for {}: {}", threadName, coordinate, e.getSanitizedMessage());
            return DownloadOutcome.failed(coordinate, e.getSanitizedMessage(), e);
        } catch (Exception e) {
            logger.error("[{}] Unexpected error for {}: {}", threadName, coordinate, e.getMessage());
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