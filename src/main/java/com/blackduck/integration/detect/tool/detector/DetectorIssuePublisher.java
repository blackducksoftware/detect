package com.blackduck.integration.detect.tool.detector;

import java.util.ArrayList;
import java.util.List;

import com.blackduck.integration.detect.tool.detector.report.DetectorDirectoryReport;
import com.blackduck.integration.detect.workflow.status.DetectIssue;
import com.blackduck.integration.detect.workflow.status.DetectIssueType;
import com.blackduck.integration.detect.workflow.status.StatusEventPublisher;
import com.blackduck.integration.detector.base.DetectorStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetectorIssuePublisher {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void publishIssues(StatusEventPublisher statusEventPublisher, List<DetectorDirectoryReport> reports) {
        //TODO (detectors): just verify we don't want to publish 'attempted' when successfully extracted, right now publishing all attempted in not-extracted.
        String spacer = "\t";
        for (DetectorDirectoryReport report : reports) {
            report.getNotExtractedDetectors().forEach(notExtracted -> {
                notExtracted.getAttemptedDetectables().forEach(attempted -> {
                    List<String> messages = new ArrayList<>();
                    messages.add(attempted.getStatusCode() + ": " + attempted.getDetectable().getName());
                    messages.add(spacer + attempted.getStatusReason());
                    statusEventPublisher.publishIssue(new DetectIssue(DetectIssueType.DETECTOR, "Detector Issue", messages));
                });
            });

        }
    }
    public boolean hasOutOfMemoryIssue(List<DetectorDirectoryReport> reports) {
        return reports.stream()
            .flatMap(report -> report.getNotExtractedDetectors().stream())
            .flatMap(notExtracted -> notExtracted.getAttemptedDetectables().stream())
            .anyMatch(attemptedDetectableReport -> {
                return attemptedDetectableReport.getStatusCode() == DetectorStatusCode.EXECUTABLE_TERMINATED_LIKELY_OUT_OF_MEMORY;
            });
    }

}
