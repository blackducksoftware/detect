package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload.DownloadConfiguration;
import com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload.DownloadResult;
import com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload.HttpArtifactDownloader;
import com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload.LocalRepositoryChecker;
import com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload.MavenCentralDownloader;
import com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload.RemoteRepositoryDownloader;
import com.blackduck.integration.detectable.detectables.maven.resolver.exception.ArtifactDownloadException;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaRepository;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Artifact Downloader that orchestrates JAR downloads using a 3-tier resolution strategy.
 *
 * <p>Tier-1: Local repositories (custom .m2 location, home ~/.m2/repository, download cache)
 * <p>Tier-2: POM-declared remote repositories
 * <p>Tier-3: Maven Central as fallback
 *
 * <p>This class follows the Single Responsibility Principle: it only orchestrates resolution.
 * All actual work is delegated to specialized classes:
 * <ul>
 *   <li>{@link LocalRepositoryChecker} - checks local repositories for existing JARs</li>
 *   <li>{@link RemoteRepositoryDownloader} - downloads from POM-declared repositories</li>
 *   <li>{@link MavenCentralDownloader} - downloads from Maven Central</li>
 * </ul>
 */
public class ArtifactDownloader {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactDownloader.class);

    private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";
    private static final String MAVEN_CENTRAL_URL_ALT = "https://repo.maven.apache.org/maven2";

    // Delegates
    private final LocalRepositoryChecker localChecker;
    private final MavenCentralDownloader mavenCentralDownloader;
    private final Map<String, HttpArtifactDownloader> pomRepoDownloaders;

    // Configuration
    private final Path resolvedCustomRepositoryPath;
    private final Path defaultRepositoryPath;
    private final Path homeM2Repository;
    private final List<JavaRepository> pomRepositories;
    private final DownloadConfiguration downloadConfiguration;

    /**
     * Constructs an ArtifactDownloader with full configuration including POM repositories.
     *
     * @param customRepositoryPath Optional custom path to a local Maven repository (.m2 location).
     * @param defaultRepositoryPath Default download cache path (typically Detect output downloads dir)
     * @param pomRepositories List of repositories declared in POM files (Tier-2)
     * @param options User-facing configuration (download flag, custom repo path)
     */
    public ArtifactDownloader(Path customRepositoryPath, Path defaultRepositoryPath,
                              List<JavaRepository> pomRepositories, MavenResolverOptions options) {

        logger.info("Initializing ArtifactDownloader...");

        // Initialize delegates
        this.localChecker = new LocalRepositoryChecker();
        this.mavenCentralDownloader = new MavenCentralDownloader();
        this.downloadConfiguration = DownloadConfiguration.createDefault();

        // Resolve custom repository path
        if (customRepositoryPath != null) {
            logger.info("User-provided custom repository path: {}", customRepositoryPath);
            this.resolvedCustomRepositoryPath = M2RepositoryPathResolver.resolve(customRepositoryPath);
            if (this.resolvedCustomRepositoryPath != null) {
                logger.info("Resolved custom repository path to: {}", this.resolvedCustomRepositoryPath);
            } else {
                logger.warn("Could not resolve custom repository path. Custom repository will be skipped.");
            }
        } else {
            this.resolvedCustomRepositoryPath = null;
            logger.debug("No custom repository path configured");
        }

        if (defaultRepositoryPath == null) {
            throw new IllegalArgumentException("Default repository path must not be null");
        }
        this.defaultRepositoryPath = defaultRepositoryPath;

        // Resolve home .m2 repository once
        this.homeM2Repository = localChecker.resolveHomeM2Repository();

        // Deduplicate and filter POM repositories
        this.pomRepositories = deduplicatePomRepositories(pomRepositories);

        // Create downloaders for each POM repository
        this.pomRepoDownloaders = new HashMap<>();
        for (JavaRepository repo : this.pomRepositories) {
            pomRepoDownloaders.put(repo.getId(), new RemoteRepositoryDownloader(repo.getUrl()));
        }

        logInitializationSummary();
    }

    /**
     * Constructs an ArtifactDownloader without POM repositories (backward compatibility).
     */
    public ArtifactDownloader(Path customRepositoryPath, Path defaultRepositoryPath,
                              MavenResolverOptions options) {
        this(customRepositoryPath, defaultRepositoryPath, Collections.emptyList(), options);
    }

    /**
     * Downloads JAR files for all dependencies in the list.
     *
     * @param dependencies the list of dependencies to download
     * @return a map of successfully downloaded artifacts to their local JAR file paths
     */
    public Map<Artifact, Path> downloadArtifacts(List<Dependency> dependencies) {
        Map<Artifact, Path> artifactPathMap = new HashMap<>();

        if (dependencies == null || dependencies.isEmpty()) {
            logger.info("No dependencies to download. Skipping JAR download phase.");
            return artifactPathMap;
        }

        logger.info("========================================");
        logger.info("STARTING ARTIFACT JAR DOWNLOAD PHASE");
        logger.info("========================================");
        logger.info("Total dependencies to process: {}", dependencies.size());

        int successCount = 0;
        int failureCount = 0;
        int skippedCount = 0;
        List<String> failedArtifacts = new ArrayList<>();
        List<String> unavailableArtifacts = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < dependencies.size(); i++) {
            Dependency dependency = dependencies.get(i);
            Artifact artifact = dependency.getArtifact();
            String coords = formatCoords(artifact);

            logger.info("----------------------------------------");
            logger.info("[{}/{}] PROCESSING: {}", i + 1, dependencies.size(), coords);

            // Skip artifacts that have a classifier (e.g., sources, javadoc, test-jar)
            String classifier = artifact.getClassifier();
            if (classifier != null && !classifier.isEmpty()) {
                logger.info("SKIPPING artifact with classifier '{}': {}", classifier, coords);
                skippedCount++;
                continue;
            }

            try {
                DownloadResult result = downloadSingleArtifact(artifact);

                if (result.isSuccess()) {
                    logger.info("SUCCESS: JAR fetched from '{}' for {}", result.getSource(), coords);
                    if (result.getDownloadedPath() != null) {
                        logger.debug("  Cached at: {}", result.getDownloadedPath());
                        artifactPathMap.put(artifact, result.getDownloadedPath());
                    }
                    successCount++;
                } else if (result.isNotFound()) {
                    logger.warn("NOT FOUND: {} not available in any repository", coords);
                    unavailableArtifacts.add(coords);
                    skippedCount++;
                } else {
                    logger.error("FAILED to download {}: {}", coords, result.getErrorMessage());
                    failureCount++;
                    failedArtifacts.add(coords + " - " + result.getErrorMessage());
                }

            } catch (ArtifactDownloadException e) {
                logger.error("DOWNLOAD ERROR for {}: {}", coords, e.getSanitizedMessage());
                logger.debug("Full error details:", e);
                failureCount++;
                failedArtifacts.add(coords + " - " + e.getSanitizedMessage());
            } catch (Exception e) {
                logger.error("UNEXPECTED ERROR for {}: {}", coords, e.getMessage());
                logger.debug("Full error details:", e);
                failureCount++;
                failedArtifacts.add(coords + " - Unexpected: " + e.getMessage());
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        logDownloadSummary(dependencies.size(), successCount, failureCount, skippedCount,
            artifactPathMap.size(), elapsedTime, unavailableArtifacts, failedArtifacts);

        return artifactPathMap;
    }

    /**
     * Downloads a single artifact JAR using 3-tier resolution.
     * Delegates all work to specialized classes.
     */
    private DownloadResult downloadSingleArtifact(Artifact artifact) throws ArtifactDownloadException {
        String coords = formatCoords(artifact);
        logger.debug("Starting 3-Tier Resolution for: {}", coords);

        // ========== TIER-1: LOCAL REPOSITORIES ==========
        logger.info("--- TIER-1: LOCAL REPOSITORIES ---");
        DownloadResult localResult = checkLocalRepositories(artifact);
        if (localResult != null) {
            return localResult;
        }

        // ========== TIER-2: POM-DECLARED REPOSITORIES ==========
        if (!pomRepositories.isEmpty()) {
            logger.info("--- TIER-2: POM-DECLARED REPOSITORIES ({}) ---", pomRepositories.size());
            DownloadResult pomResult = downloadFromPomRepositories(artifact);
            if (pomResult != null && (pomResult.isSuccess() || !pomResult.isNotFound())) {
                return pomResult;
            }
            logger.debug("Not found in any POM repository");
        }

        // ========== TIER-3: MAVEN CENTRAL (FALLBACK) ==========
        logger.info("--- TIER-3: MAVEN CENTRAL (FALLBACK) ---");
        return downloadFromMavenCentral(artifact);
    }

    /**
     * Checks all local repositories for the artifact.
     * Returns DownloadResult if found, null if not found in any local repo.
     */
    private DownloadResult checkLocalRepositories(Artifact artifact) {
        // Step 1a: Check custom .m2 repository
        if (resolvedCustomRepositoryPath != null) {
            logger.debug("  Checking custom .m2 repository: {}", resolvedCustomRepositoryPath);
            Path jarPath = localChecker.checkCustomRepository(artifact, resolvedCustomRepositoryPath);
            if (jarPath != null) {
                logger.info("  FOUND in custom .m2 repository");
                return DownloadResult.success("custom-m2-repository", jarPath);
            }
        }

        // Step 1b: Check home ~/.m2/repository
        if (homeM2Repository != null) {
            logger.debug("  Checking home .m2 repository: {}", homeM2Repository);
            Path jarPath = localChecker.checkDefaultRepository(artifact, homeM2Repository);
            if (jarPath != null) {
                logger.info("  FOUND in home .m2 repository");
                return DownloadResult.success("home-m2-repository", jarPath);
            }
        }

        // Step 1c: Check download cache
        logger.debug("  Checking download cache: {}", defaultRepositoryPath);
        Path cachedJarPath = localChecker.checkRepository(artifact, defaultRepositoryPath, "download-cache");
        if (cachedJarPath != null) {
            logger.info("  FOUND in download cache");
            return DownloadResult.success("download-cache", cachedJarPath);
        }

        logger.debug("  Not found in any local repository");
        return null;
    }

    /**
     * Attempts to download from POM-declared repositories.
     * Returns first successful result, or null if all fail.
     */
    private DownloadResult downloadFromPomRepositories(Artifact artifact) throws ArtifactDownloadException {
        Path targetPath = localChecker.buildJarPath(artifact, defaultRepositoryPath);

        for (JavaRepository repo : pomRepositories) {
            logger.debug("  Trying POM repository: {} ({})", repo.getId(), repo.getUrl());

            HttpArtifactDownloader downloader = pomRepoDownloaders.get(repo.getId());
            if (downloader == null) {
                logger.debug("    No downloader available for {}", repo.getId());
                continue;
            }

            try {
                DownloadResult result = downloader.download(artifact, targetPath, downloadConfiguration);

                if (result.isSuccess()) {
                    logger.info("  FOUND in POM repository: {}", repo.getId());
                    return DownloadResult.success("pom-repository:" + repo.getId(), result.getDownloadedPath());
                } else if (result.isNotFound()) {
                    logger.debug("    Not available in {}: 404", repo.getId());
                } else {
                    logger.debug("    Failed to download from {}: {}", repo.getId(), result.getErrorMessage());
                }

            } catch (ArtifactDownloadException e) {
                // Log but continue to next repository
                if (e.getMessage() != null && e.getMessage().contains("404")) {
                    logger.debug("    Not available in {}: 404", repo.getId());
                } else {
                    logger.debug("    Error downloading from {}: {}", repo.getId(), e.getSanitizedMessage());
                }
            }
        }

        return null;
    }

    /**
     * Downloads from Maven Central.
     */
    private DownloadResult downloadFromMavenCentral(Artifact artifact) throws ArtifactDownloadException {
        Path targetPath = localChecker.buildJarPath(artifact, defaultRepositoryPath);
        logger.debug("  Downloading from Maven Central...");

        DownloadResult result = mavenCentralDownloader.download(artifact, targetPath, downloadConfiguration);

        if (result.isSuccess()) {
            logger.info("  FOUND in Maven Central");
            return DownloadResult.success("maven-central", result.getDownloadedPath());
        } else if (result.isNotFound()) {
            logger.info("  Not available in Maven Central (404)");
            return DownloadResult.notFound("Artifact not found in Maven Central");
        } else {
            logger.warn("  Failed to download from Maven Central: {}", result.getErrorMessage());
            return DownloadResult.failure("Maven Central download failed: " + result.getErrorMessage());
        }
    }

    /**
     * Deduplicates POM repositories, removing Maven Central references (handled as Tier-3).
     */
    private List<JavaRepository> deduplicatePomRepositories(List<JavaRepository> repositories) {
        if (repositories == null || repositories.isEmpty()) {
            return Collections.emptyList();
        }

        List<JavaRepository> filtered = new ArrayList<>();
        int mavenCentralSkipped = 0;

        for (JavaRepository repo : repositories) {
            String url = repo.getUrl();
            if (url == null || url.isEmpty()) {
                continue;
            }

            String normalizedUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

            if (normalizedUrl.equals(MAVEN_CENTRAL_URL) || normalizedUrl.equals(MAVEN_CENTRAL_URL_ALT)) {
                mavenCentralSkipped++;
                continue;
            }

            filtered.add(repo);
        }

        if (mavenCentralSkipped > 0) {
            logger.debug("Deduplicated {} Maven Central reference(s) from POM repositories", mavenCentralSkipped);
        }

        return filtered;
    }

    private void logInitializationSummary() {
        logger.info("========== TIER-1: LOCAL REPOSITORIES ==========");
        logger.info("  Custom .m2 repository: {}",
            resolvedCustomRepositoryPath != null ? resolvedCustomRepositoryPath : "not configured");
        logger.info("  Home .m2 repository: {}",
            homeM2Repository != null ? homeM2Repository : "not found");
        logger.info("  Download cache: {}", defaultRepositoryPath);

        logger.info("========== TIER-2: POM REPOSITORIES ==========");
        if (pomRepositories.isEmpty()) {
            logger.info("  No POM-declared repositories configured");
        } else {
            logger.info("  {} POM repositories configured:", pomRepositories.size());
            for (int i = 0; i < pomRepositories.size(); i++) {
                JavaRepository repo = pomRepositories.get(i);
                logger.info("    {}. {} ({})", i + 1, repo.getId(), repo.getUrl());
            }
        }

        logger.info("========== TIER-3: MAVEN CENTRAL ==========");
        logger.info("  Maven Central: Always available as fallback");

        logger.info("========== DOWNLOAD CONFIGURATION ==========");
        logger.info("  {}", downloadConfiguration);
    }

    private void logDownloadSummary(int total, int success, int failures, int skipped,
                                    int mapSize, long elapsedMs,
                                    List<String> unavailable, List<String> failed) {
        logger.info("========================================");
        logger.info("JAR DOWNLOAD PHASE SUMMARY");
        logger.info("========================================");
        logger.info("Total processed: {}", total);
        logger.info("  Successfully downloaded: {}", success);
        logger.info("  Not available (skipped): {}", skipped);
        logger.info("  Failed with errors: {}", failures);
        logger.info("  Artifact-to-path map entries: {}", mapSize);
        logger.info("Time elapsed: {} ms", elapsedMs);

        if (!unavailable.isEmpty()) {
            logger.info("Artifacts not available in repositories ({}):", unavailable.size());
            unavailable.forEach(a -> logger.info("  - {}", a));
        }

        if (!failed.isEmpty()) {
            logger.error("Failed artifacts requiring attention ({}):", failed.size());
            failed.forEach(a -> logger.error("  - {}", a));
        }

        if (failures > 0) {
            logger.error("JAR DOWNLOAD PHASE COMPLETED WITH ERRORS: {} artifacts failed", failures);
        } else if (skipped > 0) {
            logger.info("JAR DOWNLOAD PHASE COMPLETED: {} artifacts unavailable but no errors", skipped);
        } else {
            logger.info("JAR DOWNLOAD PHASE COMPLETED SUCCESSFULLY: All {} artifacts downloaded", success);
        }
    }

    private String formatCoords(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }
}

