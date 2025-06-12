package com.blackduck.integration.detect.tool.signaturescanner.operation;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodePublisher;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerReport;
import com.blackduck.integration.detect.workflow.status.DetectIssue;
import com.blackduck.integration.detect.workflow.status.DetectIssueType;
import com.blackduck.integration.detect.workflow.status.SignatureScanStatus;
import com.blackduck.integration.detect.workflow.status.StatusEventPublisher;
import com.blackduck.integration.detect.workflow.status.StatusType;

public class PublishSignatureScanReports {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExitCodePublisher exitCodePublisher;
    private final StatusEventPublisher statusEventPublisher;
    private final boolean treatSkippedScanAsSuccess;

    public PublishSignatureScanReports(ExitCodePublisher exitCodePublisher, StatusEventPublisher statusEventPublisher, boolean treatSkippedScanAsSuccess) {
        this.exitCodePublisher = exitCodePublisher;
        this.statusEventPublisher = statusEventPublisher;
        this.treatSkippedScanAsSuccess = treatSkippedScanAsSuccess;
    }

    public void publishReports(List<SignatureScannerReport> signatureScannerReports) {
        // TODO this is a concern as it prints success even when we later fail with SCASS steps
        signatureScannerReports.forEach(this::publishReport);
        
        signatureScannerReports.stream()
            .filter(SignatureScannerReport::isFailure)
            .findAny()
            .ifPresent(report -> {
                logger.error(String.format(
                    "The Signature Scanner reported an error%s. The Signature Scanner log may contain relevant information.",
                    report.getExitCode().map(code -> " (" + code + ")").orElse(".")
                ));
                exitCodePublisher.publishExitCode(new ExitCodeRequest(ExitCodeType.FAILURE_SCAN));
            });

        if (!treatSkippedScanAsSuccess) {
            signatureScannerReports.stream()
                .filter(SignatureScannerReport::isSkipped)
                .findAny()
                .ifPresent(report -> {
                    logger.error(
                        "The Signature Scanner skipped a scan because the minimum scan interval was not met.");
                    exitCodePublisher.publishExitCode(new ExitCodeRequest(ExitCodeType.FAILURE_MINIMUM_INTERVAL_NOT_MET));
                });
        }
    }

    private void publishReport(SignatureScannerReport signatureScannerReport) {
        if (signatureScannerReport.isSuccessful() || (signatureScannerReport.isSkipped() && treatSkippedScanAsSuccess)) {
            statusEventPublisher.publishStatusSummary(new SignatureScanStatus(signatureScannerReport.getSignatureScanPath().getTargetCanonicalPath(), StatusType.SUCCESS));
            return;
        }

        String scanTargetPath = signatureScannerReport.getSignatureScanPath().getTargetCanonicalPath();
        if (signatureScannerReport.isSkipped()) {
            statusEventPublisher.publishIssue(new DetectIssue(
                DetectIssueType.SIGNATURE_SCANNER,
                String.format("Scanning target %s was never scanned by the BlackDuck CLI.", scanTargetPath),
                Arrays.asList("The minimum scan interval was not met and this scan was skipped by the BlackDuck CLI.")
            ));
        } else if (!signatureScannerReport.hasOutput()) {
            String errorMessage = String.format("Scanning target %s was never scanned by the BlackDuck CLI.", scanTargetPath);
            logger.info(errorMessage);
            statusEventPublisher.publishIssue(new DetectIssue(DetectIssueType.SIGNATURE_SCANNER, "Black Duck Signature Scanner", Arrays.asList(errorMessage)));
        } else {
            String errorMessage = signatureScannerReport.getErrorMessage()
                .map(message -> String.format("Scanning target %s failed: %s", scanTargetPath, message))
                .orElse(String.format("Scanning target %s failed for an unknown reason.", scanTargetPath));
            logger.error(errorMessage);
            signatureScannerReport.getException().ifPresent(exception -> logger.debug(errorMessage, exception));

            statusEventPublisher.publishIssue(new DetectIssue(DetectIssueType.SIGNATURE_SCANNER, "Black Duck Signature Scanner", Arrays.asList(errorMessage)));
        }

        statusEventPublisher.publishStatusSummary(new SignatureScanStatus(signatureScannerReport.getSignatureScanPath().getTargetCanonicalPath(), StatusType.FAILURE));
    }
}
