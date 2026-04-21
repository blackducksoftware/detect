package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Checks for artifact existence in local Maven repositories with validation.
 * Single Responsibility: Local artifact resolution without downloads.
 */
public class LocalRepositoryChecker {

    private static final Logger logger = LoggerFactory.getLogger(LocalRepositoryChecker.class);

    // JAR/ZIP magic bytes: PK.. (0x50 0x4B 0x03 0x04)
    private static final byte[] JAR_MAGIC_BYTES = new byte[] { 0x50, 0x4B, 0x03, 0x04 };
    private static final int MIN_JAR_SIZE_BYTES = 22; // Minimum valid ZIP file size (empty ZIP)

    /**
     * Resolves the home .m2 repository path (~/.m2/repository).
     *
     * @return Path to home .m2 repository, or null if not found
     */
    public Path resolveHomeM2Repository() {
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            Path homeM2 = Paths.get(userHome, ".m2", "repository");
            if (Files.isDirectory(homeM2)) {
                logger.debug("Found home .m2 repository: {}", homeM2);
                return homeM2;
            }
        }
        logger.debug("Home .m2 repository not found");
        return null;
    }

    /**
     * Checks if an artifact exists in a custom repository path.
     *
     * @param artifact The artifact to check
     * @param customPath The custom repository path (can be file or directory)
     * @return The path to the artifact if found and valid, null otherwise
     */
    public Path checkCustomRepository(Artifact artifact, Path customPath) {
        if (customPath == null) {
            return null;
        }

        ArtifactCoordinate coordinate = ArtifactCoordinate.fromAetherArtifact(artifact);
        logger.debug("[Custom Repository Check] Looking for artifact: {}", coordinate.toCoordinateString());

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
     * @return The path to the artifact if found and valid, null otherwise
     */
    public Path checkDefaultRepository(Artifact artifact, Path defaultPath) {
        if (defaultPath == null || !Files.isDirectory(defaultPath)) {
            return null;
        }

        ArtifactCoordinate coordinate = ArtifactCoordinate.fromAetherArtifact(artifact);
        logger.debug("[Default Repository Check] Looking for artifact: {}", coordinate.toCoordinateString());

        return checkRepositoryDirectory(coordinate, defaultPath, "default");
    }

    /**
     * Checks if an artifact exists in a repository path (generic).
     *
     * @param artifact The artifact to check
     * @param repositoryPath The repository path
     * @param repoName Human-readable name for logging
     * @return The path to the artifact if found and valid, null otherwise
     */
    public Path checkRepository(Artifact artifact, Path repositoryPath, String repoName) {
        if (repositoryPath == null || !Files.isDirectory(repositoryPath)) {
            return null;
        }

        ArtifactCoordinate coordinate = ArtifactCoordinate.fromAetherArtifact(artifact);
        logger.debug("[{} Repository Check] Looking for artifact: {}", repoName, coordinate.toCoordinateString());

        return checkRepositoryDirectory(coordinate, repositoryPath, repoName);
    }

    /**
     * Builds the expected JAR path within a Maven repository using standard layout.
     *
     * @param artifact The artifact
     * @param repositoryRoot The repository root path
     * @return Path to the expected JAR location
     */
    public Path buildJarPath(Artifact artifact, Path repositoryRoot) {
        ArtifactCoordinate coordinate = ArtifactCoordinate.fromAetherArtifact(artifact);
        return buildRepositoryJarPath(coordinate, repositoryRoot);
    }

    private Path checkDirectJarFile(ArtifactCoordinate coordinate, Path jarPath) {
        String expectedFileName = coordinate.toFileName();
        String actualFileName = jarPath.getFileName().toString();

        if (actualFileName.equals(expectedFileName)) {
            if (isValidJarFile(jarPath)) {
                long fileSize = getFileSizeKb(jarPath);
                logger.info(" FOUND direct JAR file ({}KB)", fileSize);
                logger.debug("File: {}", jarPath);
                return jarPath;
            }
            logger.warn("JAR file found but is invalid (corrupt or too small): {}", jarPath);
            return null;
        } else {
            logger.warn("JAR filename mismatch in custom repository!");
            logger.warn("  Artifact: {}", coordinate.toCoordinateString());
            logger.warn("  Expected filename: {}", expectedFileName);
            logger.warn("  Actual filename: {}", actualFileName);
            return null;
        }
    }

    private Path checkRepositoryDirectory(ArtifactCoordinate coordinate, Path repositoryPath, String repoType) {
        Path jarPath = buildRepositoryJarPath(coordinate, repositoryPath);
        String artifactId = coordinate.toCoordinateString();

        logger.debug("Checking {} repository for: {}", repoType, jarPath);

        if (Files.exists(jarPath)) {
            if (!Files.isReadable(jarPath)) {
                logger.warn("JAR exists but is not readable in {} repository: {}", repoType, jarPath);
                return null;
            }

            if (!isValidJarFile(jarPath)) {
                logger.warn("JAR exists but is invalid (corrupt or too small) in {} repository: {}", repoType, jarPath);
                return null;
            }

            long fileSize = getFileSizeKb(jarPath);
            logger.info("FOUND in {} repository ({}KB): {}", repoType, fileSize, artifactId);
            return jarPath;
        }

        logger.debug("NOT FOUND in {} repository: {}", repoType, artifactId);
        return null;
    }

    /**
     * Validates that a file is a valid JAR (checks size and magic bytes).
     *
     * @param jarPath Path to the file to validate
     * @return true if file is a valid JAR, false otherwise
     */
    public boolean isValidJarFile(Path jarPath) {
        if (jarPath == null || !Files.exists(jarPath) || !Files.isRegularFile(jarPath)) {
            return false;
        }

        try {
            long fileSize = Files.size(jarPath);
            if (fileSize < MIN_JAR_SIZE_BYTES) {
                logger.trace("File too small to be valid JAR: {} bytes", fileSize);
                return false;
            }
        } catch (IOException e) {
            logger.trace("Cannot read file size: {}", e.getMessage());
            return false;
        }

        return verifyJarMagicBytes(jarPath);
    }

    /**
     * Verifies that a file has valid JAR/ZIP magic bytes (PK..).
     */
    private boolean verifyJarMagicBytes(Path filePath) {
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            byte[] header = new byte[4];
            int bytesRead = fis.read(header);

            if (bytesRead < 4) {
                return false;
            }

            return header[0] == JAR_MAGIC_BYTES[0] &&
                   header[1] == JAR_MAGIC_BYTES[1] &&
                   header[2] == JAR_MAGIC_BYTES[2] &&
                   header[3] == JAR_MAGIC_BYTES[3];

        } catch (IOException e) {
            logger.trace("Failed to read file header: {}", e.getMessage());
            return false;
        }
    }

    private Path buildRepositoryJarPath(ArtifactCoordinate coordinate, Path repositoryPath) {
        String[] pathParts = coordinate.toRepositoryPath().split("/");

        Path result = repositoryPath;
        for (String part : pathParts) {
            result = result.resolve(part);
        }

        return result;
    }

    private long getFileSizeKb(Path path) {
        try {
            return Files.size(path) / 1024;
        } catch (IOException e) {
            return 0;
        }
    }
}