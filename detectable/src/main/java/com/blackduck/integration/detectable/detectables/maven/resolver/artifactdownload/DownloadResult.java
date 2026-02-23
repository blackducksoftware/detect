package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import java.nio.file.Path;

/**
 * Represents the result of an artifact download attempt.
 */
public class DownloadResult {
    private final boolean success;
    private final String source;
    private final Path downloadedPath;
    private final String errorMessage;

    private DownloadResult(boolean success, String source, Path downloadedPath, String errorMessage) {
        this.success = success;
        this.source = source;
        this.downloadedPath = downloadedPath;
        this.errorMessage = errorMessage;
    }

    public static DownloadResult success(String source, Path downloadedPath) {
        return new DownloadResult(true, source, downloadedPath, null);
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

    public Path getDownloadedPath() {
        return downloadedPath;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}

