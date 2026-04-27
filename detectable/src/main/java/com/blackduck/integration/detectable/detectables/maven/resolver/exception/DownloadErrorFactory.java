package com.blackduck.integration.detectable.detectables.maven.resolver.exception;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.ConnectException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

/**
 * Factory for creating categorized download exceptions.
 * Single Responsibility: Centralize exception creation and categorization logic.
 */
public class DownloadErrorFactory {

    /**
     * Creates an exception for network-related failures.
     */
    public static ArtifactDownloadException networkError(String artifactId, String message, Throwable cause) {
        return new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.NETWORK_ERROR,
            artifactId,
            message,
            cause
        );
    }

    /**
     * Creates an exception for repository-related failures.
     */
    public static ArtifactDownloadException repositoryError(String artifactId, String message, Throwable cause) {
        return new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.REPOSITORY_ERROR,
            artifactId,
            message,
            cause
        );
    }

    /**
     * Creates an exception for file system failures.
     */
    public static ArtifactDownloadException fileSystemError(String artifactId, String message, Throwable cause) {
        return new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.FILE_SYSTEM_ERROR,
            artifactId,
            message,
            cause
        );
    }

    /**
     * Creates an exception for configuration errors.
     */
    public static ArtifactDownloadException configurationError(String artifactId, String message) {
        return new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.CONFIGURATION_ERROR,
            artifactId,
            message
        );
    }

    /**
     * Creates an exception for partial download failures.
     */
    public static ArtifactDownloadException partialDownloadError(String artifactId, String message, Throwable cause) {
        return new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.PARTIAL_DOWNLOAD_ERROR,
            artifactId,
            message,
            cause
        );
    }

    /**
     * Creates an exception when server doesn't support range requests.
     */
    public static ArtifactDownloadException rangeNotSupportedError(String artifactId, String message) {
        return new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.RANGE_NOT_SUPPORTED_ERROR,
            artifactId,
            message
        );
    }

    /**
     * Creates an exception for insufficient disk space.
     */
    public static ArtifactDownloadException insufficientDiskSpaceError(String artifactId,
                                                                       long available,
                                                                       long required) {
        String message = String.format(
            "Insufficient disk space: %.1f MB available, %.1f MB required. " +
            "Free up disk space or configure a different download location.",
            available / (1024.0 * 1024),
            required / (1024.0 * 1024)
        );
        return new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.INSUFFICIENT_DISK_SPACE_ERROR,
            artifactId,
            message
        );
    }

    /**
     * Creates an exception for unknown errors.
     */
    public static ArtifactDownloadException unknownError(String artifactId, String message, Throwable cause) {
        return new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.UNKNOWN_ERROR,
            artifactId,
            message,
            cause
        );
    }

    /**
     * Categorizes an IOException and creates appropriate exception.
     */
    public static ArtifactDownloadException fromIOException(String artifactId, IOException e) {
        // Socket timeout
        if (e instanceof SocketTimeoutException) {
            return networkError(artifactId,
                "Connection timed out. Consider increasing timeout values or checking network latency.",
                e);
        }

        // DNS resolution failure
        if (e instanceof UnknownHostException) {
            return networkError(artifactId,
                "Cannot resolve Maven Central hostname. Check DNS configuration and internet connectivity.",
                e);
        }

        // Connection refused
        if (e instanceof ConnectException) {
            String message = e.getMessage();
            if (message != null && message.contains("refused")) {
                return networkError(artifactId,
                    "Connection refused by server. Check network connectivity and firewall settings.",
                    e);
            }
        }

        // File access denied
        if (e instanceof AccessDeniedException) {
            return fileSystemError(artifactId,
                "Access denied to file or directory. Check file system permissions.",
                e);
        }

        // File not found
        if (e instanceof NoSuchFileException) {
            return fileSystemError(artifactId,
                "File or directory not found. Check path configuration.",
                e);
        }

        // Check message for common patterns
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();

            // Disk space issues
            if (lowerMessage.contains("no space left") || lowerMessage.contains("disk full")) {
                return fileSystemError(artifactId,
                    "Insufficient disk space to save artifact. Free up disk space and retry.",
                    e);
            }

            // Permission issues
            if (lowerMessage.contains("permission denied") || lowerMessage.contains("access denied")) {
                return fileSystemError(artifactId,
                    "Permission denied. Check file system permissions for the download directory.",
                    e);
            }

            // Network issues
            if (lowerMessage.contains("connection reset") || lowerMessage.contains("broken pipe")) {
                return networkError(artifactId,
                    "Connection was reset. Check network stability and retry.",
                    e);
            }

            // SSL/TLS issues
            if (lowerMessage.contains("ssl") || lowerMessage.contains("tls") ||
                lowerMessage.contains("certificate")) {
                return networkError(artifactId,
                    "SSL/TLS error. Check certificate configuration and proxy settings.",
                    e);
            }
        }

        // Default to network error for unclassified IOException
        return networkError(artifactId,
            "IO error during download. Check network stability and disk permissions.",
            e);
    }

    /**
     * Creates exception from HTTP response code.
     */
    public static ArtifactDownloadException fromHttpError(String artifactId, int responseCode) {
        switch (responseCode) {
            case 401:
                return repositoryError(artifactId,
                    "Authentication required (401). Repository requires credentials.",
                    null);

            case 403:
                return repositoryError(artifactId,
                    "Access forbidden (403). Check repository permissions.",
                    null);

            case 404:
                return repositoryError(artifactId,
                    "Artifact not found in repository (404).",
                    null);

            case 429:
                return repositoryError(artifactId,
                    "Too many requests (429). Rate limited by repository.",
                    null);

            case 500:
            case 502:
            case 503:
            case 504:
                return repositoryError(artifactId,
                    String.format("Repository server error (%d). Try again later.", responseCode),
                    null);

            default:
                if (responseCode >= 400 && responseCode < 500) {
                    return repositoryError(artifactId,
                        String.format("Client error (%d). Check request configuration.", responseCode),
                        null);
                } else if (responseCode >= 500) {
                    return repositoryError(artifactId,
                        String.format("Server error (%d). Repository may be temporarily unavailable.", responseCode),
                        null);
                } else {
                    return unknownError(artifactId,
                        String.format("Unexpected HTTP response code: %d", responseCode),
                        null);
                }
        }
    }
}