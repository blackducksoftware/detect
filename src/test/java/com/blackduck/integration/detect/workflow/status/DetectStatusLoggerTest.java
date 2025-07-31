package com.blackduck.integration.detect.workflow.status;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.workflow.result.AirGapDetectResult;
import com.blackduck.integration.detect.workflow.result.BlackDuckBomDetectResult;
import com.blackduck.integration.detect.workflow.result.DetectResult;
import com.blackduck.integration.detect.workflow.result.ReportDetectResult;
import com.blackduck.integration.log.BufferedIntLogger;
import com.blackduck.integration.log.LogLevel;

public class DetectStatusLoggerTest {
    @Test
    public void testContent() throws IOException {
        File expectedOutputFile = new File("src/test/resources/workflow/status/expectedStatusLoggerOutput.txt");
        String expectedOutput = FileUtils.readFileToString(expectedOutputFile, StandardCharsets.UTF_8);
        BufferedIntLogger loggerActual = new BufferedIntLogger();

        DetectStatusLogger statusLogger = new DetectStatusLogger();
        List<Status> statusSummaries = createStatus();
        List<DetectResult> detectResults = createResults();
        List<DetectIssue> detectIssues = createIssues();
        List<Operation> detectOperations = createOperations();
        statusLogger.logDetectStatus(loggerActual, statusSummaries, detectResults, detectIssues, detectOperations, new ExitCodeRequest(ExitCodeType.SUCCESS), Optional.empty());
        String actualOutput = loggerActual.getOutputString(LogLevel.INFO);
        assertEquals(expectedOutput.trim().replaceAll("\r?\n", "\n"), actualOutput.trim().replaceAll("\r?\n", "\n"));
    }

    @Test
    public void testDebugContent() throws IOException {
        File expectedOutputFile = new File("src/test/resources/workflow/status/expectedDebugStatusLoggerOutput.txt");
        String expectedOutput = FileUtils.readFileToString(expectedOutputFile, StandardCharsets.UTF_8);
        BufferedIntLogger loggerActual = new BufferedIntLogger();

        DetectStatusLogger statusLogger = new DetectStatusLogger();
        List<Status> statusSummaries = createStatus();
        List<DetectResult> detectResults = createResults();
        List<DetectIssue> detectIssues = createIssues();
        List<Operation> detectOperations = createOperations();
        statusLogger.logDetectStatus(loggerActual, statusSummaries, detectResults, detectIssues, detectOperations, new ExitCodeRequest(ExitCodeType.SUCCESS), Optional.empty());
        String actualOutput = loggerActual.getOutputString(LogLevel.DEBUG);
        assertEquals(expectedOutput.trim().replaceAll("\r?\n", "\n"), actualOutput.trim().replaceAll("\r?\n", "\n"));
    }

    /* These methods create the objects that will contain the same data as the example output file.
       If these are changed you will need to update the expected output file also.
     */
    private List<Status> createStatus() {
        ArrayList<Status> statusSummaries = new ArrayList<>();
        Status status = new Status("description 1", StatusType.SUCCESS);
        statusSummaries.add(status);
        return statusSummaries;
    }

    private List<DetectResult> createResults() {
        ArrayList<DetectResult> detectResults = new ArrayList<>();
        DetectResult result = new BlackDuckBomDetectResult("https://example.com/api/projects/project_1");
        detectResults.add(result);
        result = new BlackDuckBomDetectResult("https://example.com/api/projects/project_2");
        detectResults.add(result);
        result = new AirGapDetectResult("./air_gap/directory");
        detectResults.add(result);
        result = new ReportDetectResult("report_1", "./report/1/report_file");
        detectResults.add(result);
        return detectResults;
    }

    private List<DetectIssue> createIssues() {
        ArrayList<DetectIssue> detectIssues = new ArrayList<>();
        for (DetectIssueType type : DetectIssueType.values()) {
            detectIssues.add(createIssue(type, "Junit Test Issue Set 1"));
        }
        for (DetectIssueType type : DetectIssueType.values()) {
            detectIssues.add(createIssue(type, "Junit Test Issue Set 2"));
        }
        return detectIssues;
    }

    private DetectIssue createIssue(DetectIssueType type, String messagePrefix) {
        ArrayList<String> messages = new ArrayList<>();
        String title = String.format("%s Detect Issue", messagePrefix);
        String primaryMessage = String.format("%s primary message: %s", messagePrefix, type.name());
        String secondaryMessage = String.format("%s secondary message: %s", messagePrefix, type.name());
        messages.add(primaryMessage);
        messages.add(secondaryMessage);
        return new DetectIssue(type, title, messages);
    }

    private List<Operation> createOperations() {
        ArrayList<Operation> operations = new ArrayList<>();
        Operation operation = new Operation(Instant.EPOCH, OperationType.PUBLIC, null, "description 1", StatusType.SUCCESS, null, null);
        operations.add(operation);
        operation = new Operation(Instant.EPOCH, OperationType.PUBLIC, null, "description 2", StatusType.FAILURE, null, null);
        operations.add(operation);
        return operations;
    }
}
