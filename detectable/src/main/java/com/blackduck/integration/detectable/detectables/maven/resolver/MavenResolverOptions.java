package com.blackduck.integration.detectable.detectables.maven.resolver;

import java.nio.file.Path;

/**
 * Configuration options for Maven resolver operations.
 * Includes settings for artifact downloads, timeouts, parallel processing, and retry logic.
 */
public class MavenResolverOptions {
    // Core features
    private final boolean downloadArtifactJarsEnabled;
    private final Path jarRepositoryPath;

    // HTTP timeout configuration
    private final Integer connectTimeoutMs;
    private final Integer readTimeoutMs;

    // Parallel download configuration
    private final Integer downloadThreads;

    // Retry configuration
    private final Integer retryCount;
    private final Long retryBackoffInitialMs;
    private final Long retryBackoffMaxMs;

    /**
     * Full constructor with all configuration options.
     */
    public MavenResolverOptions(boolean downloadArtifactJarsEnabled, Path jarRepositoryPath,
                               Integer connectTimeoutMs, Integer readTimeoutMs,
                               Integer downloadThreads, Integer retryCount,
                               Long retryBackoffInitialMs, Long retryBackoffMaxMs) {
        this.downloadArtifactJarsEnabled = downloadArtifactJarsEnabled;
        this.jarRepositoryPath = jarRepositoryPath;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.downloadThreads = downloadThreads;
        this.retryCount = retryCount;
        this.retryBackoffInitialMs = retryBackoffInitialMs;
        this.retryBackoffMaxMs = retryBackoffMaxMs;
    }

    /**
     * Legacy constructor for backward compatibility.
     */
    public MavenResolverOptions(boolean downloadArtifactJarsEnabled, Path jarRepositoryPath) {
        this(downloadArtifactJarsEnabled, jarRepositoryPath, null, null, null, null, null, null);
    }

    /**
     * Constructor with basic timeout configuration.
     */
    public MavenResolverOptions(boolean downloadArtifactJarsEnabled, Path jarRepositoryPath,
                               Integer connectTimeoutMs, Integer readTimeoutMs) {
        this(downloadArtifactJarsEnabled, jarRepositoryPath, connectTimeoutMs, readTimeoutMs,
             null, null, null, null);
    }

    // Getters for all fields
    public boolean isDownloadArtifactJarsEnabled() {
        return downloadArtifactJarsEnabled;
    }

    public Path getJarRepositoryPath() {
        return jarRepositoryPath;
    }

    public Integer getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public Integer getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public Integer getDownloadThreads() {
        return downloadThreads;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public Long getRetryBackoffInitialMs() {
        return retryBackoffInitialMs;
    }

    public Long getRetryBackoffMaxMs() {
        return retryBackoffMaxMs;
    }
}