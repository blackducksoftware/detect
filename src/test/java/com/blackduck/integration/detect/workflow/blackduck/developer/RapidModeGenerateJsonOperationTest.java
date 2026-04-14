package com.blackduck.integration.detect.workflow.blackduck.developer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.zeroturnaround.zip.commons.FileUtils;

import com.google.gson.Gson;
import com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.workflow.DetectRunId;
import com.blackduck.integration.detect.workflow.blackduck.developer.RapidModeGenerateJsonOperation;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;
import com.blackduck.integration.detect.workflow.file.DirectoryOptions;
import com.blackduck.integration.util.NameVersion;

public class RapidModeGenerateJsonOperationTest {

    @Test
    void test(@TempDir Path tempPath) throws IOException, DetectUserFriendlyException {
        Gson gson = Mockito.mock(Gson.class);

        File tempDir = tempPath.toFile();
        File scanDir = new File(tempDir, "scan");
        DirectoryOptions directoryOptions = new DirectoryOptions(null, null, null, scanDir.toPath(), null, null, null);
        DetectRunId detectRunId = new DetectRunId("testId", DetectRunId.correlationId());
        DirectoryManager directoryManager = new DirectoryManager(directoryOptions, detectRunId);
        RapidModeGenerateJsonOperation op = new RapidModeGenerateJsonOperation(gson, directoryManager);
        NameVersion projectNameVersion = new NameVersion("testName", "testVersion");

        List<DeveloperScansScanView> results = new LinkedList<>();
        DeveloperScansScanView resultView = Mockito.mock(DeveloperScansScanView.class);
        results.add(resultView);

        String mockedResultsJsonString = "mocked json string for results";
        Mockito.when(gson.toJson(results)).thenReturn(mockedResultsJsonString);

        File generatedFile = op.generateJsonFile(projectNameVersion, results, "");

        String expectedFilename = String.format("%s_%s_BlackDuck_DeveloperMode_Result.json", projectNameVersion.getName(), projectNameVersion.getVersion());
        String expectedPath = new File(scanDir, expectedFilename).getAbsolutePath();
        assertEquals(expectedPath, generatedFile.getAbsolutePath());

        String generatedString = FileUtils.readFileToString(generatedFile);
        assertEquals(mockedResultsJsonString, generatedString);
    }
}
