package com.blackduck.integration.detectable.detectables.docker.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.blackduck.integration.detectable.detectables.docker.DockerDetectableOptions;
import com.blackduck.integration.detectable.detectables.docker.DockerProperties;
import com.blackduck.integration.log.LogLevel;

/**
 * Regression tests for the SpEL guardrail integration inside {@link DockerProperties}.
 *
 * These exist so a future refactor that removes the guard call from
 * {@link DockerProperties#populatePropertiesFile(File, File)} (while leaving
 * {@code SpelInjectionGuard} intact) fails loudly instead of silently reopening
 * the SpEL injection sink documented in CVE-2026-41849.
 */
class DockerPropertiesTest {

    @Test
    void passthroughValueContainingSpelTemplateIsRejected(@TempDir Path tempDir) throws IOException {
        Map<String, String> passthrough = new LinkedHashMap<>();
        passthrough.put("linux.distro", "#{7*191}");

        DockerProperties dockerProperties = new DockerProperties(buildOptions(passthrough, null));

        File propsFile = tempDir.resolve("application.properties").toFile();
        File outputDir = tempDir.toFile();

        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> dockerProperties.populatePropertiesFile(propsFile, outputDir)
        );

        assertTrue(
            thrown.getMessage().contains("linux.distro"),
            "Rejection message must name the offending property, got: " + thrown.getMessage()
        );
        assertTrue(
            thrown.getMessage().contains("CVE-2026-41849"),
            "Rejection message must cite the CVE, got: " + thrown.getMessage()
        );
        assertFalse(
            propsFile.exists() && propsFile.length() > 0,
            "Properties file must not have been written with a poisoned value"
        );
    }

    @Test
    void dockerPlatformTopLayerIdContainingSpelTemplateIsRejected(@TempDir Path tempDir) {
        DockerProperties dockerProperties = new DockerProperties(
            buildOptions(new HashMap<>(), "#{T(java.lang.System).getProperty('user.home')}")
        );

        File propsFile = tempDir.resolve("application.properties").toFile();
        File outputDir = tempDir.toFile();

        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> dockerProperties.populatePropertiesFile(propsFile, outputDir)
        );

        assertTrue(
            thrown.getMessage().contains("docker.platform.top.layer.id"),
            "Rejection message must name the offending property, got: " + thrown.getMessage()
        );
    }

    @Test
    void benignPassthroughIsWrittenToPropertiesFile(@TempDir Path tempDir) throws IOException {
        Map<String, String> passthrough = new LinkedHashMap<>();
        passthrough.put("linux.distro", "alpine");
        passthrough.put("some.other.flag", "true");

        DockerProperties dockerProperties = new DockerProperties(buildOptions(passthrough, "sha256:deadbeef"));

        File propsFile = tempDir.resolve("application.properties").toFile();
        File outputDir = tempDir.toFile();

        dockerProperties.populatePropertiesFile(propsFile, outputDir);

        assertTrue(propsFile.exists(), "Properties file should have been written");

        Properties written = new Properties();
        try (FileInputStream in = new FileInputStream(propsFile)) {
            written.load(in);
        }

        assertEquals("alpine", written.getProperty("linux.distro"));
        assertEquals("true", written.getProperty("some.other.flag"));
        assertEquals("sha256:deadbeef", written.getProperty("docker.platform.top.layer.id"));
        assertEquals("Detect", written.getProperty("caller.name"));
    }

    @Test
    void placeholderStylePassthroughValueIsAllowed(@TempDir Path tempDir) throws IOException {
        // "${...}" placeholders are not SpEL templates and must not be rejected.
        Map<String, String> passthrough = new LinkedHashMap<>();
        passthrough.put("some.key", "${env.HOME}");

        DockerProperties dockerProperties = new DockerProperties(buildOptions(passthrough, null));

        File propsFile = tempDir.resolve("application.properties").toFile();
        File outputDir = tempDir.toFile();

        dockerProperties.populatePropertiesFile(propsFile, outputDir);

        Properties written = new Properties();
        try (FileInputStream in = new FileInputStream(propsFile)) {
            written.load(in);
        }
        assertEquals("${env.HOME}", written.getProperty("some.key"));
    }

    private DockerDetectableOptions buildOptions(Map<String, String> additionalDockerProperties, String dockerPlatformTopLayerId) {
        return new DockerDetectableOptions(
            null,                       // suppliedDockerImage
            null,                       // suppliedDockerImageId
            null,                       // suppliedDockerTar
            LogLevel.INFO,              // dockerInspectorLoggingLevel
            null,                       // dockerInspectorVersion
            additionalDockerProperties,
            null,                       // dockerInspectorPath
            dockerPlatformTopLayerId
        );
    }
}

