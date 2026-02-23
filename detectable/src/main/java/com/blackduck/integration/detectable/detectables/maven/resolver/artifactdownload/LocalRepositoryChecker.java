package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Checks for artifact existence in local Maven repositories with classifier awareness.
 * Single Responsibility: Local artifact resolution without downloads.
 */
public class LocalRepositoryChecker {

    private static final Logger logger = LoggerFactory.getLogger(LocalRepositoryChecker.class);

    /**
     * Checks if an artifact exists in a custom repository path.
     * Logs explicit warnings when artifacts are not found.
     *
     * @param artifact The artifact to check
     * @param customPath The custom repository path (can be file or directory)
     * @return The path to the artifact if found, null otherwise
     */
    public Path checkCustomRepository(Artifact artifact, Path customPath) {
        if (customPath == null) {
            return null;
        }

        ArtifactCoordinate coordinate = ArtifactCoordinate.fromAetherArtifact(artifact);
        logger.info("[Custom Repository Check] Looking for artifact: {}", coordinate.toCoordinateString());

        // Check if custom path points directly to a JAR file
        if (Files.isRegularFile(customPath)) {
            return checkDirectJarFile(coordinate, customPath);
        }

        // Treat as repository directory
        if (Files.isDirectory(customPath)) {
            return checkRepositoryDirectory(coordinate, customPath, "custom");
        }

        logger.warn("Custom repository path is neither a file nor directory: {}", customPath);
        return null;
    }

    /**
     * Checks if an artifact exists in the default Maven repository.
     *
     * @param artifact The artifact to check
     * @param defaultPath The default repository path (~/.m2/repository)
     * @return The path to the artifact if found, null otherwise
     */
    public Path checkDefaultRepository(Artifact artifact, Path defaultPath) {
        if (defaultPath == null || !Files.isDirectory(defaultPath)) {
            return null;
        }

        ArtifactCoordinate coordinate = ArtifactCoordinate.fromAetherArtifact(artifact);
        logger.info("[Default Repository Check] Looking for artifact: {}", coordinate.toCoordinateString());

        return checkRepositoryDirectory(coordinate, defaultPath, "default");
    }

    private Path checkDirectJarFile(ArtifactCoordinate coordinate, Path jarPath) {
        String expectedFileName = coordinate.toFileName();
        String actualFileName = jarPath.getFileName().toString();

        if (actualFileName.equals(expectedFileName)) {
            long fileSize = getFileSize(jarPath);
            logger.info("✓ FOUND direct JAR file ({}KB)", fileSize);
            logger.debug("File: {}", jarPath);
            return jarPath;
        } else {
            // EXPLICIT WARNING for filename mismatch (Fix #2)
            logger.warn("JAR filename mismatch in custom repository!");
            logger.warn("  Artifact: {}", coordinate.toCoordinateString());
            logger.warn("  Expected filename: {}", expectedFileName);
            logger.warn("  Actual filename: {}", actualFileName);
            logger.warn("  Custom path checked: {}", jarPath);
            logger.warn("  → Proceeding to check other locations...");
            return null;
        }
    }

    private Path checkRepositoryDirectory(ArtifactCoordinate coordinate, Path repositoryPath, String repoType) {
        Path jarPath = buildRepositoryJarPath(coordinate, repositoryPath);
        String artifactId = coordinate.toCoordinateString();

        logger.debug("Checking {} repository for: {}", repoType, jarPath);

        if (Files.exists(jarPath)) {
            if (!Files.isReadable(jarPath)) {
                logger.warn("JAR exists but is not readable in {} repository!", repoType);
                logger.warn("  Artifact: {}", artifactId);
                logger.warn("  Path: {}", jarPath);
                logger.warn("  → Check file permissions");
                return null;
            }

            long fileSize = getFileSize(jarPath);
            if (fileSize == 0) {
                logger.warn("JAR exists but is empty (0 bytes) in {} repository!", repoType);
                logger.warn("  Artifact: {}", artifactId);
                logger.warn("  Path: {}", jarPath);
                logger.warn("  → File may be corrupted");
                return null;
            }

            logger.info("✓ FOUND in {} repository ({}KB)", repoType, fileSize);
            logger.debug("File path: {}", jarPath);
            return jarPath;
        }

        // EXPLICIT WARNING when not found in custom repository (Fix #2)
        if ("custom".equals(repoType)) {
            logger.warn("JAR not found in custom repository!");
            logger.warn("  Artifact: {}", artifactId);
            logger.warn("  Expected path: {}", jarPath);
            logger.warn("  Custom repository: {}", repositoryPath);
            logger.warn("  → Will check default repository next...");
        } else {
            logger.info("✗ NOT FOUND in {} repository", repoType);
        }

        return null;
    }

    private Path buildRepositoryJarPath(ArtifactCoordinate coordinate, Path repositoryPath) {
        String[] pathParts = coordinate.toRepositoryPath().split("/");

        Path result = repositoryPath;
        for (String part : pathParts) {
            result = result.resolve(part);
        }

        return result;
    }

    private long getFileSize(Path path) {
        try {
            return Files.size(path) / 1024; // KB
        } catch (IOException e) {
            logger.debug("Could not determine file size for: {}", path, e);
            return 0;
        }
    }
}