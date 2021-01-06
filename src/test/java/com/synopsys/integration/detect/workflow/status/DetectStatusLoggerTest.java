package com.synopsys.integration.detect.workflow.status;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.detect.configuration.enumeration.ExitCodeType;
import com.synopsys.integration.detect.workflow.result.AirGapDetectResult;
import com.synopsys.integration.detect.workflow.result.BlackDuckBomDetectResult;
import com.synopsys.integration.detect.workflow.result.DetectResult;
import com.synopsys.integration.detect.workflow.result.ReportDetectResult;
import com.synopsys.integration.log.BufferedIntLogger;
import com.synopsys.integration.log.LogLevel;

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
        statusLogger.logDetectStatus(loggerActual, statusSummaries, detectResults, detectIssues, ExitCodeType.SUCCESS);
        String actualOutput = loggerActual.getOutputString(LogLevel.INFO);
        Assertions.assertEquals(expectedOutput.trim().replaceAll("\r?\n", "\n"), actualOutput.trim().replaceAll("\r?\n", "\n"));
    }

    /* These methods create the objects that will contain the same data as the example output file.
       If these are changed you will need to update the expected output file also.
     */
    private List<Status> createStatus() {
        ArrayList<Status> statusSummaries = new ArrayList<>();
        Status status = new Status("description 1", StatusType.SUCCESS);
        statusSummaries.add(status);
        status = new Status("description 2", StatusType.FAILURE);
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
        String primaryMessage = String.format("%s primary message: %s", messagePrefix, type.name());
        String secondaryMessage = String.format("%s secondary message: %s", messagePrefix, type.name());
        messages.add(primaryMessage);
        messages.add(secondaryMessage);
        return new DetectIssue(type, messages);
    }
}
