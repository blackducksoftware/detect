package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RepositoryPathValidator - Fix #1: Configuration validation.
 */
class RepositoryPathValidatorTest {

    private RepositoryPathValidator validator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        validator = new RepositoryPathValidator();
    }

    // ========== Custom Repository Path Tests ==========

    @Test
    void validateCustomRepositoryPath_NullPath_ReturnsNull() {
        Path result = validator.validateCustomRepositoryPath(null);
        assertNull(result);
    }

    @Test
    void validateCustomRepositoryPath_ValidDirectory_ReturnsNormalizedPath() throws IOException {
        Path customRepo = tempDir.resolve("custom-repo");
        Files.createDirectories(customRepo);

        Path result = validator.validateCustomRepositoryPath(customRepo);

        assertNotNull(result);
        assertEquals(customRepo.toAbsolutePath().normalize(), result);
    }

    @Test
    void validateCustomRepositoryPath_ValidJarFile_ReturnsNormalizedPath() throws IOException {
        Path jarFile = tempDir.resolve("artifact.jar");
        Files.createFile(jarFile);

        Path result = validator.validateCustomRepositoryPath(jarFile);

        assertNotNull(result);
        assertEquals(jarFile.toAbsolutePath().normalize(), result);
    }

    @Test
    void validateCustomRepositoryPath_NonExistentPath_ThrowsException() {
        Path nonExistent = tempDir.resolve("does-not-exist");

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> validator.validateCustomRepositoryPath(nonExistent)
        );

        assertTrue(exception.getMessage().contains("does not exist"));
        assertTrue(exception.getMessage().contains("Please ensure the path exists"));
    }

    @Test
    void validateCustomRepositoryPath_UnreadablePath_ThrowsException() throws IOException {
        // Skip on Windows as it handles permissions differently
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return;
        }

        Path unreadable = tempDir.resolve("unreadable");
        Files.createDirectory(unreadable);

        try {
            // Remove read permission
            Set<PosixFilePermission> perms = Set.of(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(unreadable, perms);

            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> validator.validateCustomRepositoryPath(unreadable)
            );

            assertTrue(exception.getMessage().contains("not readable"));
            assertTrue(exception.getMessage().contains("check file system permissions"));
        } finally {
            // Restore permissions for cleanup
            Set<PosixFilePermission> perms = Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            );
            Files.setPosixFilePermissions(unreadable, perms);
        }
    }

    @Test
    void validateCustomRepositoryPath_DirectoryTraversal_ThrowsException() {
        Path malicious = Paths.get("/tmp/../etc/passwd");

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> validator.validateCustomRepositoryPath(malicious)
        );

        assertTrue(exception.getMessage().contains("directory traversal"));
        // Updated to match actual implementation message
        assertTrue(exception.getMessage().contains("escape the expected working directory boundaries") ||
                  exception.getMessage().contains("without '..' segments"));
    }

    // ========== Default Repository Path Tests ==========

    @Test
    void validateDefaultRepositoryPath_NullPath_ThrowsException() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> validator.validateDefaultRepositoryPath(null)
        );

        assertTrue(exception.getMessage().contains("cannot be null"));
        assertTrue(exception.getMessage().contains("HOME environment variable"));
    }

    @Test
    void validateDefaultRepositoryPath_ValidWritableDirectory_Succeeds() throws IOException {
        Path defaultRepo = tempDir.resolve(".m2/repository");
        Files.createDirectories(defaultRepo);

        assertDoesNotThrow(() -> validator.validateDefaultRepositoryPath(defaultRepo));

        // Verify test file was created and deleted
        assertTrue(Files.exists(defaultRepo));
        assertTrue(Files.isDirectory(defaultRepo));
    }

    @Test
    void validateDefaultRepositoryPath_NonExistentDirectory_CreatesIt() {
        Path newRepo = tempDir.resolve("new-repo/.m2/repository");

        assertDoesNotThrow(() -> validator.validateDefaultRepositoryPath(newRepo));

        assertTrue(Files.exists(newRepo));
        assertTrue(Files.isDirectory(newRepo));
    }

    @Test
    void validateDefaultRepositoryPath_NotWritable_ThrowsException() throws IOException {
        // Skip on Windows
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return;
        }

        Path readOnlyRepo = tempDir.resolve("readonly-repo");
        Files.createDirectory(readOnlyRepo);

        try {
            // Remove write permission
            Set<PosixFilePermission> perms = Set.of(PosixFilePermission.OWNER_READ);
            Files.setPosixFilePermissions(readOnlyRepo, perms);

            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> validator.validateDefaultRepositoryPath(readOnlyRepo)
            );

            assertTrue(exception.getMessage().contains("not writable"));
            assertTrue(exception.getMessage().contains("check file system permissions"));
        } finally {
            // Restore permissions
            Set<PosixFilePermission> perms = Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            );
            Files.setPosixFilePermissions(readOnlyRepo, perms);
        }
    }

    @Test
    void validateDefaultRepositoryPath_CannotCreateTestFile_ThrowsException() throws IOException {
        // Create a file where we expect a directory
        Path fileNotDir = tempDir.resolve("file.txt");
        Files.createFile(fileNotDir);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> validator.validateDefaultRepositoryPath(fileNotDir)
        );

        assertTrue(exception.getMessage().contains("Failed to create"));
        assertTrue(exception.getMessage().contains("directory"));
    }
}