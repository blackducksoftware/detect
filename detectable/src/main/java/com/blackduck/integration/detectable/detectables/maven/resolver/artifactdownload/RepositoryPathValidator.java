package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates Maven repository paths for security and accessibility.
 * Single Responsibility: Path validation and normalization.
 */
public class RepositoryPathValidator {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryPathValidator.class);

    /**
     * Validates a custom repository path at startup.
     * Performs security checks and ensures path is accessible.
     *
     * @param customPath The path to validate (can be null)
     * @return The normalized, validated path or null if no custom path provided
     * @throws IllegalStateException if path is invalid with actionable error message
     */
    public Path validateCustomRepositoryPath(Path customPath) {
        if (customPath == null) {
            logger.debug("No custom repository path configured");
            return null;
        }

        logger.info("Validating custom repository path configuration...");

        try {
            // Normalize path to resolve any . and .. components
            Path normalizedPath = customPath.normalize().toAbsolutePath();

            // Security check: detect directory traversal attempts
            // Check if the original path contains traversal sequences before normalization
            String originalPath = customPath.toString();
            if (originalPath.contains("..") || originalPath.contains("./")) {
                // Additional check: ensure the normalized path doesn't escape expected boundaries
                // This catches attempts like "../../../etc/passwd"
                Path currentDir = Path.of("").toAbsolutePath();
                Path parentDir = currentDir.getParent();

                if (parentDir != null && !normalizedPath.startsWith(currentDir) &&
                    !normalizedPath.startsWith(parentDir)) {
                    throw new IllegalStateException(
                        "Invalid custom repository path: Path contains directory traversal sequences that " +
                        "escape the expected working directory boundaries. " +
                        "Please provide a path within the current directory tree."
                    );
                }

                // Log warning for any traversal attempts, even if they don't escape boundaries
                logger.warn("Custom repository path contains directory traversal sequences: {}. " +
                           "Resolved to: {}", originalPath, sanitizePath(normalizedPath));
            }

            // Check if path exists
            if (!Files.exists(normalizedPath)) {
                throw new IllegalStateException(
                    String.format("Custom repository path does not exist: %s. " +
                        "Please ensure the path exists or remove the configuration.",
                        sanitizePath(normalizedPath))
                );
            }

            // Check if path is readable
            if (!Files.isReadable(normalizedPath)) {
                throw new IllegalStateException(
                    String.format("Custom repository path is not readable: %s. " +
                        "Please check file system permissions.",
                        sanitizePath(normalizedPath))
                );
            }

            // If it's a file, that's valid (direct JAR reference)
            if (Files.isRegularFile(normalizedPath)) {
                logger.info("Custom repository path validated: Direct JAR file reference");
                return normalizedPath;
            }

            // If it's a directory, ensure it's actually a directory
            if (!Files.isDirectory(normalizedPath)) {
                throw new IllegalStateException(
                    String.format("Custom repository path is neither a file nor a directory: %s. " +
                        "Please provide a valid directory path or JAR file path.",
                        sanitizePath(normalizedPath))
                );
            }

            logger.info("Custom repository path validated successfully: Maven repository directory");
            return normalizedPath;

        } catch (SecurityException e) {
            throw new IllegalStateException(
                "Security manager denied access to custom repository path. " +
                "Please check security policy configuration.", e
            );
        }
    }

    /**
     * Validates the default Maven repository path (~/.m2/repository).
     * Ensures it exists, is writable, and has sufficient permissions.
     *
     * @param defaultPath The default repository path
     * @throws IllegalStateException if path cannot be created or is not writable
     */
    public void validateDefaultRepositoryPath(Path defaultPath) {
        if (defaultPath == null) {
            throw new IllegalStateException(
                "Default Maven repository path cannot be null. " +
                "Please ensure HOME environment variable is set."
            );
        }

        try {
            // Create directories if they don't exist
            Files.createDirectories(defaultPath);
            logger.debug("Default Maven repository initialized at: {}", sanitizePath(defaultPath));

            // Verify write permissions
            if (!Files.isWritable(defaultPath)) {
                throw new IllegalStateException(
                    String.format("Default Maven repository is not writable: %s. " +
                        "Please check file system permissions or disk space.",
                        sanitizePath(defaultPath))
                );
            }

            // Check if we can actually create a temp file (tests actual write capability)
            Path testFile = defaultPath.resolve(".detect-write-test-" + System.nanoTime());
            try {
                Files.createFile(testFile);
                Files.delete(testFile);
            } catch (IOException e) {
                throw new IllegalStateException(
                    String.format("Cannot write to default Maven repository: %s. " +
                        "Disk might be full or permissions might be restricted. Error: %s",
                        sanitizePath(defaultPath), e.getMessage()), e
                );
            }

            logger.debug("Write permissions verified for: {}", sanitizePath(defaultPath));

        } catch (IOException e) {
            throw new IllegalStateException(
                String.format("Failed to create default Maven repository directory: %s. " +
                    "Please check file system permissions and disk space. Error: %s",
                    sanitizePath(defaultPath), e.getMessage()), e
            );
        }
    }

    /**
     * Sanitizes a path for safe logging (removes sensitive information).
     * Shows only the last 2-3 components of the path.
     */
    private String sanitizePath(Path path) {
        if (path == null) {
            return "<null>";
        }

        int nameCount = path.getNameCount();
        if (nameCount <= 3) {
            return ".../" + path.getFileName();
        }

        // Show last 3 components
        Path subpath = path.subpath(nameCount - 3, nameCount);
        return ".../" + subpath.toString();
    }
}