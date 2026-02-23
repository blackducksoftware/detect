package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DownloadConfiguration - Fix #3: Configurable timeouts.
 */
class DownloadConfigurationTest {

    @Test
    void constructor_ValidValues_CreatesConfiguration() {
        HttpArtifactDownloader.DownloadConfiguration config =
            new HttpArtifactDownloader.DownloadConfiguration(5000, 10000, 3, 1000);

        assertEquals(5000, config.getConnectTimeoutMs());
        assertEquals(10000, config.getReadTimeoutMs());
        assertEquals(3, config.getMaxRetries());
        assertEquals(1000, config.getRetryDelayMs());
    }

    @Test
    void constructor_NegativeConnectTimeout_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new HttpArtifactDownloader.DownloadConfiguration(-1, 10000, 3, 1000)
        );

        assertTrue(exception.getMessage().contains("connect timeout must be positive"));
    }

    @Test
    void constructor_ZeroReadTimeout_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new HttpArtifactDownloader.DownloadConfiguration(5000, 0, 3, 1000)
        );

        assertTrue(exception.getMessage().contains("read timeout must be positive"));
    }

    @Test
    void constructor_ExcessiveTimeout_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new HttpArtifactDownloader.DownloadConfiguration(700000, 10000, 3, 1000)
        );

        assertTrue(exception.getMessage().contains("cannot exceed 600000ms"));
    }

    @Test
    void constructor_NegativeMaxRetries_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new HttpArtifactDownloader.DownloadConfiguration(5000, 10000, -1, 1000)
        );

        assertTrue(exception.getMessage().contains("max retries must be positive"));
    }

    @Test
    void constructor_ZeroRetryDelay_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new HttpArtifactDownloader.DownloadConfiguration(5000, 10000, 3, 0)
        );

        assertTrue(exception.getMessage().contains("retry delay must be positive"));
    }

    @Test
    void defaultConfig_ReturnsExpectedValues() {
        HttpArtifactDownloader.DownloadConfiguration config =
            HttpArtifactDownloader.DownloadConfiguration.defaultConfig();

        assertEquals(30000, config.getConnectTimeoutMs());
        assertEquals(30000, config.getReadTimeoutMs());
        assertEquals(3, config.getMaxRetries());
        assertEquals(1000, config.getRetryDelayMs());
    }

    @Test
    void fromProperties_NullValues_UsesDefaults() {
        HttpArtifactDownloader.DownloadConfiguration config =
            HttpArtifactDownloader.DownloadConfiguration.fromProperties(null, null);

        assertEquals(30000, config.getConnectTimeoutMs());
        assertEquals(30000, config.getReadTimeoutMs());
    }

    @Test
    void fromProperties_ValidValues_UsesProvidedValues() {
        HttpArtifactDownloader.DownloadConfiguration config =
            HttpArtifactDownloader.DownloadConfiguration.fromProperties(45000, 60000);

        assertEquals(45000, config.getConnectTimeoutMs());
        assertEquals(60000, config.getReadTimeoutMs());
    }

    @Test
    void fromProperties_NegativeValues_UsesDefaults() {
        HttpArtifactDownloader.DownloadConfiguration config =
            HttpArtifactDownloader.DownloadConfiguration.fromProperties(-5000, -10000);

        assertEquals(30000, config.getConnectTimeoutMs());
        assertEquals(30000, config.getReadTimeoutMs());
    }

    @Test
    void fromProperties_MixedValues_UsesAppropriately() {
        HttpArtifactDownloader.DownloadConfiguration config =
            HttpArtifactDownloader.DownloadConfiguration.fromProperties(15000, null);

        assertEquals(15000, config.getConnectTimeoutMs());
        assertEquals(30000, config.getReadTimeoutMs());
    }
}