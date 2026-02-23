package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import com.blackduck.integration.detectable.detectables.maven.resolver.exception.ArtifactDownloadException;
import com.blackduck.integration.detectable.detectables.maven.resolver.exception.DownloadErrorFactory;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Downloads artifacts from a remote Maven repository.
 * Single Responsibility: Handle downloads from POM-declared repositories.
 */
public class RemoteRepositoryDownloader implements HttpArtifactDownloader {

    private static final Logger logger = LoggerFactory.getLogger(RemoteRepositoryDownloader.class);

    private final String baseRepositoryUrl;
    private final PartialDownloadManager partialDownloadManager;
    private final DiskSpaceChecker diskSpaceChecker;

    /**
     * Creates a downloader for a specific remote repository.
     *
     * @param baseRepositoryUrl The base URL of the repository (e.g., "https://repo.example.com/maven2")
     */
    public RemoteRepositoryDownloader(String baseRepositoryUrl) {
        this.baseRepositoryUrl = normalizeUrl(baseRepositoryUrl);
        this.partialDownloadManager = new PartialDownloadManager();
        this.diskSpaceChecker = new DiskSpaceChecker();
    }

    @Override
    public DownloadResult download(Artifact artifact, Path targetPath, DownloadConfiguration configuration)
            throws ArtifactDownloadException {

        String artifactId = formatArtifactId(artifact);
        String jarUrl = buildRepositoryUrl(artifact);

        logger.debug("Remote repository URL: {}", jarUrl);

        // Check for partial download
        PartialDownloadManager.PartialDownloadInfo partialInfo =
            partialDownloadManager.checkPartialDownload(targetPath);

        long startOffset = 0;
        Path downloadPath = targetPath;

        if (partialInfo.isResumable()) {
            startOffset = partialInfo.getBytesDownloaded();
            downloadPath = partialInfo.getPartialFile();
            logger.debug("Attempting to resume download from byte {}", startOffset);
        } else if (partialInfo.getPartialFile() != null) {
            // Clean up non-resumable partial file
            partialDownloadManager.cleanupPartialFile(partialInfo.getPartialFile());
        }

        // Download with retries following same pattern as MavenCentralDownloader
        for (int attempt = 1; attempt <= configuration.getMaxRetries(); attempt++) {
            if (attempt > 1) {
                int sleepMs = configuration.getRetryDelayMs() * attempt;
                logger.debug("Retry {} of {} (waiting {}ms)...", attempt, configuration.getMaxRetries(), sleepMs);
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw DownloadErrorFactory.networkError(artifactId,
                        "Download interrupted during retry delay", e);
                }
            }

            logger.debug("Attempt {}/{}: Connecting to {}", attempt, configuration.getMaxRetries(), baseRepositoryUrl);

            try {
                DownloadResult result = attemptDownload(
                    artifact, targetPath, downloadPath, jarUrl, configuration, startOffset
                );

                if (result.isSuccess()) {
                    // If we used a partial file, promote it to final
                    if (!downloadPath.equals(targetPath) && Files.exists(downloadPath)) {
                        try {
                            partialDownloadManager.promotePartialToFinal(downloadPath, targetPath);
                        } catch (IOException e) {
                            throw DownloadErrorFactory.fileSystemError(artifactId,
                                    "Failed to promote partial download to final location", e);
                        }
                    }
                    return result;
                }

                // Check if error is non-retryable (404 is non-retryable for remote repos)
                if (isNonRetryableError(result)) {
                    return result;
                }

            } catch (ArtifactDownloadException e) {
                // For 404 errors, return immediately without retrying
                if (e.getMessage() != null && e.getMessage().contains("404")) {
                    logger.debug("Artifact not found in repository (404): {}", baseRepositoryUrl);
                    return DownloadResult.failure("Artifact not found in repository (404)");
                }

                if (attempt == configuration.getMaxRetries()) {
                    throw e;
                }
                logger.debug("Attempt {}/{} failed: {}", attempt, configuration.getMaxRetries(),
                    e.getSanitizedMessage());

                // If resume failed, try fresh download on next attempt
                if (e.getCategory() == ArtifactDownloadException.ErrorCategory.RANGE_NOT_SUPPORTED_ERROR ||
                    e.getCategory() == ArtifactDownloadException.ErrorCategory.PARTIAL_DOWNLOAD_ERROR) {
                    partialDownloadManager.cleanupPartialFile(downloadPath);
                    startOffset = 0;
                    downloadPath = targetPath;
                }
            }
        }

        throw DownloadErrorFactory.networkError(artifactId,
            String.format("Failed to download from %s after %d attempts", baseRepositoryUrl, configuration.getMaxRetries()),
            null);
    }

    private DownloadResult attemptDownload(Artifact artifact, Path targetPath, Path downloadPath,
                                          String jarUrl, DownloadConfiguration config, long startOffset)
            throws ArtifactDownloadException {

        String artifactId = formatArtifactId(artifact);
        HttpURLConnection connection = null;

        try {
            URL url = new URL(jarUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(config.getConnectTimeoutMs());
            connection.setReadTimeout(config.getReadTimeoutMs());
            connection.setRequestProperty("User-Agent", "Black Duck Detect Maven Resolver");

            // Add Range header if resuming
            if (startOffset > 0) {
                String rangeHeader = partialDownloadManager.buildRangeHeader(startOffset);
                connection.setRequestProperty("Range", rangeHeader);
                logger.debug("Requesting range: {}", rangeHeader);
            }

            int responseCode = connection.getResponseCode();

            // Handle resume response
            if (startOffset > 0) {
                if (responseCode == 206) {
                    // Partial content - resume supported
                    logger.debug("Server supports resume (206 Partial Content)");
                } else if (responseCode == 200) {
                    // Full content - resume not supported
                    logger.debug("Server does not support resume, starting fresh download");
                    partialDownloadManager.cleanupPartialFile(downloadPath);
                    startOffset = 0;
                    downloadPath = targetPath;

                    // Reconnect without Range header
                    connection.disconnect();
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(config.getConnectTimeoutMs());
                    connection.setReadTimeout(config.getReadTimeoutMs());
                    connection.setRequestProperty("User-Agent", "Black Duck Detect Maven Resolver");
                    responseCode = connection.getResponseCode();
                } else {
                    throw DownloadErrorFactory.rangeNotSupportedError(artifactId,
                        "Server returned unexpected response to range request: " + responseCode);
                }
            }

            // Handle response codes
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 206) {
                long contentLength = connection.getContentLengthLong();

                // Parse total size from Content-Range if available
                if (responseCode == 206) {
                    String contentRange = connection.getHeaderField("Content-Range");
                    long totalSize = partialDownloadManager.parseContentRangeTotal(contentRange);
                    if (totalSize > 0) {
                        contentLength = totalSize;
                    }
                }

                // Check disk space before downloading
                if (contentLength > 0) {
                    DiskSpaceChecker.DiskSpaceCheckResult spaceCheck =
                        diskSpaceChecker.checkAvailableSpace(targetPath, contentLength, startOffset);

                    if (!spaceCheck.hasEnoughSpace()) {
                        throw DownloadErrorFactory.insufficientDiskSpaceError(
                            artifactId,
                            spaceCheck.getAvailableBytes(),
                            spaceCheck.getRequiredBytes()
                        );
                    }
                }

                logger.debug("HTTP {} - Downloading JAR ({} KB) from {}",
                    responseCode,
                    contentLength > 0 ? contentLength / 1024 : "unknown size",
                    baseRepositoryUrl);

                // Create parent directories if needed
                Files.createDirectories(targetPath.getParent());

                // Determine final download path
                Path actualDownloadPath = (startOffset > 0) ? downloadPath :
                    partialDownloadManager.getPartialFilePath(targetPath);

                // Download with progress reporting
                long bytesWritten = downloadWithProgress(
                    connection,
                    actualDownloadPath,
                    artifactId,
                    contentLength,
                    startOffset
                );

                // Verify download integrity if content length was provided
                if (contentLength > 0 && startOffset == 0) {
                    if (bytesWritten != contentLength) {
                        Files.deleteIfExists(actualDownloadPath);
                        throw DownloadErrorFactory.networkError(artifactId,
                            String.format("Incomplete download: expected %d bytes, received %d bytes",
                                contentLength, bytesWritten), null);
                    }
                }

                logger.info("DOWNLOAD SUCCESSFUL from {} - {} KB downloaded",
                    baseRepositoryUrl, bytesWritten / 1024);

                // Move partial file to final location if needed
                if (!actualDownloadPath.equals(targetPath)) {
                    partialDownloadManager.promotePartialToFinal(actualDownloadPath, targetPath);
                }

                return DownloadResult.success(baseRepositoryUrl, targetPath);

            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                logger.debug("JAR NOT AVAILABLE - HTTP 404 from {}", baseRepositoryUrl);
                return DownloadResult.failure("Artifact JAR not found in repository (404)");

            } else {
                throw DownloadErrorFactory.fromHttpError(artifactId, responseCode);
            }

        } catch (IOException e) {
            throw DownloadErrorFactory.fromIOException(artifactId, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private long downloadWithProgress(HttpURLConnection connection, Path targetPath,
                                      String artifactId, long totalSize, long startOffset)
            throws IOException {

        // Open output stream in append mode if resuming
        StandardOpenOption[] openOptions = (startOffset > 0) ?
            new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.APPEND } :
            new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                                       StandardOpenOption.TRUNCATE_EXISTING };

        try (InputStream inputStream = connection.getInputStream();
             ProgressReportingInputStream progressStream = new ProgressReportingInputStream(
                 inputStream, artifactId, totalSize, startOffset);
             OutputStream outputStream = Files.newOutputStream(targetPath, openOptions)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesWritten = 0;

            while ((bytesRead = progressStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesWritten += bytesRead;
            }

            return totalBytesWritten;
        }
    }

    private boolean isNonRetryableError(DownloadResult result) {
        return result.getErrorMessage() != null &&
            (result.getErrorMessage().contains("404") ||
             result.getErrorMessage().contains("403") ||
             result.getErrorMessage().contains("401"));
    }

    private String buildRepositoryUrl(Artifact artifact) {
        String groupPath = artifact.getGroupId().replace('.', '/');
        String artifactPath = artifact.getArtifactId();
        String version = artifact.getVersion();

        // Build filename with classifier support
        StringBuilder fileName = new StringBuilder();
        fileName.append(artifact.getArtifactId());
        fileName.append("-");
        fileName.append(version);

        String classifier = artifact.getClassifier();
        if (classifier != null && !classifier.isEmpty()) {
            fileName.append("-");
            fileName.append(classifier);
        }

        fileName.append(".jar");

        return String.format("%s/%s/%s/%s/%s",
            baseRepositoryUrl, groupPath, artifactPath, version, fileName.toString());
    }

    private String formatArtifactId(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    private String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        // Remove trailing slash for consistency
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}