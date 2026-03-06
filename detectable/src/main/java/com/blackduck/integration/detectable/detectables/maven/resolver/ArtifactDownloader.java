package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaRepository;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
 * <p>Internal tuning (timeouts, retries) uses {@link MavenDownloadConstants}.
 * Only user-facing options come from {@link MavenResolverOptions}.
 */
public class ArtifactDownloader {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactDownloader.class);

    private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";
    private static final String MAVEN_CENTRAL_URL_ALT = "https://repo.maven.apache.org/maven2";

    private final Path resolvedCustomRepositoryPath;
    private final Path defaultRepositoryPath;
    private final List<JavaRepository> pomRepositories;

    /**
     * Constructs an ArtifactDownloader with full configuration including POM repositories.
     *
     * @param customRepositoryPath Optional custom path to a local Maven repository (.m2 location).
     *                             Resolved automatically via {@link M2RepositoryPathResolver}.
     * @param defaultRepositoryPath Default download cache path (typically Detect output downloads dir)
     * @param pomRepositories List of repositories declared in POM files (Tier-2)
     * @param options User-facing configuration (download flag, custom repo path)
     */
    public ArtifactDownloader(Path customRepositoryPath, Path defaultRepositoryPath,
                              List<JavaRepository> pomRepositories, MavenResolverOptions options) {

        logger.info("Initializing ArtifactDownloader...");

        // Resolve custom repository path using M2RepositoryPathResolver
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

        this.pomRepositories = deduplicatePomRepositories(pomRepositories);

        // Log initialization summary
        logger.info("========== TIER-1: LOCAL REPOSITORIES ==========");
        logger.info("  Custom .m2 repository: {}",
            resolvedCustomRepositoryPath != null ? resolvedCustomRepositoryPath : "not configured");
        logger.info("  Default download cache: {}", defaultRepositoryPath);
        Path homeM2 = getHomeM2Repository();
        logger.info("  Home .m2 repository: {}", homeM2 != null ? homeM2 : "not found");

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

        logger.info("========== DOWNLOAD CONFIGURATION (internal defaults) ==========");
        logger.info("  Connect timeout: {}ms", MavenDownloadConstants.CONNECT_TIMEOUT_MS);
        logger.info("  Read timeout: {}ms", MavenDownloadConstants.READ_TIMEOUT_MS);
        logger.info("  Retry policy: {} retries, {}ms initial backoff, {}ms max backoff",
            MavenDownloadConstants.RETRY_COUNT,
            MavenDownloadConstants.RETRY_BACKOFF_INITIAL_MS,
            MavenDownloadConstants.RETRY_BACKOFF_MAX_MS);
    }

    /**
     * Constructs an ArtifactDownloader without POM repositories (backward compatibility).
     */
    public ArtifactDownloader(Path customRepositoryPath, Path defaultRepositoryPath,
                              MavenResolverOptions options) {
        this(customRepositoryPath, defaultRepositoryPath, Collections.emptyList(), options);
    }

    private Path getHomeM2Repository() {
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            Path homeM2 = Paths.get(userHome, ".m2", "repository");
            if (homeM2.toFile().isDirectory()) {
                return homeM2;
            }
        }
        return null;
    }

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

            String normalizedUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

            if (normalizedUrl.equals(MAVEN_CENTRAL_URL) || normalizedUrl.equals(MAVEN_CENTRAL_URL_ALT)) {
                logger.debug("Skipping Maven Central repository from Tier-2 (will use as Tier-3): {}", repo.getId());
                mavenCentralSkipped++;
                continue;
            }

            filtered.add(repo);
            logger.debug("Added POM repository to Tier-2: {} ({})", repo.getId(), url);
        }

        if (mavenCentralSkipped > 0) {
            logger.info("Deduplicated {} Maven Central reference(s) from POM repositories", mavenCentralSkipped);
        }

        logger.info("POM repository deduplication complete: {} repositories retained for Tier-2", filtered.size());
        return filtered;
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

        logger.info("STARTING ARTIFACT JAR DOWNLOAD PHASE");
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

                if (result.success) {
                    logger.info("SUCCESS: JAR fetched from '{}' for {}", result.source, coords);
                    if (result.path != null) {
                        logger.debug("  Cached at: {}", result.path);
                        artifactPathMap.put(artifact, result.path);
                    }
                    successCount++;
                } else if (result.notFound) {
                    logger.warn("Artifact JAR for {} not found in any configured repository", coords);
                    unavailableArtifacts.add(coords);
                    skippedCount++;
                } else {
                    logger.error("FAILED to download {}: {}", coords, result.errorMessage);
                    failureCount++;
                    failedArtifacts.add(coords + " - " + result.errorMessage);
                }

            } catch (Exception e) {
                logger.error("UNEXPECTED ERROR downloading {}: {}", coords, e.getMessage());
                logger.debug("Full error details:", e);
                failureCount++;
                failedArtifacts.add(coords + " - Unexpected: " + e.getMessage());
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        // ===== SUMMARY LOG 1: Success/failure with source =====
        logger.info("JAR DOWNLOAD PHASE SUMMARY");
        logger.info("Total processed: {}", dependencies.size());
        logger.info("  Successfully downloaded: {}", successCount);
        logger.info("  Not available (skipped): {}", skippedCount);
        logger.info("  Failed with errors: {}", failureCount);
        logger.info("  Artifact-to-path map entries: {}", artifactPathMap.size());
        logger.info("Time elapsed: {} ms", elapsedTime);

        if (!unavailableArtifacts.isEmpty()) {
            logger.info("Artifacts not available in repositories ({}):", unavailableArtifacts.size());
            unavailableArtifacts.forEach(a -> logger.info("  - {}", a));
        }

        if (!failedArtifacts.isEmpty()) {
            logger.error("Failed artifacts requiring attention ({}):", failedArtifacts.size());
            failedArtifacts.forEach(a -> logger.error("  - {}", a));
        }

        // ===== SUMMARY LOG 2: Phase completion =====
        if (failureCount > 0) {
            logger.error("JAR DOWNLOAD PHASE COMPLETED WITH ERRORS: {} artifacts failed", failureCount);
        } else if (skippedCount > 0) {
            logger.info("JAR DOWNLOAD PHASE COMPLETED: {} artifacts unavailable but no errors", skippedCount);
        } else {
            logger.info("JAR DOWNLOAD PHASE COMPLETED SUCCESSFULLY: All {} artifacts downloaded", successCount);
        }
        logger.info("JAR download phase complete. Moving to the next part of the code.");

        return artifactPathMap;
    }

    /**
     * Downloads a single artifact JAR using 3-tier resolution.
     */
    private DownloadResult downloadSingleArtifact(Artifact artifact) {
        String coords = formatCoords(artifact);
        Path homeM2 = getHomeM2Repository();

        logger.info("Starting 3-Tier Resolution for: {}", coords);

        // Calculate total steps for progress logging
        int currentStep = 0;
        int totalSteps = 1; // Maven Central always last
        if (resolvedCustomRepositoryPath != null) totalSteps++;
        if (homeM2 != null) totalSteps++;
        totalSteps++; // download cache
        totalSteps += pomRepositories.size();

        // ========== TIER-1: LOCAL REPOSITORIES ==========
        logger.info("--- TIER-1: LOCAL REPOSITORIES ---");

        // Step 1a: Check custom .m2 repository
        if (resolvedCustomRepositoryPath != null) {
            currentStep++;
            logger.info("  [Step {}/{}] Checking custom .m2 repository: {}", currentStep, totalSteps, resolvedCustomRepositoryPath);
            Path jarPath = findJarInRepository(artifact, resolvedCustomRepositoryPath);
            if (jarPath != null) {
                logger.info("  FOUND in custom .m2 repository: {}", jarPath);
                return DownloadResult.success("custom-m2-repository", jarPath);
            }
            logger.info("  Not found in custom .m2 repository");
        }

        // Step 1b: Check home ~/.m2/repository
        if (homeM2 != null) {
            currentStep++;
            logger.info("  [Step {}/{}] Checking home .m2 repository: {}", currentStep, totalSteps, homeM2);
            Path jarPath = findJarInRepository(artifact, homeM2);
            if (jarPath != null) {
                logger.info("  FOUND in home .m2 repository: {}", jarPath);
                return DownloadResult.success("home-m2-repository", jarPath);
            }
            logger.info("  Not found in home .m2 repository");
        }

        // Step 1c: Check download cache
        currentStep++;
        logger.info("  [Step {}/{}] Checking download cache: {}", currentStep, totalSteps, defaultRepositoryPath);
        Path cachedJarPath = findJarInRepository(artifact, defaultRepositoryPath);
        if (cachedJarPath != null) {
            logger.info("  FOUND in download cache: {}", cachedJarPath);
            return DownloadResult.success("download-cache", cachedJarPath);
        }
        logger.info("  Not found in download cache");

        // ========== TIER-2: POM-DECLARED REPOSITORIES ==========
        if (!pomRepositories.isEmpty()) {
            logger.info("--- TIER-2: POM-DECLARED REPOSITORIES ---");
            logger.info("  Checking {} POM repositories...", pomRepositories.size());

            for (JavaRepository repo : pomRepositories) {
                currentStep++;
                logger.info("  [Step {}/{}] Trying repository: {} ({})",
                    currentStep, totalSteps, repo.getId(), repo.getUrl());

                DownloadResult result = downloadFromRemoteWithRetry(artifact, repo.getUrl(), repo.getId());
                if (result.success) {
                    logger.info("  FOUND in POM repository: {} ({})", repo.getId(), repo.getUrl());
                    return DownloadResult.success("pom-repository:" + repo.getId(), result.path);
                } else if (result.notFound) {
                    logger.debug("    Not available in {}: 404 Not Found", repo.getId());
                } else {
                    logger.debug("    Failed to download from {}: {}", repo.getId(), result.errorMessage);
                }
            }

            logger.info("  Not found in any POM repository");
        }

        // ========== TIER-3: MAVEN CENTRAL (FALLBACK) ==========
        currentStep++;
        logger.info("--- TIER-3: MAVEN CENTRAL (FALLBACK) ---");
        logger.info("  [Step {}/{}] Downloading from Maven Central...", currentStep, totalSteps);

        DownloadResult centralResult = downloadFromRemoteWithRetry(artifact, MAVEN_CENTRAL_URL, "maven-central");
        if (centralResult.success) {
            logger.info("  FOUND in Maven Central. Saved to: {}", centralResult.path);
            return DownloadResult.success("maven-central", centralResult.path);
        } else if (centralResult.notFound) {
            logger.info("  Not available in Maven Central: 404 Not Found");
            return DownloadResult.notFound("Artifact JAR for " + coords + " not found in Maven Central");
        } else {
            logger.warn("  Failed to download from Maven Central: {}", centralResult.errorMessage);
            return DownloadResult.failure("Maven Central download failed: " + centralResult.errorMessage);
        }
    }

    /**
     * Attempts to download a JAR from a remote repository URL with retry logic.
     */
    private DownloadResult downloadFromRemoteWithRetry(Artifact artifact, String repoBaseUrl, String repoId) {
        String jarRelativePath = buildJarRelativePath(artifact);
        String jarUrl = repoBaseUrl.endsWith("/")
            ? repoBaseUrl + jarRelativePath
            : repoBaseUrl + "/" + jarRelativePath;

        // Handle SNAPSHOT versions — try maven-metadata.xml for timestamped version
        if (artifact.getVersion().endsWith("-SNAPSHOT")) {
            logger.debug("  SNAPSHOT detected for {}. Attempting timestamped resolution...", formatCoords(artifact));
            String timestampedUrl = resolveSnapshotUrl(artifact, repoBaseUrl);
            if (timestampedUrl != null) {
                logger.debug("  Resolved SNAPSHOT to timestamped URL: {}", timestampedUrl);
                jarUrl = timestampedUrl;
            } else {
                logger.debug("  Could not resolve SNAPSHOT timestamp. Using default SNAPSHOT URL.");
            }
        }

        Path targetPath = buildJarPath(artifact, defaultRepositoryPath);

        int maxRetries = MavenDownloadConstants.RETRY_COUNT;
        long backoffMs = MavenDownloadConstants.RETRY_BACKOFF_INITIAL_MS;
        long maxBackoffMs = MavenDownloadConstants.RETRY_BACKOFF_MAX_MS;

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            if (attempt > 1) {
                logger.info("    Retry attempt {}/{} for {} from {}", attempt - 1, maxRetries, formatCoords(artifact), repoId);
                try {
                    logger.debug("    Waiting {}ms before retry...", backoffMs);
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return DownloadResult.failure("Download interrupted during retry backoff");
                }
                backoffMs = Math.min(backoffMs * 2, maxBackoffMs);
            } else {
                logger.debug("    Attempt {}: downloading from {}", attempt, jarUrl);
            }

            try {
                URL url = URI.create(jarUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(MavenDownloadConstants.CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(MavenDownloadConstants.READ_TIMEOUT_MS);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "BlackDuck-Detect-MavenResolver");

                int responseCode = connection.getResponseCode();
                logger.debug("    HTTP response: {} from {}", responseCode, jarUrl);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Ensure parent directories exist
                    Files.createDirectories(targetPath.getParent());

                    // Download to a temp file first, then move atomically
                    Path tempFile = targetPath.getParent().resolve(targetPath.getFileName() + ".tmp");
                    try (InputStream in = connection.getInputStream()) {
                        Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    }

                    // Verify the file is not empty
                    long fileSize = Files.size(tempFile);
                    if (fileSize == 0) {
                        Files.deleteIfExists(tempFile);
                        logger.warn("    Downloaded file is empty (0 bytes). Discarding.");
                        continue; // retry
                    }

                    Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    logger.debug("    Downloaded {} bytes to {}", fileSize, targetPath);
                    connection.disconnect();
                    return DownloadResult.success(repoId, targetPath);

                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    connection.disconnect();
                    return DownloadResult.notFound("404 Not Found at " + jarUrl);

                } else if (responseCode >= 500) {
                    // Server error — worth retrying
                    logger.debug("    Server error (HTTP {}), will retry if attempts remain", responseCode);
                    connection.disconnect();
                    continue;

                } else {
                    connection.disconnect();
                    return DownloadResult.failure("HTTP " + responseCode + " from " + jarUrl);
                }

            } catch (IOException e) {
                logger.debug("    IOException on attempt {}: {}", attempt, e.getMessage());
                if (attempt > maxRetries) {
                    return DownloadResult.failure("IOException after " + maxRetries + " retries: " + e.getMessage());
                }
                // continue to retry
            }
        }

        return DownloadResult.failure("Exhausted all retry attempts for " + jarUrl);
    }

    /**
     * Attempts to resolve a SNAPSHOT artifact's timestamped version via maven-metadata.xml.
     *
     * @return the full URL to the timestamped JAR, or null if resolution fails
     */
    private String resolveSnapshotUrl(Artifact artifact, String repoBaseUrl) {
        String groupPath = artifact.getGroupId().replace('.', '/');
        String metadataUrl = repoBaseUrl
            + (repoBaseUrl.endsWith("/") ? "" : "/")
            + groupPath + "/"
            + artifact.getArtifactId() + "/"
            + artifact.getVersion() + "/maven-metadata.xml";

        try {
            URL url = URI.create(metadataUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(MavenDownloadConstants.CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(MavenDownloadConstants.READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "BlackDuck-Detect-MavenResolver");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect();
                logger.debug("  SNAPSHOT metadata not available (HTTP {}): {}", responseCode, metadataUrl);
                return null;
            }

            // Simple XML parsing for <timestamp> and <buildNumber>
            String metadataContent;
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                metadataContent = baos.toString("UTF-8");
            }
            connection.disconnect();

            String timestamp = extractXmlValue(metadataContent, "timestamp");
            String buildNumber = extractXmlValue(metadataContent, "buildNumber");

            if (timestamp != null && buildNumber != null) {
                String baseVersion = artifact.getVersion().replace("-SNAPSHOT", "");
                String timestampedFilename = artifact.getArtifactId() + "-" + baseVersion + "-" + timestamp + "-" + buildNumber + ".jar";
                String timestampedUrl = repoBaseUrl
                    + (repoBaseUrl.endsWith("/") ? "" : "/")
                    + groupPath + "/"
                    + artifact.getArtifactId() + "/"
                    + artifact.getVersion() + "/"
                    + timestampedFilename;
                logger.debug("  Resolved SNAPSHOT: timestamp={}, buildNumber={}", timestamp, buildNumber);
                return timestampedUrl;
            }

        } catch (Exception e) {
            logger.debug("  Failed to resolve SNAPSHOT metadata: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Extracts a simple XML element value (no attributes, no namespaces).
     */
    private String extractXmlValue(String xml, String tagName) {
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";
        int start = xml.indexOf(openTag);
        int end = xml.indexOf(closeTag);
        if (start >= 0 && end > start) {
            return xml.substring(start + openTag.length(), end).trim();
        }
        return null;
    }

    /**
     * Looks for a JAR in a local repository using standard Maven layout.
     */
    private Path findJarInRepository(Artifact artifact, Path repositoryRoot) {
        if (repositoryRoot == null || !repositoryRoot.toFile().isDirectory()) {
            return null;
        }

        Path jarPath = buildJarPath(artifact, repositoryRoot);

        if (Files.exists(jarPath) && Files.isRegularFile(jarPath)) {
            long fileSize = jarPath.toFile().length();
            logger.debug("  Found JAR at {} (size: {} bytes)", jarPath, fileSize);
            if (fileSize > 0) {
                return jarPath;
            } else {
                logger.warn("  Found JAR at {} but file is empty (0 bytes). Skipping.", jarPath);
            }
        }

        return null;
    }

    /**
     * Builds the expected JAR path within a Maven repository using standard layout.
     */
    private Path buildJarPath(Artifact artifact, Path repositoryRoot) {
        String groupPath = artifact.getGroupId().replace('.', '/');
        return repositoryRoot
            .resolve(groupPath)
            .resolve(artifact.getArtifactId())
            .resolve(artifact.getVersion())
            .resolve(artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar");
    }

    /**
     * Builds the relative path portion (groupId/artifactId/version/artifactId-version.jar).
     */
    private String buildJarRelativePath(Artifact artifact) {
        String groupPath = artifact.getGroupId().replace('.', '/');
        return groupPath + "/"
            + artifact.getArtifactId() + "/"
            + artifact.getVersion() + "/"
            + artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar";
    }

    private String formatCoords(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    /**
     * Internal result class for download operations.
     */
    private static class DownloadResult {
        final boolean success;
        final boolean notFound;
        final String source;
        final Path path;
        final String errorMessage;

        private DownloadResult(boolean success, boolean notFound, String source, Path path, String errorMessage) {
            this.success = success;
            this.notFound = notFound;
            this.source = source;
            this.path = path;
            this.errorMessage = errorMessage;
        }

        static DownloadResult success(String source, Path path) {
            return new DownloadResult(true, false, source, path, null);
        }

        static DownloadResult notFound(String message) {
            return new DownloadResult(false, true, null, null, message);
        }

        static DownloadResult failure(String errorMessage) {
            return new DownloadResult(false, false, null, null, errorMessage);
        }
    }
}

