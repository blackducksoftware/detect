package com.blackduck.integration.detect.tool.signaturescanner;

import static com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerReportTestUtil.skippedReport;
import static com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerReportTestUtil.successfulReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodePublisher;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerReport;
import com.blackduck.integration.detect.tool.signaturescanner.operation.PublishSignatureScanReports;
import com.blackduck.integration.detect.workflow.status.DetectIssue;
import com.blackduck.integration.detect.workflow.status.Status;
import com.blackduck.integration.detect.workflow.status.StatusEventPublisher;
import com.blackduck.integration.detect.workflow.status.StatusType;

public class PublishSignatureScanReportsTest {
    @Test
    public void treatsSkippedAsSuccess() {
        List<SignatureScannerReport> reports = Arrays.asList(
            skippedReport("skipped", 2),
            successfulReport("success", 1)
        );

        EventAssert publishedEvents = publishReports(reports, true);

        publishedEvents.assertWinningExitCode(ExitCodeType.SUCCESS);
    }

    @Test
    public void treatsSkippedAsIntervalNotMet() {
        List<SignatureScannerReport> reports = Arrays.asList(
            skippedReport("skipped", 2),
            successfulReport("success", 1)
        );

        EventAssert publishedEvents = publishReports(reports, false);

        publishedEvents.assertWinningExitCode(ExitCodeType.FAILURE_MINIMUM_INTERVAL_NOT_MET);
    }

    @Test
    public void treatsStatusSummaryAsSuccess() {
        List<SignatureScannerReport> reports = Collections.singletonList(
            skippedReport("skipped", 2)
        );

        EventAssert publishedEvents = publishReports(reports, true);

        publishedEvents.assertOnlySuccesfullStatusSummaries();
        publishedEvents.assertNoIssues();
    }

    @Test
    public void skippedIsAnIssueAndFailure() {
        List<SignatureScannerReport> reports = Collections.singletonList(
            skippedReport("skipped", 2)
        );

        EventAssert publishedEvents = publishReports(reports, false);

        publishedEvents.assertOnlyFailureStatusSummaries();
        publishedEvents.assertAnIssueContains("minimum scan interval");
    }

    public EventAssert publishReports(List<SignatureScannerReport> reports, boolean treatSkippedAsSuccess) {
        InMemoryStatusEventPublisher statusEventPublisher = new InMemoryStatusEventPublisher();
        InMemoryExitCodePublisher exitCodePublisher = new InMemoryExitCodePublisher();
        PublishSignatureScanReports publishSignatureScanReports = new PublishSignatureScanReports(exitCodePublisher, statusEventPublisher, treatSkippedAsSuccess);
        publishSignatureScanReports.publishReports(reports);

        return new EventAssert(exitCodePublisher, statusEventPublisher);
    }

    private class EventAssert {
        private final InMemoryExitCodePublisher exitCodePublisher;
        private final InMemoryStatusEventPublisher statusEventPublisher;

        private EventAssert(InMemoryExitCodePublisher exitCodePublisher, InMemoryStatusEventPublisher statusEventPublisher) {
            this.exitCodePublisher = exitCodePublisher;
            this.statusEventPublisher = statusEventPublisher;
        }

        public void assertWinningExitCode(ExitCodeType exitCodeType) {
            ExitCodeType winning = exitCodePublisher.exitCodeRequests.stream()
                .map(ExitCodeRequest::getExitCodeType)
                .reduce(ExitCodeType.SUCCESS, ExitCodeType::getWinningExitCodeType);

            Assertions.assertEquals(exitCodeType, winning);
        }

        public void assertOnlySuccesfullStatusSummaries() {
            Assertions.assertTrue(statusEventPublisher.statusSummaries.stream()
                .allMatch(status -> status.getStatusType().equals(StatusType.SUCCESS)));
        }

        public void assertNoIssues() {
            Assertions.assertTrue(statusEventPublisher.issues.isEmpty());
        }

        public void assertOnlyFailureStatusSummaries() {
            Assertions.assertTrue(statusEventPublisher.statusSummaries.stream()
                .allMatch(status -> status.getStatusType().equals(StatusType.FAILURE)));
        }

        public void assertAnIssueContains(String text) {
            Assertions.assertTrue(statusEventPublisher.issues.stream()
                .anyMatch(issue -> issue.getMessages().stream()
                    .anyMatch(message -> message.contains(text))));
        }
    }

    private class InMemoryExitCodePublisher extends ExitCodePublisher {
        public List<ExitCodeRequest> exitCodeRequests = new ArrayList<>();

        public InMemoryExitCodePublisher() {
            super(null);
        }

        @Override
        public void publishExitCode(ExitCodeType exitCodeType) {
            exitCodeRequests.add(new ExitCodeRequest(exitCodeType));
        }
    }

    private class InMemoryStatusEventPublisher extends StatusEventPublisher {
        public List<Status> statusSummaries = new ArrayList<>();
        public List<DetectIssue> issues = new ArrayList<>();

        public InMemoryStatusEventPublisher() {
            super(null);
        }

        @Override
        public void publishStatusSummary(Status status) {
            statusSummaries.add(status);
        }

        @Override
        public void publishIssue(DetectIssue issue) {
            issues.add(issue);
        }
    }
}
