package com.blackduck.integration.detectable.detectables.maven.resolver.exception;

/**
 * Custom exception for artifact download failures with categorized error types.
 * Provides structured error information for better diagnostics and error handling.
 */
public class ArtifactDownloadException extends Exception {

    /**
     * Error categories for different types of download failures.
     */
    public enum ErrorCategory {
        NETWORK_ERROR("Network communication failed"),
        REPOSITORY_ERROR("Repository access or artifact availability issue"),
        FILE_SYSTEM_ERROR("Local file system operation failed"),
        CONFIGURATION_ERROR("Invalid configuration detected"),
        PARTIAL_DOWNLOAD_ERROR("Partial download or resume operation failed"),
        RANGE_NOT_SUPPORTED_ERROR("Server does not support resume/range requests"),
        INSUFFICIENT_DISK_SPACE_ERROR("Insufficient disk space for download"),
        UNKNOWN_ERROR("Unexpected error occurred");

        private final String description;

        ErrorCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final ErrorCategory category;
    private final String artifactCoordinates;
    private final String actionableMessage;

    /**
     * Constructs an ArtifactDownloadException with full details.
     *
     * @param category The error category
     * @param artifactCoordinates The artifact being downloaded (can be null)
     * @param actionableMessage User-friendly message with suggested actions
     * @param cause The underlying exception (can be null)
     */
    public ArtifactDownloadException(
            ErrorCategory category,
            String artifactCoordinates,
            String actionableMessage,
            Throwable cause) {
        super(buildMessage(category, artifactCoordinates, actionableMessage), cause);
        this.category = category;
        this.artifactCoordinates = artifactCoordinates;
        this.actionableMessage = actionableMessage;
    }

    /**
     * Constructs an ArtifactDownloadException without a cause.
     */
    public ArtifactDownloadException(
            ErrorCategory category,
            String artifactCoordinates,
            String actionableMessage) {
        this(category, artifactCoordinates, actionableMessage, null);
    }

    private static String buildMessage(
            ErrorCategory category,
            String artifactCoordinates,
            String actionableMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(category.name()).append("] ");
        sb.append(category.getDescription());

        if (artifactCoordinates != null && !artifactCoordinates.isEmpty()) {
            sb.append(" for artifact: ").append(artifactCoordinates);
        }

        if (actionableMessage != null && !actionableMessage.isEmpty()) {
            sb.append(". ").append(actionableMessage);
        }

        return sb.toString();
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public String getArtifactCoordinates() {
        return artifactCoordinates;
    }

    public String getActionableMessage() {
        return actionableMessage;
    }

    /**
     * Creates a sanitized version of this exception for logging.
     * Removes sensitive information like full file paths.
     */
    public String getSanitizedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(category.name()).append("] ");
        sb.append(category.getDescription());

        if (artifactCoordinates != null && !artifactCoordinates.isEmpty()) {
            sb.append(" for artifact: ").append(artifactCoordinates);
        }

        // Don't include actionable message in sanitized version as it might contain paths
        return sb.toString();
    }
}