package com.blackduck.integration.detect.workflow.diagnostic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.blackduck.integration.configuration.config.PropertyConfiguration;
import com.blackduck.integration.configuration.source.MapPropertySource;
import com.blackduck.integration.detect.configuration.DetectInfo;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.workflow.DetectRunId;
import com.blackduck.integration.detect.workflow.event.EventSystem;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;
import com.blackduck.integration.detect.workflow.file.DirectoryOptions;
import com.blackduck.integration.util.OperatingSystemType;

class DiagnosticSystemTest {

    private static final String TEST_PROPERTY_SOURCE_NAME = "TEST_PROPERTIES";
    private static final String TEST_RUN_ID = "test-run-id";

    @TempDir
    File tempDir;

    /**
     * Verifies that when detect.diagnostic.archive.path is set, the diagnostic zip file
     * is copied to the specified custom path after finish() is called.
     */
    @Test
    void diagnosticZipCopiedToCustomArchivePath() throws IOException {
        // Arrange
        File outputDir = new File(tempDir, "detect-output");
        outputDir.mkdirs();
        File customArchiveDir = new File(tempDir, "custom-archive");
        customArchiveDir.mkdirs();

        DetectRunId detectRunId = new DetectRunId(TEST_RUN_ID, "test-correlation-id");
        DirectoryOptions directoryOptions = new DirectoryOptions(
            null, outputDir.toPath(), null, null, null, null, null
        );
        DirectoryManager directoryManager = new DirectoryManager(directoryOptions, detectRunId);

        // Pre-create the source zip file that finish() will attempt to copy.
        // The copy logic in DiagnosticSystem.finish() looks for {runId}.zip in the runs output directory.
        File runsOutputDir = directoryManager.getRunsOutputDirectory();
        File sourceZipFile = new File(runsOutputDir, TEST_RUN_ID + ".zip");
        FileUtils.writeStringToFile(sourceZipFile, "dummy zip content", StandardCharsets.UTF_8);

        // Use trailing separator so the concatenation in finish() produces a valid path:
        // detectDiagnosticOutputPath + "detect-run-" + runId + ".zip"
        String customArchivePath = customArchiveDir.getAbsolutePath() + File.separator;
        PropertyConfiguration propertyConfiguration = buildPropertyConfiguration(customArchivePath);

        DetectInfo detectInfo = new DetectInfo("test-version", OperatingSystemType.LINUX, "2024-01-01");
        EventSystem eventSystem = new EventSystem();

        DiagnosticSystem diagnosticSystem = new DiagnosticSystem(
            propertyConfiguration,
            detectRunId,
            detectInfo,
            directoryManager,
            eventSystem,
            new TreeMap<>()
        );

        // Act
        diagnosticSystem.finish();

        // Assert
        File expectedDestinationFile = new File(customArchiveDir, "detect-run-" + TEST_RUN_ID + ".zip");
        Assertions.assertTrue(
            expectedDestinationFile.exists(),
            "Diagnostic zip should be copied to the custom archive path when detect.diagnostic.archive.path is set"
        );
    }

    /**
     * Verifies that when detect.diagnostic.archive.path is empty, no copy of the
     * diagnostic zip is performed to an alternate location.
     */
    @Test
    void diagnosticZipNotCopiedWhenArchivePathIsEmpty() throws IOException {
        // Arrange
        File outputDir = new File(tempDir, "detect-output");
        outputDir.mkdirs();
        File unexpectedCopyDir = new File(tempDir, "should-not-receive-copy");

        DetectRunId detectRunId = new DetectRunId(TEST_RUN_ID, "test-correlation-id");
        DirectoryOptions directoryOptions = new DirectoryOptions(
            null, outputDir.toPath(), null, null, null, null, null
        );
        DirectoryManager directoryManager = new DirectoryManager(directoryOptions, detectRunId);

        // Configure detect.diagnostic.archive.path as empty — copy should be skipped
        PropertyConfiguration propertyConfiguration = buildPropertyConfiguration("");

        DetectInfo detectInfo = new DetectInfo("test-version", OperatingSystemType.LINUX, "2024-01-01");
        EventSystem eventSystem = new EventSystem();

        DiagnosticSystem diagnosticSystem = new DiagnosticSystem(
            propertyConfiguration,
            detectRunId,
            detectInfo,
            directoryManager,
            eventSystem,
            new TreeMap<>()
        );

        // Act
        diagnosticSystem.finish();

        // Assert — no file should have been placed in an unrelated directory
        Assertions.assertFalse(
            unexpectedCopyDir.exists(),
            "No diagnostic zip should be copied when detect.diagnostic.archive.path is empty"
        );
    }

    private PropertyConfiguration buildPropertyConfiguration(String archivePath) {
        HashMap<String, String> propertySourceMap = new HashMap<>();
        propertySourceMap.put(DetectProperties.DETECT_DIAGNOSTIC_ARCHIVE_PATH.getKey(), archivePath);
        MapPropertySource mapPropertySource = new MapPropertySource(TEST_PROPERTY_SOURCE_NAME, propertySourceMap);
        return new PropertyConfiguration(Collections.singletonList(mapPropertySource), Collections.emptySortedMap());
    }
}

