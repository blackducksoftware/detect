package com.blackduck.integration.detectable.detectables.maven.resolver.exception;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ArtifactDownloadException - Fix #5: Error categorization.
 */
class ArtifactDownloadExceptionTest {

    @Test
    void constructor_WithAllParameters_BuildsCorrectMessage() {
        IOException cause = new IOException("Network error");
        ArtifactDownloadException exception = new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.NETWORK_ERROR,
            "com.example:lib:1.0",
            "Check your internet connection",
            cause
        );

        String message = exception.getMessage();
        assertTrue(message.contains("[NETWORK_ERROR]"));
        assertTrue(message.contains("Network communication failed"));
        assertTrue(message.contains("com.example:lib:1.0"));
        assertTrue(message.contains("Check your internet connection"));
        assertEquals(cause, exception.getCause());
    }

    @Test
    void constructor_WithoutCause_BuildsCorrectMessage() {
        ArtifactDownloadException exception = new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.FILE_SYSTEM_ERROR,
            "org.test:artifact:2.0",
            "Disk is full"
        );

        String message = exception.getMessage();
        assertTrue(message.contains("[FILE_SYSTEM_ERROR]"));
        assertTrue(message.contains("Local file system operation failed"));
        assertTrue(message.contains("org.test:artifact:2.0"));
        assertTrue(message.contains("Disk is full"));
        assertNull(exception.getCause());
    }

    @Test
    void constructor_NullArtifactCoordinates_HandlesGracefully() {
        ArtifactDownloadException exception = new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.CONFIGURATION_ERROR,
            null,
            "Invalid configuration"
        );

        String message = exception.getMessage();
        assertTrue(message.contains("[CONFIGURATION_ERROR]"));
        assertFalse(message.contains("artifact:"));
        assertTrue(message.contains("Invalid configuration"));
    }

    @Test
    void constructor_NullActionableMessage_HandlesGracefully() {
        ArtifactDownloadException exception = new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.REPOSITORY_ERROR,
            "com.test:lib:1.0",
            null
        );

        String message = exception.getMessage();
        assertTrue(message.contains("[REPOSITORY_ERROR]"));
        assertTrue(message.contains("com.test:lib:1.0"));
        // Should not have trailing period
        assertFalse(message.endsWith(". "));
    }

    @Test
    void getCategory_ReturnsCorrectCategory() {
        ArtifactDownloadException exception = new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.UNKNOWN_ERROR,
            "artifact",
            "message"
        );

        assertEquals(ArtifactDownloadException.ErrorCategory.UNKNOWN_ERROR, exception.getCategory());
    }

    @Test
    void getArtifactCoordinates_ReturnsCorrectValue() {
        String coords = "com.example:lib:1.0";
        ArtifactDownloadException exception = new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.NETWORK_ERROR,
            coords,
            "message"
        );

        assertEquals(coords, exception.getArtifactCoordinates());
    }

    @Test
    void getActionableMessage_ReturnsCorrectValue() {
        String actionable = "Please check network settings";
        ArtifactDownloadException exception = new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.NETWORK_ERROR,
            "artifact",
            actionable
        );

        assertEquals(actionable, exception.getActionableMessage());
    }

    @Test
    void getSanitizedMessage_RemovesSensitiveInfo() {
        ArtifactDownloadException exception = new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.FILE_SYSTEM_ERROR,
            "com.example:lib:1.0",
            "Cannot write to /home/user/secret/path"
        );

        String sanitized = exception.getSanitizedMessage();
        assertTrue(sanitized.contains("[FILE_SYSTEM_ERROR]"));
        assertTrue(sanitized.contains("com.example:lib:1.0"));
        // Should NOT contain the actionable message with paths
        assertFalse(sanitized.contains("/home/user/secret/path"));
        assertFalse(sanitized.contains("Cannot write"));
    }

    @Test
    void getSanitizedMessage_HandlesNullValues() {
        ArtifactDownloadException exception = new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.UNKNOWN_ERROR,
            null,
            null
        );

        String sanitized = exception.getSanitizedMessage();
        assertTrue(sanitized.contains("[UNKNOWN_ERROR]"));
        assertFalse(sanitized.contains("null"));
    }

    @Test
    void errorCategory_HasCorrectDescriptions() {
        assertEquals("Network communication failed",
            ArtifactDownloadException.ErrorCategory.NETWORK_ERROR.getDescription());
        assertEquals("Repository access or artifact availability issue",
            ArtifactDownloadException.ErrorCategory.REPOSITORY_ERROR.getDescription());
        assertEquals("Local file system operation failed",
            ArtifactDownloadException.ErrorCategory.FILE_SYSTEM_ERROR.getDescription());
        assertEquals("Invalid configuration detected",
            ArtifactDownloadException.ErrorCategory.CONFIGURATION_ERROR.getDescription());
        assertEquals("Unexpected error occurred",
            ArtifactDownloadException.ErrorCategory.UNKNOWN_ERROR.getDescription());
    }

    @Test
    void exception_ChainedProperly() {
        IOException rootCause = new IOException("Root problem");
        ArtifactDownloadException level1 = new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.NETWORK_ERROR,
            "artifact1",
            "Network issue",
            rootCause
        );
        ArtifactDownloadException level2 = new ArtifactDownloadException(
            ArtifactDownloadException.ErrorCategory.UNKNOWN_ERROR,
            "artifact2",
            "Wrapped error",
            level1
        );

        assertEquals(level1, level2.getCause());
        assertEquals(rootCause, level2.getCause().getCause());
    }
}