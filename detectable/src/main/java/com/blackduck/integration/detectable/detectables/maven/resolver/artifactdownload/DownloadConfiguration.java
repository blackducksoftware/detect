package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unified configuration for artifact downloads.
 * Single Responsibility: Encapsulate HTTP and retry settings for downloads.
 * Open/Closed: Extend via composition with RetryPolicy; sealed against modification.
 *
 * <p>This class consolidates all download-related configuration:
 * <ul>
 *   <li>HTTP connection settings (connect/read timeouts)</li>
 *   <li>Retry policy (max retries, backoff delays)</li>
 * </ul>
 */
public final class DownloadConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DownloadConfiguration.class);

    // Default values matching previous MavenDownloadConstants
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 30_000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 60_000;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_RETRY_BACKOFF_INITIAL_MS = 1_000L;
    private static final long DEFAULT_RETRY_BACKOFF_MAX_MS = 30_000L;
    private static final int DEFAULT_DOWNLOAD_THREADS = 4;

    // Validation bounds
    private static final int MAX_TIMEOUT_MS = 600_000; // 10 minutes max
    private static final int MIN_TIMEOUT_MS = 1;

    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int downloadThreads;
    private final RetryPolicy retryPolicy;

    /**
     * Private constructor - use builder or factory methods.
     */
    private DownloadConfiguration(int connectTimeoutMs, int readTimeoutMs, int downloadThreads, RetryPolicy retryPolicy) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.downloadThreads = downloadThreads;
        this.retryPolicy = retryPolicy;
    }

    /**
     * Creates default configuration with sensible defaults.
     * These values are tuned to work across a wide range of network environments.
     *
     * @return Default download configuration
     */
    public static DownloadConfiguration createDefault() {
        return new Builder().build();
    }

    /**
     * Creates configuration from legacy parameters for backward compatibility.
     *
     * @param maxRetries Maximum retry attempts
     * @param retryDelayMs Initial retry delay in milliseconds
     * @param connectTimeoutMs Connection timeout in milliseconds
     * @param readTimeoutMs Read timeout in milliseconds
     * @return Download configuration
     * @deprecated Use {@link Builder} instead for new code
     */
    @Deprecated
    public static DownloadConfiguration fromLegacy(int maxRetries, int retryDelayMs, int connectTimeoutMs, int readTimeoutMs) {
        return new Builder()
            .connectTimeoutMs(connectTimeoutMs)
            .readTimeoutMs(readTimeoutMs)
            .retryPolicy(new RetryPolicy(maxRetries, (long) retryDelayMs, DEFAULT_RETRY_BACKOFF_MAX_MS))
            .build();
    }

    /**
     * Creates configuration from optional user properties.
     * Falls back to defaults for null or invalid values.
     *
     * @param connectTimeout Optional connection timeout
     * @param readTimeout Optional read timeout
     * @return Download configuration
     */
    public static DownloadConfiguration fromProperties(Integer connectTimeout, Integer readTimeout) {
        Builder builder = new Builder();
        if (connectTimeout != null && connectTimeout > 0) {
            builder.connectTimeoutMs(connectTimeout);
        }
        if (readTimeout != null && readTimeout > 0) {
            builder.readTimeoutMs(readTimeout);
        }
        return builder.build();
    }

    // Getters

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public int getDownloadThreads() {
        return downloadThreads;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    /**
     * Convenience method to get max retries from retry policy.
     * @return Maximum retry attempts
     */
    public int getMaxRetries() {
        return retryPolicy.getMaxRetries();
    }

    /**
     * Convenience method to get initial backoff from retry policy.
     * @return Initial retry delay in milliseconds
     * @deprecated Use {@link #getRetryPolicy()} and call its methods directly
     */
    @Deprecated
    public int getRetryDelayMs() {
        return (int) retryPolicy.getInitialBackoffMs();
    }

    @Override
    public String toString() {
        return String.format(
            "DownloadConfiguration{connectTimeout=%dms, readTimeout=%dms, threads=%d, maxRetries=%d, initialBackoff=%dms, maxBackoff=%dms}",
            connectTimeoutMs, readTimeoutMs, downloadThreads,
            retryPolicy.getMaxRetries(), retryPolicy.getInitialBackoffMs(), retryPolicy.getMaxBackoffMs()
        );
    }

    /**
     * Builder for DownloadConfiguration.
     * Allows flexible configuration while maintaining immutability of the result.
     */
    public static final class Builder {
        private int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
        private int readTimeoutMs = DEFAULT_READ_TIMEOUT_MS;
        private int downloadThreads = DEFAULT_DOWNLOAD_THREADS;
        private RetryPolicy retryPolicy;

        public Builder() {
            // Default retry policy
            this.retryPolicy = new RetryPolicy(
                DEFAULT_MAX_RETRIES,
                DEFAULT_RETRY_BACKOFF_INITIAL_MS,
                DEFAULT_RETRY_BACKOFF_MAX_MS
            );
        }

        public Builder connectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = validateTimeout("connect timeout", connectTimeoutMs);
            return this;
        }

        public Builder readTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = validateTimeout("read timeout", readTimeoutMs);
            return this;
        }

        public Builder downloadThreads(int downloadThreads) {
            if (downloadThreads < 1) {
                logger.warn("Download threads must be at least 1, got {}. Using 1.", downloadThreads);
                this.downloadThreads = 1;
            } else if (downloadThreads > 20) {
                logger.warn("Download threads {} exceeds maximum (20). Using 20.", downloadThreads);
                this.downloadThreads = 20;
            } else {
                this.downloadThreads = downloadThreads;
            }
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy != null ? retryPolicy : RetryPolicy.defaultPolicy();
            return this;
        }

        /**
         * Convenience method to configure retries inline.
         */
        public Builder retries(int maxRetries, long initialBackoffMs, long maxBackoffMs) {
            this.retryPolicy = new RetryPolicy(maxRetries, initialBackoffMs, maxBackoffMs);
            return this;
        }

        public DownloadConfiguration build() {
            logger.debug("Building DownloadConfiguration: connectTimeout={}ms, readTimeout={}ms, threads={}, retryPolicy={}",
                connectTimeoutMs, readTimeoutMs, downloadThreads, retryPolicy);
            return new DownloadConfiguration(connectTimeoutMs, readTimeoutMs, downloadThreads, retryPolicy);
        }

        private int validateTimeout(String name, int value) {
            if (value < MIN_TIMEOUT_MS) {
                logger.warn("{} must be positive, got {}. Using {}ms.", name, value, MIN_TIMEOUT_MS);
                return MIN_TIMEOUT_MS;
            }
            if (value > MAX_TIMEOUT_MS) {
                logger.warn("{} {}ms exceeds maximum ({}ms). Using maximum.", name, value, MAX_TIMEOUT_MS);
                return MAX_TIMEOUT_MS;
            }
            return value;
        }
    }
}
