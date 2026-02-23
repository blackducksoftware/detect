package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

/**
 * Configuration settings for artifact downloads.
 */
public class DownloadConfiguration {
    private final int maxRetries;
    private final int retryDelayMs;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public DownloadConfiguration(int maxRetries, int retryDelayMs, int connectTimeoutMs, int readTimeoutMs) {
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public static DownloadConfiguration createDefault() {
        return new DownloadConfiguration(3, 1000, 30000, 60000);
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getRetryDelayMs() {
        return retryDelayMs;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }
}

