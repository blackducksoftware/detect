package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import java.nio.file.Path;

/**
 * Represents the result of an artifact download attempt.
 *
 * <p>Three possible states:
 * <ul>
 *   <li><strong>Success:</strong> {@code isSuccess() == true} - artifact downloaded/found</li>
 *   <li><strong>Not Found:</strong> {@code isNotFound() == true} - 404, artifact doesn't exist (not an error)</li>
 *   <li><strong>Failure:</strong> {@code isSuccess() == false && isNotFound() == false} - actual error occurred</li>
 * </ul>
 */
public class DownloadResult {
    private final boolean success;
    private final boolean notFound;
    private final String source;
    private final Path downloadedPath;
    private final String errorMessage;

    private DownloadResult(boolean success, boolean notFound, String source, Path downloadedPath, String errorMessage) {
        this.success = success;
        this.notFound = notFound;
        this.source = source;
        this.downloadedPath = downloadedPath;
        this.errorMessage = errorMessage;
    }

    /**
     * Creates a successful download result.
     *
     * @param source The source where the artifact was found (e.g., "maven-central", "home-m2-repository")
     * @param downloadedPath The path to the downloaded/located artifact
     * @return A success result
     */
    public static DownloadResult success(String source, Path downloadedPath) {
        return new DownloadResult(true, false, source, downloadedPath, null);
    }

    /**
     * Creates a not-found result (404 - artifact doesn't exist in repository).
     * This is NOT an error - the artifact simply isn't available.
     *
     * @param message Description of what was not found
     * @return A not-found result
     */
    public static DownloadResult notFound(String message) {
        return new DownloadResult(false, true, null, null, message);
    }

    /**
     * Creates a failure result (actual error during download).
     *
     * @param errorMessage Description of the error
     * @return A failure result
     */
    public static DownloadResult failure(String errorMessage) {
        return new DownloadResult(false, false, null, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isNotFound() {
        return notFound;
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

