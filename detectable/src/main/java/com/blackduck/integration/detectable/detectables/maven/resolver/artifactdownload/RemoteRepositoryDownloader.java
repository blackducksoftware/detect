package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import com.blackduck.integration.detectable.detectables.maven.resolver.exception.ArtifactDownloadException;
import com.blackduck.integration.detectable.detectables.maven.resolver.exception.DownloadErrorFactory;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Downloads artifacts from a remote Maven repository with SNAPSHOT support.
 * Single Responsibility: Handle HTTP downloads from a specific Maven repository.
 *
 * <p>Features:
 * <ul>
 *   <li>SNAPSHOT resolution via maven-metadata.xml (timestamped + plain fallback)</li>
 *   <li>Resumable downloads with Range header support</li>
 *   <li>Disk space checking before download</li>
 *   <li>Progress reporting for large files</li>
 *   <li>Retry logic via {@link RetryPolicy}</li>
 * </ul>
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

    /**
     * Returns the base repository URL.
     */
    public String getBaseRepositoryUrl() {
        return baseRepositoryUrl;
    }

    @Override
    public DownloadResult download(Artifact artifact, Path targetPath, DownloadConfiguration configuration)
            throws ArtifactDownloadException {

        String artifactId = formatArtifactId(artifact);
        RetryPolicy retryPolicy = configuration.getRetryPolicy();

        // Build URL with SNAPSHOT support
        String primaryJarUrl = buildRepositoryUrl(artifact);
        String plainSnapshotUrl = null;
        boolean isSnapshot = artifact.getVersion().endsWith("-SNAPSHOT");

        if (isSnapshot) {
            logger.debug("SNAPSHOT detected for {}. Attempting timestamped resolution...", artifactId);
            String timestampedUrl = resolveSnapshotUrl(artifact, configuration);
            if (timestampedUrl != null) {
                logger.debug("Resolved SNAPSHOT to timestamped URL: {}", timestampedUrl);
                plainSnapshotUrl = primaryJarUrl; // Keep plain URL as fallback
                primaryJarUrl = timestampedUrl;
            } else {
                logger.debug("Could not resolve SNAPSHOT timestamp. Using default SNAPSHOT URL.");
            }
        }

        logger.debug("Remote repository URL: {}", primaryJarUrl);

        // Check for partial download
        PartialDownloadManager.PartialDownloadInfo partialInfo =
            partialDownloadManager.checkPartialDownload(targetPath);

        // Track mutable state for resume attempts
        final long[] startOffset = { 0 };
        final Path[] downloadPath = { targetPath };

        if (partialInfo.isResumable()) {
            startOffset[0] = partialInfo.getBytesDownloaded();
            downloadPath[0] = partialInfo.getPartialFile();
            logger.debug("Attempting to resume download from byte {}", startOffset[0]);
        } else if (partialInfo.getPartialFile() != null) {
            partialDownloadManager.cleanupPartialFile(partialInfo.getPartialFile());
        }

        // Final URL references for lambda
        final String finalPrimaryUrl = primaryJarUrl;
        final String finalPlainSnapshotUrl = plainSnapshotUrl;

        // Use RetryPolicy for retry logic
        try {
            return retryPolicy.executeWithRetry(() -> {
                try {
                    DownloadResult result = attemptDownload(
                        artifact, targetPath, downloadPath[0], finalPrimaryUrl, configuration, startOffset[0]
                    );

                    if (result.isSuccess()) {
                        if (!downloadPath[0].equals(targetPath) && Files.exists(downloadPath[0])) {
                            partialDownloadManager.promotePartialToFinal(downloadPath[0], targetPath);
                        }
                        return result;
                    }

                    // If primary URL 404 and we have a plain SNAPSHOT fallback, try it
                    if (result.isNotFound() && finalPlainSnapshotUrl != null && !finalPrimaryUrl.equals(finalPlainSnapshotUrl)) {
                        logger.debug("Timestamped SNAPSHOT URL failed (404). Trying plain SNAPSHOT URL...");
                        DownloadResult fallbackResult = attemptDownload(
                            artifact, targetPath, downloadPath[0], finalPlainSnapshotUrl, configuration, startOffset[0]
                        );
                        if (fallbackResult.isSuccess()) {
                            if (!downloadPath[0].equals(targetPath) && Files.exists(downloadPath[0])) {
                                partialDownloadManager.promotePartialToFinal(downloadPath[0], targetPath);
                            }
                            return fallbackResult;
                        }
                        return fallbackResult;
                    }

                    if (isNonRetryableError(result)) {
                        return result;
                    }

                    throw DownloadErrorFactory.repositoryError(artifactId,
                        "Download failed: " + result.getErrorMessage(), null);

                } catch (ArtifactDownloadException e) {
                    if (e.getMessage() != null && e.getMessage().contains("404")) {
                        return DownloadResult.notFound("Artifact not found in repository (404)");
                    }

                    if (e.getCategory() == ArtifactDownloadException.ErrorCategory.RANGE_NOT_SUPPORTED_ERROR ||
                        e.getCategory() == ArtifactDownloadException.ErrorCategory.PARTIAL_DOWNLOAD_ERROR) {
                        partialDownloadManager.cleanupPartialFile(downloadPath[0]);
                        startOffset[0] = 0;
                        downloadPath[0] = targetPath;
                    }

                    throw e;
                } catch (IOException e) {
                    throw DownloadErrorFactory.fileSystemError(artifactId,
                        "Failed to promote partial download to final location", e);
                }
            }, "download from " + baseRepositoryUrl);

        } catch (ArtifactDownloadException e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                return DownloadResult.notFound("Artifact not found in repository (404)");
            }
            throw e;
        }
    }

    /**
     * Resolves a SNAPSHOT artifact's timestamped version via maven-metadata.xml.
     *
     * @param artifact The SNAPSHOT artifact
     * @param configuration Download configuration for timeouts
     * @return The full URL to the timestamped JAR, or null if resolution fails
     */
    private String resolveSnapshotUrl(Artifact artifact, DownloadConfiguration configuration) {
        String groupPath = artifact.getGroupId().replace('.', '/');
        String metadataUrl = baseRepositoryUrl + "/" + groupPath + "/"
            + artifact.getArtifactId() + "/"
            + artifact.getVersion() + "/maven-metadata.xml";

        HttpURLConnection connection = null;
        try {
            URL url = new URL(metadataUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(configuration.getConnectTimeoutMs());
            connection.setReadTimeout(configuration.getReadTimeoutMs());
            connection.setRequestProperty("User-Agent", "Black Duck Detect Maven Resolver");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                logger.debug("SNAPSHOT metadata not available (HTTP {}): {}", responseCode, metadataUrl);
                return null;
            }

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

            String timestamp = extractXmlValue(metadataContent, "timestamp");
            String buildNumber = extractXmlValue(metadataContent, "buildNumber");

            if (timestamp != null && buildNumber != null) {
                String baseVersion = artifact.getVersion().replace("-SNAPSHOT", "");
                StringBuilder fileName = new StringBuilder();
                fileName.append(artifact.getArtifactId());
                fileName.append("-").append(baseVersion);
                fileName.append("-").append(timestamp);
                fileName.append("-").append(buildNumber);

                String classifier = artifact.getClassifier();
                if (classifier != null && !classifier.isEmpty()) {
                    fileName.append("-").append(classifier);
                }
                fileName.append(".jar");

                String timestampedUrl = baseRepositoryUrl + "/" + groupPath + "/"
                    + artifact.getArtifactId() + "/"
                    + artifact.getVersion() + "/"
                    + fileName.toString();

                logger.debug("Resolved SNAPSHOT: timestamp={}, buildNumber={}", timestamp, buildNumber);
                return timestampedUrl;
            }

        } catch (Exception e) {
            logger.debug("Failed to resolve SNAPSHOT metadata: {}", e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
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

            if (startOffset > 0) {
                String rangeHeader = partialDownloadManager.buildRangeHeader(startOffset);
                connection.setRequestProperty("Range", rangeHeader);
                logger.debug("Requesting range: {}", rangeHeader);
            }

            int responseCode = connection.getResponseCode();

            if (startOffset > 0) {
                if (responseCode == 206) {
                    logger.debug("Server supports resume (206 Partial Content)");
                } else if (responseCode == 200) {
                    throw DownloadErrorFactory.rangeNotSupportedError(artifactId,
                        "Server returned 200 (not 206) for range request — this server does not support resume");
                } else {
                    throw DownloadErrorFactory.rangeNotSupportedError(artifactId,
                        "Server returned unexpected response to range request: " + responseCode);
                }
            }

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 206) {
                long contentLength = connection.getContentLengthLong();

                if (responseCode == 206) {
                    String contentRange = connection.getHeaderField("Content-Range");
                    long totalSize = partialDownloadManager.parseContentRangeTotal(contentRange);
                    if (totalSize > 0) {
                        contentLength = totalSize;
                    }
                }

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

                Files.createDirectories(targetPath.getParent());

                Path actualDownloadPath = (startOffset > 0) ? downloadPath :
                    partialDownloadManager.getPartialFilePath(targetPath);

                long bytesWritten = downloadWithProgress(
                    connection, actualDownloadPath, artifactId, contentLength, startOffset
                );

                if (contentLength > 0 && startOffset == 0 && bytesWritten != contentLength) {
                    Files.deleteIfExists(actualDownloadPath);
                    throw DownloadErrorFactory.networkError(artifactId,
                        String.format("Incomplete download: expected %d bytes, received %d bytes",
                            contentLength, bytesWritten), null);
                }

                logger.info("DOWNLOAD SUCCESSFUL from {} - {} KB downloaded",
                    baseRepositoryUrl, bytesWritten / 1024);

                if (!actualDownloadPath.equals(targetPath)) {
                    partialDownloadManager.promotePartialToFinal(actualDownloadPath, targetPath);
                }

                return DownloadResult.success(baseRepositoryUrl, targetPath);

            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                logger.debug("JAR NOT AVAILABLE - HTTP 404 from {}", baseRepositoryUrl);
                return DownloadResult.notFound("Artifact JAR not found in repository (404)");

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
        if (result.isNotFound()) {
            return true;
        }
        return result.getErrorMessage() != null &&
            (result.getErrorMessage().contains("403") ||
             result.getErrorMessage().contains("401"));
    }

    private String buildRepositoryUrl(Artifact artifact) {
        String groupPath = artifact.getGroupId().replace('.', '/');
        String version = artifact.getVersion();

        StringBuilder fileName = new StringBuilder();
        fileName.append(artifact.getArtifactId());
        fileName.append("-").append(version);

        String classifier = artifact.getClassifier();
        if (classifier != null && !classifier.isEmpty()) {
            fileName.append("-").append(classifier);
        }

        fileName.append(".jar");

        return String.format("%s/%s/%s/%s/%s",
            baseRepositoryUrl, groupPath, artifact.getArtifactId(), version, fileName.toString());
    }

    private String formatArtifactId(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    private String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

