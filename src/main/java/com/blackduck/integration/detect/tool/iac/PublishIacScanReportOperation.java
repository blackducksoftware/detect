package com.blackduck.integration.detect.tool.iac;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodePublisher;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import com.blackduck.integration.detect.workflow.status.DetectIssue;
import com.blackduck.integration.detect.workflow.status.DetectIssueType;
import com.blackduck.integration.detect.workflow.status.StatusEventPublisher;
import com.blackduck.integration.detect.workflow.status.StatusType;

public class PublishIacScanReportOperation {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ExitCodePublisher exitCodePublisher;
    private final StatusEventPublisher statusEventPublisher;

    public PublishIacScanReportOperation(ExitCodePublisher exitCodePublisher, StatusEventPublisher statusEventPublisher) {
        this.exitCodePublisher = exitCodePublisher;
        this.statusEventPublisher = statusEventPublisher;
    }

    public void publishReports(List<IacScanReport> iacScanReports) {
        logErrors(iacScanReports);
        publishExitCode(iacScanReports);
        publishStatusEvents(iacScanReports);
    }

    private void logErrors(List<IacScanReport> iacScanReports) {
        iacScanReports.stream()
            .filter(report -> report.getErrorMessage().isPresent())
            .forEach(report -> logger.error(String.format("%s for target %s", report.getErrorMessage().get(), report.getScanTarget())));
    }

    private void publishExitCode(List<IacScanReport> iacScanReports) {
        iacScanReports.stream()
            .filter(iacScanReport -> iacScanReport.getErrorMessage().isPresent())
            .findAny()
            .ifPresent(iacScanReport -> exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_IAC));
    }

    private void publishStatusEvents(List<IacScanReport> iacScanReports) {
        for (IacScanReport iacScanReport : iacScanReports) {
            if (!iacScanReport.getErrorMessage().isPresent()) {
                statusEventPublisher.publishStatusSummary(new IacScanStatus(iacScanReport.getScanTarget(), StatusType.SUCCESS));
            } else {
                String errorMessage = iacScanReport.getErrorMessage().get();
                statusEventPublisher.publishIssue(new DetectIssue(DetectIssueType.IAC_SCANNER, "IaC", Collections.singletonList(errorMessage)));
                statusEventPublisher.publishStatusSummary(new IacScanStatus(iacScanReport.getScanTarget(), StatusType.FAILURE));
            }
        }
    }
}
