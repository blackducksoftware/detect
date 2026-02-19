package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.detectable.detectables.maven.resolver.download.*;
import com.blackduck.integration.detectable.detectables.maven.resolver.exception.ArtifactDownloadException;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaRepository;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Refactored Artifact Downloader following SOLID principles.
 * Orchestrates artifact download process using specialized components.
 *
 * Key improvements:
 * - Strict configuration validation at startup
 * - Explicit warnings for missing artifacts
 * - Configurable HTTP timeouts
 * - Structured error categorization
 * - Atomic file writes
 * - Proper resource management
 * - Parallel downloads with controlled concurrency
 * - Configurable retry logic with exponential backoff
 * - Classifier-aware artifact resolution
 * - POM-declared repository support (Tier-2)
 */
public class ArtifactDownloaderV2 {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactDownloaderV2.class);

    // Maven Central URLs to deduplicate
    private static final String MAVEN_CENTRAL_URL_1 = "https://repo.maven.apache.org/maven2";
    private static final String MAVEN_CENTRAL_URL_2 = "https://repo1.maven.org/maven2";

    private final Path customRepositoryPath;
    private final Path defaultRepositoryPath;
    private final List<JavaRepository> pomRepositories; // POM-declared repositories (Tier-2)
    private final Map<String, HttpArtifactDownloader> remoteDownloaders; // Cache for POM repository downloaders
    private final RepositoryPathValidator pathValidator;
    private final LocalRepositoryChecker localChecker;
    private final HttpArtifactDownloader httpDownloader;
    private final HttpArtifactDownloader.DownloadConfiguration downloadConfig;
    private final ParallelDownloadManager parallelDownloadManager;
    private final RetryPolicy retryPolicy;
    private final boolean useParallelDownloads;

    /**
     * Constructs an ArtifactDownloader with full configuration including POM repositories.
     * This is the primary constructor that supports 3-tier resolution:
     * Tier-1: Local repositories (custom and .m2)
     * Tier-2: POM-declared repositories
     * Tier-3: Maven Central
     *
     * @param customRepositoryPath Optional custom path to check for existing JARs
     * @param defaultRepositoryPath Default Maven repository path (~/.m2/repository)
     * @param pomRepositories List of repositories declared in POM files (Tier-2)
     * @param options Configuration options including timeouts, threads, and retry settings
     * @throws IllegalStateException if paths are invalid or inaccessible
     */
    public ArtifactDownloaderV2(Path customRepositoryPath, Path defaultRepositoryPath,
                               List<JavaRepository> pomRepositories, MavenResolverOptions options) {

        // Initialize core components (Dependency Injection)
        this.pathValidator = new RepositoryPathValidator();
        this.localChecker = new LocalRepositoryChecker();
        this.httpDownloader = new MavenCentralDownloader();  // Tier-3 downloader
        this.remoteDownloaders = new HashMap<>();  // Cache for Tier-2 downloaders

        // Validate paths at startup (Fix #1)
        logger.info("Initializing ArtifactDownloader with enhanced features...");

        // Validate custom repository path if provided
        this.customRepositoryPath = pathValidator.validateCustomRepositoryPath(customRepositoryPath);

        // Validate default repository path (required)
        pathValidator.validateDefaultRepositoryPath(defaultRepositoryPath);
        this.defaultRepositoryPath = defaultRepositoryPath;

        // Configure HTTP timeouts
        // Uses provided values or defaults (30s connect, 30s read) if not specified
        this.downloadConfig = HttpArtifactDownloader.DownloadConfiguration.fromProperties(
            options.getConnectTimeoutMs(), options.getReadTimeoutMs()
        );

        // Configure retry policy with intelligent defaults
        // Default: 3 retries, 1 second initial backoff, 30 second max backoff
        int retryCount = options.getRetryCount() != null ? options.getRetryCount() : 3;
        long retryInitialMs = options.getRetryBackoffInitialMs() != null ? options.getRetryBackoffInitialMs() : 1000L;
        long retryMaxMs = options.getRetryBackoffMaxMs() != null ? options.getRetryBackoffMaxMs() : 30000L;

        this.retryPolicy = new RetryPolicy(retryCount, retryInitialMs, retryMaxMs);

        // Configure parallel downloads
        // Only enabled if thread count > 1 is explicitly specified
        this.useParallelDownloads = options.getDownloadThreads() != null && options.getDownloadThreads() > 1;
        if (this.useParallelDownloads) {
            // Create parallel download manager with specified thread count
            this.parallelDownloadManager = new ParallelDownloadManager(
                options.getDownloadThreads(),
                localChecker,
                httpDownloader
            );
            logger.debug("Parallel download manager created with {} threads", options.getDownloadThreads());
        } else {
            // Sequential downloads will be used
            this.parallelDownloadManager = null;
            logger.debug("Parallel downloads disabled - will use sequential downloads");
        }

        // Process and deduplicate POM repositories (Tier-2)
        this.pomRepositories = deduplicatePomRepositories(pomRepositories);

        // Initialize Tier-2 downloaders for each unique POM repository
        for (JavaRepository repo : this.pomRepositories) {
            String url = repo.getUrl();
            if (!remoteDownloaders.containsKey(url)) {
                remoteDownloaders.put(url, new RemoteRepositoryDownloader(url));
                logger.debug("  Initialized Tier-2 downloader for: {}", url);
            }
        }

        // Log initialization summary with enhanced 3-tier information
        logger.info("ArtifactDownloader initialized successfully with 3-tier resolution");
        logger.info("========== TIER-1: LOCAL REPOSITORIES ==========");
        logger.info("  Custom repository: {}",
            customRepositoryPath != null ? sanitizePath(customRepositoryPath) : "not configured");
        logger.info("  Default repository (.m2): {}", sanitizePath(defaultRepositoryPath));

        logger.info("========== TIER-2: POM REPOSITORIES ==========");
        if (this.pomRepositories.isEmpty()) {
            logger.info("  No POM-declared repositories configured");
        } else {
            logger.info("  {} POM repositories configured:", this.pomRepositories.size());
            for (int i = 0; i < this.pomRepositories.size(); i++) {
                JavaRepository repo = this.pomRepositories.get(i);
                logger.info("    {}. {} ({})", i + 1, repo.getId(), repo.getUrl());
            }
        }

        logger.info("========== TIER-3: MAVEN CENTRAL ==========");
        logger.info("  Maven Central: Always available as fallback");

        logger.info("========== DOWNLOAD CONFIGURATION ==========");
        logger.info("  Connect timeout: {}ms", downloadConfig.getConnectTimeoutMs());
        logger.info("  Read timeout: {}ms", downloadConfig.getReadTimeoutMs());
        logger.info("  Parallel downloads: {} (threads: {})",
            useParallelDownloads ? "enabled" : "disabled",
            useParallelDownloads && parallelDownloadManager != null ? options.getDownloadThreads() : 1);
        logger.info("  Retry policy: {} retries, {}ms initial backoff",
            retryPolicy.getMaxRetries(), retryPolicy.getInitialBackoffMs());
    }

    /**
     * Constructs an ArtifactDownloader without POM repositories (backward compatibility).
     * Uses only Tier-1 (local) and Tier-3 (Maven Central) resolution.
     *
     * @param customRepositoryPath Optional custom path to check for existing JARs
     * @param defaultRepositoryPath Default Maven repository path (~/.m2/repository)
     * @param options Configuration options including timeouts, threads, and retry settings
     */
    public ArtifactDownloaderV2(Path customRepositoryPath, Path defaultRepositoryPath,
                               MavenResolverOptions options) {
        // Delegate to primary constructor with empty POM repositories list
        this(customRepositoryPath, defaultRepositoryPath, Collections.emptyList(), options);
    }

    /**
     * Legacy constructor for backward compatibility.
     * Uses only Tier-1 and Tier-3 resolution (no POM repositories).
     *
     * @deprecated Use {@link #ArtifactDownloaderV2(Path, Path, MavenResolverOptions)} instead.
     * This constructor will be removed in a future version to avoid ambiguity.
     */
    @Deprecated
    public ArtifactDownloaderV2(Path customRepositoryPath, Path defaultRepositoryPath,
                               Integer connectTimeoutMs, Integer readTimeoutMs) {
        this(customRepositoryPath, defaultRepositoryPath, Collections.emptyList(),
             new MavenResolverOptions(true, customRepositoryPath, connectTimeoutMs, readTimeoutMs));
    }

    /**
     * Deduplicates POM repositories by removing Maven Central URLs.
     * Maven Central is always available as Tier-3, so we exclude it from Tier-2.
     * This prevents redundant checks and maintains clear tier separation.
     *
     * @param repositories Original list of POM-declared repositories
     * @return Filtered list excluding Maven Central URLs
     */
    private List<JavaRepository> deduplicatePomRepositories(List<JavaRepository> repositories) {
        if (repositories == null || repositories.isEmpty()) {
            logger.debug("No POM repositories to deduplicate");
            return Collections.emptyList();
        }

        List<JavaRepository> filtered = new ArrayList<>();
        int mavenCentralSkipped = 0;

        for (JavaRepository repo : repositories) {
            String url = repo.getUrl();
            if (url == null || url.isEmpty()) {
                logger.debug("Skipping repository with null/empty URL: {}", repo.getId());
                continue;
            }

            // Normalize URL for comparison (remove trailing slash)
            String normalizedUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

            // Check if this is Maven Central (skip it since it's Tier-3)
            if (normalizedUrl.equals(MAVEN_CENTRAL_URL_1) || normalizedUrl.equals(MAVEN_CENTRAL_URL_2)) {
                logger.debug("Skipping Maven Central repository from Tier-2 (will use as Tier-3): {}", repo.getId());
                mavenCentralSkipped++;
                continue;
            }

            // Add non-Maven Central repository to Tier-2
            filtered.add(repo);
            logger.debug("Added POM repository to Tier-2: {} ({})", repo.getId(), url);
        }

        if (mavenCentralSkipped > 0) {
            logger.info("Deduplicated {} Maven Central reference(s) from POM repositories", mavenCentralSkipped);
        }

        logger.info("POM repository deduplication complete: {} repositories retained for Tier-2",
            filtered.size());

        return filtered;
    }

    /**
     * Downloads JAR files for all dependencies in the list.
     * Uses parallel downloads if enabled, otherwise sequential.
     *
     * @param dependencies List of Aether dependencies to download
     */
    public void downloadArtifacts(List<Dependency> dependencies) {
        if (dependencies.isEmpty()) {
            logger.info("No dependencies to download. Skipping JAR download phase.");
            return;
        }

        // Use parallel downloads if enabled and worthwhile
        if (useParallelDownloads && parallelDownloadManager != null && dependencies.size() > 2) {
            downloadArtifactsParallel(dependencies);
        } else {
            downloadArtifactsSequential(dependencies);
        }
    }

    /**
     * Downloads artifacts using parallel download manager with 3-tier resolution.
     * Supports both legacy (2-tier) and enhanced (3-tier) resolution based on
     * whether POM repositories are configured.
     */
    private void downloadArtifactsParallel(List<Dependency> dependencies) {
        try {
            // Configure download with retry policy
            HttpArtifactDownloader.DownloadConfiguration configWithRetry =
                new HttpArtifactDownloader.DownloadConfiguration(
                    downloadConfig.getConnectTimeoutMs(),
                    downloadConfig.getReadTimeoutMs(),
                    retryPolicy.getMaxRetries(),
                    (int) retryPolicy.getInitialBackoffMs()
                );

            // Log 3-tier configuration for parallel downloads
            logger.info("Parallel download configuration:");
            logger.info("  Tier-1: Local repositories (custom + .m2)");
            if (!pomRepositories.isEmpty()) {
                logger.info("  Tier-2: {} POM repositories", pomRepositories.size());
            }
            logger.info("  Tier-3: Maven Central (fallback)");

            // Use the new overload that supports POM repositories
            ParallelDownloadManager.ParallelDownloadResult result =
                parallelDownloadManager.downloadArtifacts(
                    dependencies,
                    customRepositoryPath,
                    defaultRepositoryPath,
                    pomRepositories,  // Pass POM repositories for Tier-2 resolution
                    configWithRetry
                );

            if (!result.isCompleteSuccess()) {
                logger.error("Some artifacts failed to download: {} failures out of {}",
                    result.getFailureCount(), result.getTotalArtifacts());
            } else {
                logger.info("All artifacts downloaded successfully in parallel");
            }

        } finally {
            // Ensure thread pool is shut down
            if (parallelDownloadManager != null) {
                parallelDownloadManager.shutdown();
            }
        }
    }

    /**
     * Downloads artifacts sequentially (original implementation).
     */
    private void downloadArtifactsSequential(List<Dependency> dependencies) {
        logger.info("========================================");
        logger.info("STARTING ARTIFACT JAR DOWNLOAD PHASE");
        logger.info("========================================");
        logger.info("Total dependencies to process: {}", dependencies.size());

        // Statistics tracking
        int successCount = 0;
        int failureCount = 0;
        int skippedCount = 0;
        List<String> failedArtifacts = new ArrayList<>();
        List<String> unavailableArtifacts = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < dependencies.size(); i++) {
            Dependency dependency = dependencies.get(i);
            Artifact artifact = dependency.getArtifact();
            String artifactId = formatArtifactId(artifact);

            logger.info("----------------------------------------");
            logger.info("[{}/{}] PROCESSING: {}", i + 1, dependencies.size(), artifactId);

            try {
                DownloadResult result = downloadArtifact(artifact);

                if (result.isSuccess()) {
                    logger.info("✓ SUCCESS: JAR fetched from {}", result.getSource());
                    if (result.getPath() != null) {
                        logger.debug("  Cached at: {}", result.getPath());
                    }
                    successCount++;
                } else if (result.isNotFound()) {
                    logger.warn("⚠ NOT AVAILABLE: JAR not found in any repository for {}", artifactId);
                    unavailableArtifacts.add(artifactId);
                    skippedCount++;
                } else {
                    logger.error("✗ FAILED: {}", result.getErrorMessage());
                    failureCount++;
                    failedArtifacts.add(artifactId + " - " + result.getErrorMessage());
                }

            } catch (ArtifactDownloadException e) {
                // Structured error reporting (Fix #5)
                logger.error("✗ {} downloading {}: {}",
                    e.getCategory().name(), artifactId, e.getActionableMessage());
                logger.debug("Full error details:", e);
                failureCount++;
                failedArtifacts.add(artifactId + " - " + e.getCategory() + ": " + e.getActionableMessage());

            } catch (Exception e) {
                logger.error("✗ UNEXPECTED ERROR downloading {}: {}", artifactId, e.getMessage());
                logger.debug("Full error details:", e);
                failureCount++;
                failedArtifacts.add(artifactId + " - Unexpected: " + e.getMessage());
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        // Final summary
        logger.info("========================================");
        logger.info("JAR DOWNLOAD PHASE SUMMARY");
        logger.info("========================================");
        logger.info("Total processed: {}", dependencies.size());
        logger.info("  ✓ Successfully downloaded: {}", successCount);
        logger.info("  ⚠ Not available (skipped): {}", skippedCount);
        logger.info("  ✗ Failed with errors: {}", failureCount);
        logger.info("Time elapsed: {} ms", elapsedTime);

        if (!unavailableArtifacts.isEmpty()) {
            logger.info("Artifacts not available in repositories ({}):", unavailableArtifacts.size());
            unavailableArtifacts.forEach(a -> logger.info("  - {}", a));
        }

        if (!failedArtifacts.isEmpty()) {
            logger.error("Failed artifacts requiring attention ({}):", failedArtifacts.size());
            failedArtifacts.forEach(a -> logger.error("  - {}", a));
        }

        if (failureCount > 0) {
            logger.error("JAR DOWNLOAD PHASE COMPLETED WITH ERRORS: {} artifacts failed", failureCount);
        } else if (skippedCount > 0) {
            logger.info("JAR DOWNLOAD PHASE COMPLETED: {} artifacts unavailable but no errors", skippedCount);
        } else {
            logger.info("JAR DOWNLOAD PHASE COMPLETED SUCCESSFULLY: All {} artifacts downloaded", successCount);
        }
        logger.info("========================================");
    }

    /**
     * Downloads a single artifact using the enhanced 3-tier resolution process.
     * Resolution order:
     *   Tier-1: Local repositories (custom, then .m2)
     *   Tier-2: POM-declared remote repositories
     *   Tier-3: Maven Central as fallback
     *
     * @param artifact The artifact to download
     * @return Result of the download operation
     * @throws ArtifactDownloadException if all tiers fail
     */
    private DownloadResult downloadArtifact(Artifact artifact) throws ArtifactDownloadException {
        String artifactId = formatArtifactId(artifact);

        logger.info("╔════════════════════════════════════════════════════════╗");
        logger.info("║  Starting 3-Tier Resolution for: {}", artifactId);
        logger.info("╚════════════════════════════════════════════════════════╝");

        // Calculate total steps based on configuration
        int currentStep = 0;
        int totalSteps = 2; // At minimum: .m2 + Maven Central
        if (customRepositoryPath != null) totalSteps++;
        totalSteps += pomRepositories.size(); // Add POM repositories

        // ========== TIER-1: LOCAL REPOSITORIES ==========
        logger.info("┌─── TIER-1: LOCAL REPOSITORIES ───┐");

        // Step 1a: Check custom repository (if configured)
        if (customRepositoryPath != null) {
            currentStep++;
            logger.info("  [Step {}/{}] Checking custom repository...", currentStep, totalSteps);
            Path customJarPath = localChecker.checkCustomRepository(artifact, customRepositoryPath);
            if (customJarPath != null) {
                logger.info("  ✓ FOUND in custom repository: {}", sanitizePath(customJarPath));
                return DownloadResult.success("custom-repository", customJarPath);
            }
            logger.info("  ✗ Not found in custom repository");
        }

        // Step 1b: Check default .m2/repository
        currentStep++;
        logger.info("  [Step {}/{}] Checking .m2/repository...", currentStep, totalSteps);
        Path defaultJarPath = localChecker.checkDefaultRepository(artifact, defaultRepositoryPath);
        if (defaultJarPath != null) {
            logger.info("  ✓ FOUND in .m2/repository: {}", sanitizePath(defaultJarPath));
            return DownloadResult.success("m2-repository", defaultJarPath);
        }
        logger.info("  ✗ Not found in .m2/repository");

        // ========== TIER-2: POM-DECLARED REPOSITORIES ==========
        if (!pomRepositories.isEmpty()) {
            logger.info("┌─── TIER-2: POM-DECLARED REPOSITORIES ───┐");
            logger.info("  Checking {} POM repositories...", pomRepositories.size());

            for (JavaRepository repo : pomRepositories) {
                currentStep++;
                logger.info("  [Step {}/{}] Checking repository: {} ({})",
                    currentStep, totalSteps, repo.getId(), repo.getUrl());

                try {
                    // Get or create downloader for this repository
                    HttpArtifactDownloader remoteDownloader = remoteDownloaders.get(repo.getUrl());
                    if (remoteDownloader == null) {
                        // Should not happen, but handle gracefully
                        logger.debug("    Creating on-demand downloader for: {}", repo.getUrl());
                        remoteDownloader = new RemoteRepositoryDownloader(repo.getUrl());
                        remoteDownloaders.put(repo.getUrl(), remoteDownloader);
                    }

                    // Try to download from this POM repository
                    Path targetPath = buildRepositoryJarPath(artifact, defaultRepositoryPath);
                    HttpArtifactDownloader.DownloadResult httpResult = remoteDownloader.download(
                        artifact, targetPath, downloadConfig
                    );

                    if (httpResult.isSuccess()) {
                        logger.info("  ✓ FOUND in POM repository: {} ({})", repo.getId(), repo.getUrl());
                        return DownloadResult.success("pom-repository:" + repo.getId(), httpResult.getPath());
                    } else if (httpResult.getErrorMessage() != null && httpResult.getErrorMessage().contains("404")) {
                        logger.debug("    Not available in {}: 404 Not Found", repo.getId());
                    } else {
                        logger.debug("    Failed to download from {}: {}", repo.getId(), httpResult.getErrorMessage());
                    }

                } catch (ArtifactDownloadException e) {
                    // Log but continue to next repository
                    if (e.getMessage() != null && e.getMessage().contains("404")) {
                        logger.debug("    Not available in {}: 404 Not Found", repo.getId());
                    } else {
                        logger.warn("    Error downloading from {}: {}", repo.getId(), e.getSanitizedMessage());
                    }
                } catch (Exception e) {
                    // Unexpected error, log and continue
                    logger.warn("    Unexpected error with repository {}: {}", repo.getId(), e.getMessage());
                }
            }

            logger.info("  ✗ Not found in any POM repository");
        }

        // ========== TIER-3: MAVEN CENTRAL (FALLBACK) ==========
        currentStep++;
        logger.info("┌─── TIER-3: MAVEN CENTRAL (FALLBACK) ───┐");
        logger.info("  [Step {}/{}] Downloading from Maven Central...", currentStep, totalSteps);

        Path targetPath = buildRepositoryJarPath(artifact, defaultRepositoryPath);

        try {
            HttpArtifactDownloader.DownloadResult httpResult = httpDownloader.download(
                artifact, targetPath, downloadConfig
            );

            if (httpResult.isSuccess()) {
                logger.info("  ✓ FOUND in Maven Central");
                return DownloadResult.success(httpResult.getSource(), httpResult.getPath());
            } else {
                logger.info("  ✗ Not available in Maven Central: {}", httpResult.getErrorMessage());
                return DownloadResult.failure(httpResult.getErrorMessage(),
                    httpResult.getErrorMessage().contains("404"));
            }

        } catch (ArtifactDownloadException e) {
            logger.error("  ✗ Failed to download from Maven Central: {}", e.getSanitizedMessage());
            throw e;
        }
    }

    private Path buildRepositoryJarPath(Artifact artifact, Path repositoryPath) {
        String groupPath = artifact.getGroupId().replace('.', '/');
        return repositoryPath
            .resolve(groupPath)
            .resolve(artifact.getArtifactId())
            .resolve(artifact.getVersion())
            .resolve(artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar");
    }

    private String formatArtifactId(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    private String sanitizePath(Path path) {
        if (path == null) return "<null>";
        String pathStr = path.toString();
        int lastSep = pathStr.lastIndexOf('/');
        if (lastSep > 0) {
            return ".../" + pathStr.substring(lastSep + 1);
        }
        return pathStr;
    }

    /**
     * Internal result class for download operations.
     */
    private static class DownloadResult {
        private final boolean success;
        private final String source;
        private final Path path;
        private final String errorMessage;
        private final boolean notFound;

        private DownloadResult(boolean success, String source, Path path, String errorMessage, boolean notFound) {
            this.success = success;
            this.source = source;
            this.path = path;
            this.errorMessage = errorMessage;
            this.notFound = notFound;
        }

        static DownloadResult success(String source, Path path) {
            return new DownloadResult(true, source, path, null, false);
        }

        static DownloadResult failure(String errorMessage, boolean notFound) {
            return new DownloadResult(false, null, null, errorMessage, notFound);
        }

        boolean isSuccess() { return success; }
        String getSource() { return source; }
        Path getPath() { return path; }
        String getErrorMessage() { return errorMessage; }
        boolean isNotFound() { return notFound; }
    }
}