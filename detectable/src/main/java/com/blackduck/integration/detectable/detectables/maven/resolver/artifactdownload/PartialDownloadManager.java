package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;

/**
 * Manages partial downloads and resume functionality using HTTP Range requests.
 * Single Responsibility: Handle partial download state and resume logic.
 */
public class PartialDownloadManager {

    private static final Logger logger = LoggerFactory.getLogger(PartialDownloadManager.class);
    private static final String PARTIAL_SUFFIX = ".partial";
    private static final Duration STALE_THRESHOLD = Duration.ofHours(24);

    /**
     * Information about a partial download that can be resumed.
     */
    public static class PartialDownloadInfo {
        private final Path partialFile;
        private final long bytesDownloaded;
        private final boolean isResumable;
        private final String reason;

        private PartialDownloadInfo(Path partialFile, long bytesDownloaded, boolean isResumable, String reason) {
            this.partialFile = partialFile;
            this.bytesDownloaded = bytesDownloaded;
            this.isResumable = isResumable;
            this.reason = reason;
        }

        public static PartialDownloadInfo resumable(Path partialFile, long bytesDownloaded) {
            return new PartialDownloadInfo(partialFile, bytesDownloaded, true, null);
        }

        public static PartialDownloadInfo notResumable(String reason) {
            return new PartialDownloadInfo(null, 0, false, reason);
        }

        public Path getPartialFile() {
            return partialFile;
        }

        public long getBytesDownloaded() {
            return bytesDownloaded;
        }

        public boolean isResumable() {
            return isResumable;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * Checks for an existing partial download and determines if it can be resumed.
     *
     * @param targetPath The final destination path for the download
     * @return PartialDownloadInfo indicating if resume is possible
     */
    public PartialDownloadInfo checkPartialDownload(Path targetPath) {
        if (targetPath == null) {
            return PartialDownloadInfo.notResumable("Target path is null");
        }

        // Normalize path to prevent directory traversal
        Path normalizedTarget;
        try {
            normalizedTarget = targetPath.normalize().toAbsolutePath();
        } catch (Exception e) {
            logger.warn("Failed to normalize target path, cannot check for partial download: {}", e.getMessage());
            return PartialDownloadInfo.notResumable("Path normalization failed");
        }

        // Check for partial file
        Path partialFile = getPartialFilePath(normalizedTarget);

        if (!Files.exists(partialFile)) {
            logger.debug("No partial download found for: {}", sanitizePath(normalizedTarget));
            return PartialDownloadInfo.notResumable("No partial file exists");
        }

        try {
            // Check file attributes
            BasicFileAttributes attrs = Files.readAttributes(partialFile, BasicFileAttributes.class);
            long fileSize = attrs.size();

            if (fileSize == 0) {
                logger.debug("Partial file is empty, removing: {}", sanitizePath(partialFile));
                Files.deleteIfExists(partialFile);
                return PartialDownloadInfo.notResumable("Partial file was empty");
            }

            // Check if partial file is stale
            Instant lastModified = attrs.lastModifiedTime().toInstant();
            Duration age = Duration.between(lastModified, Instant.now());

            if (age.compareTo(STALE_THRESHOLD) > 0) {
                logger.info("Partial download is stale ({}h old), removing: {}",
                    age.toHours(), sanitizePath(partialFile));
                Files.deleteIfExists(partialFile);
                return PartialDownloadInfo.notResumable("Partial file was stale");
            }

            // Validate file is readable
            if (!Files.isReadable(partialFile) || !Files.isWritable(partialFile)) {
                logger.warn("Partial file exists but is not accessible, removing: {}", sanitizePath(partialFile));
                Files.deleteIfExists(partialFile);
                return PartialDownloadInfo.notResumable("Partial file not accessible");
            }

            logger.info("Found resumable partial download: {} bytes already downloaded", fileSize);
            return PartialDownloadInfo.resumable(partialFile, fileSize);

        } catch (IOException e) {
            logger.warn("Error checking partial download, will start fresh: {}", e.getMessage());
            try {
                Files.deleteIfExists(partialFile);
            } catch (IOException deleteError) {
                logger.debug("Failed to delete partial file: {}", deleteError.getMessage());
            }
            return PartialDownloadInfo.notResumable("Error checking partial file");
        }
    }

    /**
     * Creates a path for the partial download file.
     *
     * @param targetPath The final destination path
     * @return Path to the partial file
     */
    public Path getPartialFilePath(Path targetPath) {
        return targetPath.resolveSibling(targetPath.getFileName() + PARTIAL_SUFFIX);
    }

    /**
     * Validates if server supports range requests based on response headers.
     *
     * @param responseCode HTTP response code
     * @param acceptRanges Accept-Ranges header value
     * @param contentRange Content-Range header value
     * @return true if server supports resuming
     */
    public boolean validateRangeSupport(int responseCode, String acceptRanges, String contentRange) {
        // Check for 206 Partial Content response
        if (responseCode == 206) {
            if (contentRange != null && contentRange.startsWith("bytes ")) {
                logger.debug("Server supports range requests (206 with Content-Range)");
                return true;
            }
        }

        // Check Accept-Ranges header for non-206 responses
        if (responseCode == 200 && acceptRanges != null && acceptRanges.contains("bytes")) {
            logger.debug("Server indicates range support via Accept-Ranges header");
            return true;
        }

        logger.debug("Server does not support range requests (code: {}, Accept-Ranges: {})",
            responseCode, acceptRanges);
        return false;
    }

    /**
     * Cleans up partial download file.
     *
     * @param partialFile Path to the partial file
     */
    public void cleanupPartialFile(Path partialFile) {
        if (partialFile == null) {
            return;
        }

        try {
            if (Files.exists(partialFile)) {
                Files.delete(partialFile);
                logger.debug("Cleaned up partial file: {}", sanitizePath(partialFile));
            }
        } catch (IOException e) {
            logger.warn("Failed to cleanup partial file {}: {}", sanitizePath(partialFile), e.getMessage());
        }
    }

    /**
     * Moves partial file to final destination atomically.
     *
     * @param partialFile Source partial file
     * @param targetPath Final destination
     * @throws IOException if move fails
     */
    public void promotePartialToFinal(Path partialFile, Path targetPath) throws IOException {
        if (partialFile == null || targetPath == null) {
            throw new IllegalArgumentException("Paths cannot be null");
        }

        // Ensure paths are normalized
        Path normalizedPartial = partialFile.normalize().toAbsolutePath();
        Path normalizedTarget = targetPath.normalize().toAbsolutePath();

        // Security check: ensure paths are in expected directories
        if (!normalizedPartial.getParent().equals(normalizedTarget.getParent())) {
            throw new SecurityException("Partial and target files must be in the same directory");
        }

        try {
            Files.move(normalizedPartial, normalizedTarget,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            logger.debug("Promoted partial download to final: {}", sanitizePath(normalizedTarget));
        } catch (IOException e) {
            logger.error("Failed to promote partial file to final: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Builds HTTP Range header value for resume.
     *
     * @param startByte The byte offset to start from
     * @return Range header value
     */
    public String buildRangeHeader(long startByte) {
        if (startByte < 0) {
            throw new IllegalArgumentException("Start byte cannot be negative");
        }

        // Validate reasonable range to prevent abuse
        if (startByte > 10L * 1024 * 1024 * 1024) { // 10GB max
            throw new IllegalArgumentException("Start byte exceeds maximum allowed range");
        }

        return "bytes=" + startByte + "-";
    }

    /**
     * Parses Content-Range header to extract total size.
     *
     * @param contentRange Content-Range header value
     * @return Total file size, or -1 if cannot parse
     */
    public long parseContentRangeTotal(String contentRange) {
        if (contentRange == null || !contentRange.startsWith("bytes ")) {
            return -1;
        }

        try {
            // Format: "bytes START-END/TOTAL" or "bytes START-END/*"
            int slashIndex = contentRange.lastIndexOf('/');
            if (slashIndex > 0 && slashIndex < contentRange.length() - 1) {
                String totalStr = contentRange.substring(slashIndex + 1);
                if (!"*".equals(totalStr)) {
                    return Long.parseLong(totalStr);
                }
            }
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse Content-Range total: {}", contentRange);
        }

        return -1;
    }

    /**
     * Sanitizes path for safe logging.
     */
    private String sanitizePath(Path path) {
        if (path == null) {
            return "<null>";
        }

        String fileName = path.getFileName() != null ? path.getFileName().toString() : "<unnamed>";
        Path parent = path.getParent();
        if (parent != null && parent.getFileName() != null) {
            return ".../" + parent.getFileName() + "/" + fileName;
        }
        return ".../" + fileName;
    }
}