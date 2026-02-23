package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Input stream wrapper that reports download progress at controlled intervals.
 * Single Responsibility: Track and report download progress.
 */
public class ProgressReportingInputStream extends FilterInputStream {

    private static final Logger logger = LoggerFactory.getLogger(ProgressReportingInputStream.class);

    // Progress reporting thresholds
    private static final long BYTES_PER_REPORT = 1024 * 1024; // 1 MB
    private static final long TIME_BETWEEN_REPORTS_MS = 2000; // 2 seconds
    private static final long BYTES_PER_KB = 1024;
    private static final long BYTES_PER_MB = 1024 * 1024;

    private final String artifactId;
    private final long totalSize;
    private final long startOffset;
    private final AtomicLong bytesRead;
    private final Instant startTime;

    private long lastReportBytes;
    private long lastReportTimeMs;
    private boolean completed;

    /**
     * Creates a progress-reporting input stream.
     *
     * @param inputStream The underlying input stream
     * @param artifactId Artifact identifier for logging
     * @param totalSize Total expected size in bytes (-1 if unknown)
     * @param startOffset Starting byte offset for resumed downloads
     */
    public ProgressReportingInputStream(InputStream inputStream, String artifactId,
                                        long totalSize, long startOffset) {
        super(inputStream);
        this.artifactId = sanitizeArtifactId(artifactId);
        this.totalSize = totalSize;
        this.startOffset = Math.max(0, startOffset);
        this.bytesRead = new AtomicLong(0);
        this.startTime = Instant.now();
        this.lastReportBytes = 0;
        this.lastReportTimeMs = System.currentTimeMillis();
        this.completed = false;

        // Log initial state
        if (startOffset > 0) {
            logger.info("  → Resuming download from byte {}", formatBytes(startOffset));
        }
        if (totalSize > 0) {
            logger.info("  → Total size: {}", formatBytes(totalSize));
        }
    }

    @Override
    public int read() throws IOException {
        int byteRead = super.read();
        if (byteRead != -1) {
            updateProgress(1);
        } else {
            reportCompletion();
        }
        return byteRead;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesReadCount = super.read(b, off, len);
        if (bytesReadCount > 0) {
            updateProgress(bytesReadCount);
        } else if (bytesReadCount == -1) {
            reportCompletion();
        }
        return bytesReadCount;
    }

    @Override
    public void close() throws IOException {
        try {
            reportCompletion();
        } finally {
            super.close();
        }
    }

    /**
     * Updates progress and reports if thresholds are met.
     */
    private void updateProgress(int bytes) {
        long totalBytesRead = bytesRead.addAndGet(bytes);
        long currentTimeMs = System.currentTimeMillis();

        // Check if we should report progress
        boolean shouldReport = false;

        // Report based on bytes threshold
        if (totalBytesRead - lastReportBytes >= BYTES_PER_REPORT) {
            shouldReport = true;
        }

        // Report based on time threshold
        if (currentTimeMs - lastReportTimeMs >= TIME_BETWEEN_REPORTS_MS) {
            shouldReport = true;
        }

        if (shouldReport) {
            reportProgress(totalBytesRead, currentTimeMs);
            lastReportBytes = totalBytesRead;
            lastReportTimeMs = currentTimeMs;
        }
    }

    /**
     * Reports current download progress.
     */
    private void reportProgress(long currentBytes, long currentTimeMs) {
        long totalDownloaded = startOffset + currentBytes;

        // Build progress message
        StringBuilder message = new StringBuilder();
        message.append("  → Progress: ");
        message.append(formatBytes(totalDownloaded));

        // Add total size and percentage if known
        if (totalSize > 0) {
            double percentage = (totalDownloaded * 100.0) / totalSize;
            message.append(" / ");
            message.append(formatBytes(totalSize));
            message.append(String.format(" (%.1f%%)", percentage));
        }

        // Calculate and add download speed
        long elapsedMs = currentTimeMs - startTime.toEpochMilli();
        if (elapsedMs > 0 && currentBytes > 0) {
            double bytesPerSecond = (currentBytes * 1000.0) / elapsedMs;
            message.append(" - ");
            message.append(formatSpeed(bytesPerSecond));
        }

        // Add time estimate if possible
        if (totalSize > 0 && currentBytes > 0 && elapsedMs > 0) {
            long remainingBytes = totalSize - totalDownloaded;
            double bytesPerMs = currentBytes / (double) elapsedMs;
            if (bytesPerMs > 0) {
                long remainingMs = (long) (remainingBytes / bytesPerMs);
                if (remainingMs > 0) {
                    message.append(" - ");
                    message.append(formatDuration(Duration.ofMillis(remainingMs)));
                    message.append(" remaining");
                }
            }
        }

        logger.info(message.toString());
    }

    /**
     * Reports download completion.
     */
    private void reportCompletion() {
        if (completed) {
            return;
        }
        completed = true;

        long totalBytesRead = bytesRead.get();
        long totalDownloaded = startOffset + totalBytesRead;
        Duration totalDuration = Duration.between(startTime, Instant.now());

        StringBuilder message = new StringBuilder();
        message.append("  → Download complete: ");
        message.append(formatBytes(totalDownloaded));

        if (totalDuration.toMillis() > 0) {
            message.append(" in ");
            message.append(formatDuration(totalDuration));

            // Add average speed
            double avgBytesPerSecond = (totalBytesRead * 1000.0) / totalDuration.toMillis();
            message.append(" (avg ");
            message.append(formatSpeed(avgBytesPerSecond));
            message.append(")");
        }

        logger.info(message.toString());
    }

    /**
     * Formats bytes into human-readable string.
     */
    private String formatBytes(long bytes) {
        if (bytes < BYTES_PER_KB) {
            return bytes + " B";
        } else if (bytes < BYTES_PER_MB) {
            return String.format("%.1f KB", bytes / (double) BYTES_PER_KB);
        } else {
            return String.format("%.1f MB", bytes / (double) BYTES_PER_MB);
        }
    }

    /**
     * Formats download speed into human-readable string.
     */
    private String formatSpeed(double bytesPerSecond) {
        if (bytesPerSecond < BYTES_PER_KB) {
            return String.format("%.0f B/s", bytesPerSecond);
        } else if (bytesPerSecond < BYTES_PER_MB) {
            return String.format("%.1f KB/s", bytesPerSecond / BYTES_PER_KB);
        } else {
            return String.format("%.1f MB/s", bytesPerSecond / BYTES_PER_MB);
        }
    }

    /**
     * Formats duration into human-readable string.
     */
    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        } else {
            return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60);
        }
    }

    /**
     * Sanitizes artifact ID to prevent log injection.
     */
    private String sanitizeArtifactId(String artifactId) {
        if (artifactId == null) {
            return "unknown";
        }
        // Remove any control characters or newlines
        return artifactId.replaceAll("[\\r\\n\\t]", "");
    }
}