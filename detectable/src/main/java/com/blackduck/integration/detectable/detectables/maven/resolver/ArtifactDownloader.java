package com.blackduck.integration.detectable.detectables.maven.resolver;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Downloads and caches Maven artifact JAR files from local and remote repositories.
 *
 * <p>This class handles:
 * <ul>
 *   <li>Checking local Maven repository (.m2) for cached artifacts</li>
 *   <li>Downloading from Maven Central with retry logic</li>
 *   <li>Handling timestamped SNAPSHOT versions</li>
 *   <li>Caching downloaded artifacts for reuse</li>
 * </ul>
 */
public class ArtifactDownloader {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactDownloader.class);

    private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;

    private final Path customRepositoryPath;  // User's custom JAR location (optional)
    private final Path defaultRepositoryPath; // Default .m2/repository location

    /**
     * Constructs an ArtifactDownloader.
     *
     * @param customRepositoryPath Optional custom path to check for existing JARs. Can be either:
     *                            (1) A repository directory using Maven layout (groupId/artifactId/version/file.jar), or
     *                            (2) A direct path to a specific JAR file.
     *                            This location is checked first if provided. Can be null.
     * @param defaultRepositoryPath Default Maven repository path (~/.m2/repository).
     *                             Used as fallback for lookups and as the destination for all downloads.
     * @throws IllegalStateException if defaultRepositoryPath cannot be created or is not writable
     */
    public ArtifactDownloader(Path customRepositoryPath, Path defaultRepositoryPath) {
        this.customRepositoryPath = customRepositoryPath;
        this.defaultRepositoryPath = defaultRepositoryPath;

        // Ensure default repository directory exists and is writable (where we'll download to)
        try {
            Files.createDirectories(defaultRepositoryPath);
            logger.debug("Default Maven repository initialized at: {}", defaultRepositoryPath);

            // Verify write permissions
            if (!Files.isWritable(defaultRepositoryPath)) {
                String errorMsg = "Default Maven repository is not writable: " + defaultRepositoryPath;
                logger.error(errorMsg);
                throw new IllegalStateException(errorMsg + ". Please check file system permissions.");
            }

            logger.debug("Write permissions verified for: {}", defaultRepositoryPath);
        } catch (IOException e) {
            String errorMsg = "Failed to create default Maven repository directory: " + defaultRepositoryPath;
            logger.error(errorMsg + " - {}", e.getMessage());
            throw new IllegalStateException(errorMsg + ". Please check file system permissions and disk space.", e);
        }

        if (customRepositoryPath != null) {
            logger.info("Custom JAR repository configured: {}", customRepositoryPath);
            if (!Files.exists(customRepositoryPath)) {
                logger.warn("Custom JAR repository does not exist: {}", customRepositoryPath);
                logger.warn("Will fall back to default .m2 repository for JAR lookups");
            }
        }
    }

    /**
     * Downloads JAR files for all dependencies in the parsed result.
     *
     * @param dependencies List of Aether dependencies to download
     */
    public void downloadArtifacts(List<Dependency> dependencies) {
        logger.info("========================================");
        logger.info("STARTING ARTIFACT JAR DOWNLOAD PHASE");
        logger.info("========================================");
        logger.info("Total dependencies to process: {}", dependencies.size());

        if (dependencies.isEmpty()) {
            logger.info("No dependencies to download. Skipping JAR download phase.");
            logger.info("JAR DOWNLOAD PHASE COMPLETED: No artifacts to process");
            return;
        }

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
                    logger.info("✓ SUCCESS: JAR fetched from {} for {}", result.getSource(), artifactId);
                    if (result.getPath() != null) {
                        logger.debug("  Cached at: {}", result.getPath());
                    }
                    successCount++;
                } else {
                    if (result.getErrorMessage() != null && result.getErrorMessage().contains("not found")) {
                        logger.warn("✗ NOT AVAILABLE: JAR not found in any repository for {}", artifactId);
                        logger.debug("  Reason: {}", result.getErrorMessage());
                        unavailableArtifacts.add(artifactId);
                        skippedCount++;
                    } else {
                        logger.error("✗ FAILED: Could not fetch JAR for {}", artifactId);
                        logger.error("  Error: {}", result.getErrorMessage());
                        failureCount++;
                        failedArtifacts.add(artifactId);
                    }
                }
            } catch (Exception e) {
                logger.error("✗ ERROR: Unexpected exception while downloading {}", artifactId);
                logger.error("  Exception: {}", e.getMessage(), e);
                failureCount++;
                failedArtifacts.add(artifactId);
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
     * Downloads a single artifact JAR file.
     *
     * Flow:
     * 1. If custom repository path provided, check there first
     * 2. If not found in custom path (or no custom path), check default .m2/repository
     * 3. If not found anywhere, download from Maven Central to .m2/repository
     *
     * @param artifact Aether artifact to download
     * @return DownloadResult indicating success or failure with details
     */
    private DownloadResult downloadArtifact(Artifact artifact) {
        String artifactId = formatArtifactId(artifact);
        logger.debug("  Starting artifact resolution for: {}", artifactId);

        // Step 1: Check custom repository path (if provided)
        if (customRepositoryPath != null) {
            logger.info("  [Step 1/3] Checking custom repository...");
            logger.debug("  Custom repository path: {}", customRepositoryPath);

            // Check if custom path points directly to a JAR file
            if (Files.isRegularFile(customRepositoryPath)) {
                logger.debug("  Custom path is a direct JAR file");
                String expectedFileName = artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar";
                String actualFileName = customRepositoryPath.getFileName().toString();

                if (actualFileName.equals(expectedFileName)) {
                    long fileSize = getFileSize(customRepositoryPath);
                    logger.info("  ✓ FOUND direct JAR file ({}KB)", fileSize);
                    logger.debug("    File: {}", customRepositoryPath);
                    return DownloadResult.success("custom-jar-file", customRepositoryPath);
                } else {
                    logger.warn("  ✗ Custom JAR filename mismatch: expected '{}', found '{}'", expectedFileName, actualFileName);
                    logger.info("  ↓ Proceeding to check .m2/repository...");
                }
            } else {
                // Treat as repository directory
                logger.debug("  Custom path treated as repository directory");
                Path customJarPath = getRepositoryJarPath(artifact, customRepositoryPath);
                logger.debug("  Looking for: {}", customJarPath);

                if (Files.exists(customJarPath)) {
                    long fileSize = getFileSize(customJarPath);
                    logger.info("  ✓ FOUND in custom repository ({}KB)", fileSize);
                    logger.debug("    File path: {}", customJarPath);
                    return DownloadResult.success("custom-repository", customJarPath);
                }

                logger.info("  ✗ NOT FOUND in custom repository");
                logger.info("  ↓ Proceeding to check .m2/repository...");
            }
        }

        // Step 2: Check default .m2/repository
        int stepNumber = (customRepositoryPath != null) ? 2 : 1;
        int totalSteps = (customRepositoryPath != null) ? 3 : 2;
        logger.info("  [Step {}/{}] Checking .m2/repository...", stepNumber, totalSteps);
        logger.debug("  .m2 repository path: {}", defaultRepositoryPath);
        Path defaultJarPath = getRepositoryJarPath(artifact, defaultRepositoryPath);
        logger.debug("  Looking for: {}", defaultJarPath);

        if (Files.exists(defaultJarPath)) {
            long fileSize = getFileSize(defaultJarPath);
            logger.info("  ✓ FOUND in .m2/repository ({}KB)", fileSize);
            logger.debug("    File: {}", defaultJarPath.getFileName());
            return DownloadResult.success("m2-repository", defaultJarPath);
        }

        logger.info("  ✗ NOT FOUND in .m2/repository");
        logger.info("  ↓ Proceeding to download from Maven Central...");

        // Step 3: Download from Maven Central to .m2/repository
        stepNumber = (customRepositoryPath != null) ? 3 : 2;
        logger.info("  [Step {}/{}] Downloading from Maven Central...", stepNumber, totalSteps);
        return downloadFromMavenCentral(artifact, defaultJarPath);
    }

    /**
     * Safely gets file size in KB, returns 0 if error.
     */
    private long getFileSize(Path path) {
        try {
            return Files.size(path) / 1024; // KB
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Downloads artifact from Maven Central with retry logic.
     */
    private DownloadResult downloadFromMavenCentral(Artifact artifact, Path targetPath) {
        String artifactId = formatArtifactId(artifact);
        String jarUrl = buildMavenCentralUrl(artifact);

        logger.debug("  → Maven Central URL: {}", jarUrl);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            if (attempt > 1) {
                int sleepMs = RETRY_DELAY_MS * attempt;
                logger.info("  → Retry {} of {} (waiting {}ms)...", attempt, MAX_RETRIES, sleepMs);
                try {
                    Thread.sleep(sleepMs); // Exponential backoff
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("  → Download interrupted");
                    return DownloadResult.failure("Download interrupted: " + e.getMessage());
                }
            }

            logger.debug("  → Attempt {}/{}: Connecting to Maven Central...", attempt, MAX_RETRIES);

            HttpURLConnection connection = null;
            try {
                URL url = new URL(jarUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    long contentLength = connection.getContentLengthLong();
                    logger.info("  ✓ HTTP 200 OK - Downloading JAR ({} KB)...", contentLength / 1024);

                    Files.createDirectories(targetPath.getParent());
                    try (InputStream inputStream = connection.getInputStream()) {
                        long bytesWritten = Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

                        // Verify download integrity if content length was provided
                        if (contentLength > 0 && bytesWritten != contentLength) {
                            logger.error("  ✗ DOWNLOAD INCOMPLETE - Expected {} bytes, got {} bytes", contentLength, bytesWritten);
                            Files.deleteIfExists(targetPath);
                            if (attempt == MAX_RETRIES) {
                                return DownloadResult.failure("Incomplete download after " + MAX_RETRIES + " attempts");
                            }
                            continue; // Retry
                        }

                        logger.info("  ✓ DOWNLOAD SUCCESSFUL - Saved to .m2/repository ({}KB)", bytesWritten / 1024);
                        logger.debug("    Saved to: {}", targetPath);
                    }
                    return DownloadResult.success("maven-central", targetPath);
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    logger.info("  ✗ JAR NOT AVAILABLE - HTTP 404 from Maven Central");
                    logger.debug("    This artifact does not have a JAR in Maven Central (POM-only or unavailable)");
                    return DownloadResult.failure("Artifact JAR for " + artifactId + " not found in Maven Central (404)");
                } else {
                    logger.warn("  ⚠ HTTP {} response from Maven Central", responseCode);
                    if (attempt == MAX_RETRIES) {
                        logger.error("  ✗ DOWNLOAD FAILED - Exhausted all {} retry attempts", MAX_RETRIES);
                        return DownloadResult.failure("Failed after " + MAX_RETRIES + " attempts (last HTTP " + responseCode + ")");
                    }
                }
            } catch (IOException e) {
                logger.warn("  ⚠ Attempt {}/{} failed: {}", attempt, MAX_RETRIES, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    logger.error("  ✗ DOWNLOAD FAILED - Exhausted all {} retry attempts", MAX_RETRIES);
                    logger.error("    Last error: {}", e.getMessage());
                    return DownloadResult.failure("Failed after " + MAX_RETRIES + " attempts: " + e.getMessage());
                }
            } finally {
                // CRITICAL: Always disconnect to release resources
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        logger.error("  → Download failed after all retry attempts");
        return DownloadResult.failure("Failed to download after " + MAX_RETRIES + " attempts");
    }

    /**
     * Builds Maven Central URL for the artifact JAR.
     */
    private String buildMavenCentralUrl(Artifact artifact) {
        String groupPath = artifact.getGroupId().replace('.', '/');
        String artifactPath = artifact.getArtifactId();
        String version = artifact.getVersion();
        String fileName = artifact.getArtifactId() + "-" + version + ".jar";

        return String.format("%s/%s/%s/%s/%s",
            MAVEN_CENTRAL_URL, groupPath, artifactPath, version, fileName);
    }

    /**
     * Gets the path where the artifact should be stored in the Maven repository.
     * Uses standard Maven repository layout: groupId/artifactId/version/artifactId-version.jar
     *
     * @param artifact The Maven artifact
     * @param repositoryPath The base repository path
     * @return Full path to the JAR file in the repository
     */
    private Path getRepositoryJarPath(Artifact artifact, Path repositoryPath) {
        String groupPath = artifact.getGroupId().replace('.', File.separatorChar);
        String artifactPath = artifact.getArtifactId();
        String version = artifact.getVersion();
        String fileName = artifact.getArtifactId() + "-" + version + ".jar";

        return repositoryPath
            .resolve(groupPath)
            .resolve(artifactPath)
            .resolve(version)
            .resolve(fileName);
    }

    /**
     * Formats artifact coordinates as a readable string.
     */
    private String formatArtifactId(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    /**
     * Result of an artifact download operation.
     */
    private static class DownloadResult {
        private final boolean success;
        private final String source;
        private final Path path;
        private final String errorMessage;

        private DownloadResult(boolean success, String source, Path path, String errorMessage) {
            this.success = success;
            this.source = source;
            this.path = path;
            this.errorMessage = errorMessage;
        }

        static DownloadResult success(String source, Path path) {
            return new DownloadResult(true, source, path, null);
        }

        static DownloadResult failure(String errorMessage) {
            return new DownloadResult(false, null, null, errorMessage);
        }

        boolean isSuccess() {
            return success;
        }

        String getSource() {
            return source;
        }

        Path getPath() {
            return path;
        }

        String getErrorMessage() {
            return errorMessage;
        }
    }
}
