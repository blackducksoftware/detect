package com.blackduck.integration.detect.workflow.report.output;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import org.apache.commons.lang3.StringUtils;

import com.blackduck.integration.common.util.Bds;
import com.blackduck.integration.detect.configuration.DetectInfo;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.autonomous.AutonomousManager;
import com.blackduck.integration.detect.tool.detector.DetectorToolResult;
import com.blackduck.integration.detect.tool.detector.report.detectable.AttemptedDetectableReport;
import com.blackduck.integration.detect.tool.detector.report.detectable.ExtractedDetectableReport;
import com.blackduck.integration.detect.workflow.event.Event;
import com.blackduck.integration.detect.workflow.event.EventSystem;
import com.blackduck.integration.detect.workflow.result.DetectResult;
import com.blackduck.integration.detect.workflow.status.DetectIssue;
import com.blackduck.integration.detect.workflow.status.FormattedCodeLocation;
import com.blackduck.integration.detect.workflow.status.Operation;
import com.blackduck.integration.detect.workflow.status.OperationType;
import com.blackduck.integration.detect.workflow.status.Status;
import com.blackduck.integration.detect.workflow.status.StatusType;
import com.blackduck.integration.detect.workflow.status.UnrecognizedPaths;
import com.blackduck.integration.detectable.detectable.explanation.Explanation;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detector.base.DetectorStatusCode;
import com.blackduck.integration.detector.base.DetectorType;
import com.blackduck.integration.util.NameVersion;

public class FormattedOutputManager {
    private final Set<FormattedCodeLocation> codeLocations = new HashSet<>();
    private final List<Status> statusSummaries = new ArrayList<>();
    private final List<DetectResult> detectResults = new ArrayList<>();
    private final List<DetectIssue> detectIssues = new ArrayList<>();
    private final List<ExitCodeRequest> overallStatus = new ArrayList<>();
    private final Map<String, List<File>> unrecognizedPaths = new HashMap<>();
    private final List<Operation> detectOperations = new LinkedList<>();
    private DetectorToolResult detectorToolResult = null;
    private NameVersion projectNameVersion = null;
    private SortedMap<String, String> rawMaskedPropertyValues = null;

    public FormattedOutputManager(EventSystem eventSystem) {
        eventSystem.registerListener(Event.DetectorsComplete, result -> detectorToolResult = result);
        eventSystem.registerListener(Event.StatusSummary, statusSummaries::add);
        eventSystem.registerListener(Event.Issue, detectIssues::add);
        eventSystem.registerListener(Event.ResultProduced, detectResults::add);
        eventSystem.registerListener(Event.CodeLocationsCompleted, codeLocations::addAll);
        eventSystem.registerListener(Event.UnrecognizedPaths, this::addUnrecognizedPaths);
        eventSystem.registerListener(Event.ProjectNameVersionChosen, nameVersion -> projectNameVersion = nameVersion);
        eventSystem.registerListener(Event.RawMaskedPropertyValuesCollected, keyValueMap -> rawMaskedPropertyValues = keyValueMap);
        eventSystem.registerListener(Event.DetectOperationsComplete, detectOperations::addAll);
    }

    public FormattedOutput createFormattedOutput(DetectInfo detectInfo, ExitCodeRequest exitCodeRequest, Optional<AutonomousManager> autonomousManagerOptional) {
        FormattedOutput formattedOutput = new FormattedOutput();
        formattedOutput.formatVersion = "0.5.0";
        formattedOutput.detectVersion = detectInfo.getDetectVersion();

        formattedOutput.results = Bds.of(detectResults)
            .map(result -> new FormattedResultOutput(result.getResultLocation(), result.getResultMessage(), removeTabsFromMessages(result.getResultSubMessages())))
            .toList();

        // avoid doing this if trans. guidance list is empty because it will just print the
        // same info as the "results" item above.
        List<FormattedResultOutput> transitiveOutput = Bds.of(detectResults)
        .map(result -> new FormattedResultOutput(result.getResultLocation(), result.getResultMessage(), removeTabsFromMessages(result.getTransitiveUpgradeGuidanceSubMessages())))
        .toList();
        if (!transitiveOutput.isEmpty() && !transitiveOutput.get(0).subMessages.isEmpty()) {
            formattedOutput.transitiveGuidance = transitiveOutput;
        }

        formattedOutput.status = Bds.of(statusSummaries)
            .map(status -> new FormattedStatusOutput(status.getDescriptionKey(), status.getStatusType().toString()))
            .toList();

        formattedOutput.issues = Bds.of(detectIssues)
            .map(issue -> new FormattedIssueOutput(issue.getType().name(), issue.getTitle(), issue.getMessages()))
            .toList();
        formattedOutput.operations = visibleOperations();

        // The exit status is known prior to this method being called and is passed in...
        // we will construct a reasonable facsimile to the other status, issues etc. for outputting the 
        // detect exit status.
        overallStatus.add(exitCodeRequest);
        formattedOutput.overallStatus = Bds.of(overallStatus)
                .map(overallStatus -> new FormattedStatusOutput(overallStatus.getExitCodeType().toString(), overallStatus.getReason()))
                .toList();

        if (detectorToolResult != null) { //TODO (Detector): Add formatted output results...
            formattedOutput.detectors = convertDetectors();
        }
        if (projectNameVersion != null) {
            formattedOutput.projectName = projectNameVersion.getName();
            formattedOutput.projectVersion = projectNameVersion.getVersion();
        }

        formattedOutput.codeLocations = Bds.of(this.codeLocations)
            .map(FormattedCodeLocationOutput::new)
            .toList();

        formattedOutput.unrecognizedPaths = new HashMap<>();
        unrecognizedPaths.keySet().forEach(key -> formattedOutput.unrecognizedPaths.put(key, unrecognizedPaths.get(key).stream().map(File::toString).collect(Collectors.toList())));

        formattedOutput.propertyValues = rawMaskedPropertyValues;
        if (autonomousManagerOptional.isPresent()) {
            AutonomousManager autonomousManager = autonomousManagerOptional.get();
            if (autonomousManager.getAutonomousScanEnabled()) {
                formattedOutput.propertyValues = autonomousManager.getAllScanSettingsProperties();
            }
        }

        return formattedOutput;
    }

    private List<FormattedOperationOutput> visibleOperations() {
        return Bds.of(detectOperations)
            .filter(operation -> operation.getOperationType() == OperationType.PUBLIC
                || operation.getStatusType() != StatusType.SUCCESS) //EITHER a public operation or a failed internal operation
            .map(operation -> new FormattedOperationOutput(
                Operation.formatTimestamp(operation.getStartTime()),
                Operation.formatTimestamp(operation.getEndTime().orElse(null)),
                operation.getName(),
                operation.getStatusType().name()
            ))
            .toList();
    }

    private List<String> removeTabsFromMessages(List<String> messages) {
        if (messages.isEmpty()) {
            return messages;
        }
        // if a line starts with a tab character remove it.  Any other tabs replace it with spaces to preserve a similar look to the messages as the console output.
        return messages.stream()
            .filter(StringUtils::isNotBlank)
            .map(message -> StringUtils.replaceOnce(message, "\t", ""))
            .map(message -> StringUtils.replace(message, "\t", "  "))
            .collect(Collectors.toList());
    }

    private List<FormattedDetectorOutput> convertDetectors() {
        List<FormattedDetectorOutput> outputs = new ArrayList<>();
        if (detectorToolResult != null) {

            detectorToolResult.getDetectorReports().forEach(report -> {
                report.getExtractedDetectors().forEach(extracted -> {
                    extracted.getAttemptedDetectables().stream()
                        .map(attempted -> convertAttempted(report.getDirectory(), extracted.getRule().getDetectorType(), attempted, "ATTEMPTED", attempted.getStatusCode()))
                        .forEach(outputs::add);

                    outputs.add(convertExtracted(report.getDirectory(), extracted.getRule().getDetectorType(), extracted.getExtractedDetectable(), "SUCCESS"));
                });
                report.getNotExtractedDetectors().forEach(notExtracted -> {
                    notExtracted.getAttemptedDetectables().stream()
                        .map(attempted -> convertAttempted(report.getDirectory(), notExtracted.getRule().getDetectorType(), attempted, "FAILURE", attempted.getStatusCode()))
                        .forEach(outputs::add);
                });
            });
        }
        return outputs;
    }

    private FormattedDetectorOutput convertAttempted(
        File directory,
        DetectorType detectorType,
        AttemptedDetectableReport attempted,
        String status,
        DetectorStatusCode overrideStatusCode
    ) {
        FormattedDetectorOutput detectorOutput = new FormattedDetectorOutput();
        detectorOutput.folder = directory.toString();
        detectorOutput.detectorName = attempted.getDetectable().getName();
        detectorOutput.detectorType = detectorType.toString();
        detectorOutput.detectorAccuracy = attempted.getDetectable().getAccuracyType().toString();

        detectorOutput.extracted = false;
        detectorOutput.status = status;
        detectorOutput.statusCode = overrideStatusCode;
        detectorOutput.statusReason = attempted.getStatusReason();
        detectorOutput.explanations = Bds.of(attempted.getExplanations()).map(Explanation::describeSelf).toList();
        return detectorOutput;
    }

    private FormattedDetectorOutput convertExtracted(File directory, DetectorType detectorType, ExtractedDetectableReport extracted, String status) {
        FormattedDetectorOutput detectorOutput = new FormattedDetectorOutput();
        detectorOutput.folder = directory.toString();
        detectorOutput.detectorName = extracted.getDetectable().getName();
        detectorOutput.detectorType = detectorType.toString();
        detectorOutput.detectorAccuracy = extracted.getDetectable().getAccuracyType().toString();

        detectorOutput.extracted = true;
        detectorOutput.status = status;
        detectorOutput.statusCode = DetectorStatusCode.PASSED;
        detectorOutput.statusReason = "Passed";
        detectorOutput.explanations = Bds.of(extracted.getExplanations()).map(Explanation::describeSelf).toList();

        Extraction extraction = extracted.getExtraction();
        detectorOutput.extractedReason = extraction.getDescription();
        detectorOutput.relevantFiles = Bds.of(extraction.getRelevantFiles()).map(File::toString).toList();
        detectorOutput.projectName = extraction.getProjectName();
        detectorOutput.projectVersion = extraction.getProjectVersion();
        if (extraction.getCodeLocations() != null) {
            detectorOutput.codeLocationCount = extraction.getCodeLocations().size();
        }

        return detectorOutput;
    }

    private void addUnrecognizedPaths(UnrecognizedPaths unrecognizedPaths) {
        if (!this.unrecognizedPaths.containsKey(unrecognizedPaths.getGroup())) {
            this.unrecognizedPaths.put(unrecognizedPaths.getGroup(), new ArrayList<>());
        }
        this.unrecognizedPaths.get(unrecognizedPaths.getGroup()).addAll(unrecognizedPaths.getPaths());
    }

}
