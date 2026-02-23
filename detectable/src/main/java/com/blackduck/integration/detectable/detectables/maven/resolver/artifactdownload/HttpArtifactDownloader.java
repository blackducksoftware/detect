package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import com.blackduck.integration.detectable.detectables.maven.resolver.exception.ArtifactDownloadException;
import org.eclipse.aether.artifact.Artifact;

import java.nio.file.Path;

/**
 * Interface for downloading Maven artifacts via HTTP.
 * Interface Segregation Principle: Focused interface for HTTP downloads only.
 */

public interface HttpArtifactDownloader {

    /**
     * Downloads an artifact from a Maven repository.
     *
     * @param artifact The artifact to download
     * @param targetPath The path where the artifact should be saved
     * @param configuration Download configuration including timeouts
     * @return The result of the download operation
     * @throws ArtifactDownloadException if download fails
     */
    DownloadResult download(Artifact artifact, Path targetPath, DownloadConfiguration configuration)
        throws ArtifactDownloadException;

    /**
     * Result of a download operation.
     */
    class DownloadResult {
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

        public static DownloadResult success(String source, Path path) {
            return new DownloadResult(true, source, path, null);
        }

        public static DownloadResult failure(String errorMessage) {
            return new DownloadResult(false, null, null, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getSource() {
            return source;
        }

        public Path getPath() {
            return path;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Configuration for artifact downloads.
     */
    class DownloadConfiguration {
        private final int connectTimeoutMs;
        private final int readTimeoutMs;
        private final int maxRetries;
        private final int retryDelayMs;

        public DownloadConfiguration(int connectTimeoutMs, int readTimeoutMs, int maxRetries, int retryDelayMs) {
            validateTimeout("connect timeout", connectTimeoutMs);
            validateTimeout("read timeout", readTimeoutMs);
            validatePositive("max retries", maxRetries);
            validatePositive("retry delay", retryDelayMs);

            this.connectTimeoutMs = connectTimeoutMs;
            this.readTimeoutMs = readTimeoutMs;
            this.maxRetries = maxRetries;
            this.retryDelayMs = retryDelayMs;
        }

        private void validateTimeout(String name, int value) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be positive, got: " + value + "ms");
            }
            if (value > 600000) { // 10 minutes max
                throw new IllegalArgumentException(name + " cannot exceed 600000ms (10 minutes), got: " + value + "ms");
            }
        }

        private void validatePositive(String name, int value) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be positive, got: " + value);
            }
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public int getRetryDelayMs() {
            return retryDelayMs;
        }

        /**
         * Creates default configuration with 30s timeouts.
         */
        public static DownloadConfiguration defaultConfig() {
            return new DownloadConfiguration(30000, 30000, 3, 1000);
        }

        /**
         * Creates configuration from user properties.
         */
        public static DownloadConfiguration fromProperties(Integer connectTimeout, Integer readTimeout) {
            int connect = connectTimeout != null && connectTimeout > 0 ? connectTimeout : 30000;
            int read = readTimeout != null && readTimeout > 0 ? readTimeout : 30000;
            return new DownloadConfiguration(connect, read, 3, 1000);
        }
    }
}