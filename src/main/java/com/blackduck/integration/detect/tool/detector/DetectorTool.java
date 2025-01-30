package com.blackduck.integration.detect.tool.detector;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.bdio2.model.GitInfo;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detect.configuration.ExcludeIncludeEnumFilter;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodePublisher;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import com.blackduck.integration.detect.tool.detector.report.DetectorDirectoryReport;
import com.blackduck.integration.detect.tool.detector.report.rule.EvaluatedDetectorRuleReport;
import com.blackduck.integration.detect.tool.detector.report.rule.ExtractedDetectorRuleReport;
import com.blackduck.integration.detect.tool.detector.report.util.DetectorReporter;
import com.blackduck.integration.detect.workflow.codelocation.DetectCodeLocation;
import com.blackduck.integration.detect.workflow.git.DetectorGitProjectInfoDecider;
import com.blackduck.integration.detect.workflow.nameversion.DetectorEvaluationNameVersionDecider;
import com.blackduck.integration.detect.workflow.nameversion.DetectorNameVersionDecider;
import com.blackduck.integration.detect.workflow.report.util.ReportConstants;
import com.blackduck.integration.detect.workflow.status.DetectIssue;
import com.blackduck.integration.detect.workflow.status.DetectIssueType;
import com.blackduck.integration.detect.workflow.status.DetectorStatus;
import com.blackduck.integration.detect.workflow.status.StatusEventPublisher;
import com.blackduck.integration.detect.workflow.status.StatusType;
import com.blackduck.integration.detect.workflow.status.UnrecognizedPaths;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detector.accuracy.directory.DirectoryEvaluation;
import com.blackduck.integration.detector.accuracy.directory.DirectoryEvaluator;
import com.blackduck.integration.detector.base.DetectorType;
import com.blackduck.integration.detector.finder.DirectoryFindResult;
import com.blackduck.integration.detector.finder.DirectoryFinder;
import com.blackduck.integration.detector.finder.DirectoryFinderOptions;
import com.blackduck.integration.detector.rule.DetectableDefinition;
import com.blackduck.integration.detector.rule.DetectorRule;
import com.blackduck.integration.detector.rule.DetectorRuleSet;
import com.blackduck.integration.util.NameVersion;

public class DetectorTool {
    private static final String THREE_TABS = "\t\t\t";
    private static final String TWO_TABS = "\t\t";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DirectoryFinder directoryFinder;
    private final CodeLocationConverter codeLocationConverter;
    private final DetectorIssuePublisher detectorIssuePublisher;
    private final StatusEventPublisher statusEventPublisher;
    private final ExitCodePublisher exitCodePublisher;
    private final DetectorEventPublisher detectorEventPublisher;
    private final DirectoryEvaluator directoryEvaluator;

    public DetectorTool(
        DirectoryFinder directoryFinder,
        CodeLocationConverter codeLocationConverter,
        DetectorIssuePublisher detectorIssuePublisher,
        StatusEventPublisher statusEventPublisher,
        ExitCodePublisher exitCodePublisher,
        DetectorEventPublisher detectorEventPublisher,
        DirectoryEvaluator directoryEvaluator
    ) {
        this.directoryFinder = directoryFinder;
        this.codeLocationConverter = codeLocationConverter;
        this.detectorIssuePublisher = detectorIssuePublisher;
        this.statusEventPublisher = statusEventPublisher;
        this.exitCodePublisher = exitCodePublisher;
        this.detectorEventPublisher = detectorEventPublisher;
        this.directoryEvaluator = directoryEvaluator;
    }

    public DetectorToolResult performDetectors(
        File directory,
        DetectorRuleSet detectorRuleSet,
        DirectoryFinderOptions directoryFinderOptions,
        String projectDetector,
        List<DetectorType> requiredDetectors,
        ExcludeIncludeEnumFilter<DetectorType> requiredAccuracyTypes,
        FileFinder fileFinder
    ) {
        logger.debug("Starting detector file system traversal.");
        Optional<DirectoryFindResult> findResultOptional = directoryFinder.findDirectories(directory, directoryFinderOptions, fileFinder);

        if (!findResultOptional.isPresent()) {
            logger.error("The source directory could not be searched for detectors - detector tool failed.");
            logger.error("Please ensure the provided source path is a directory and detect has access.");
            exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_CONFIGURATION, "Detector tool failed to run on the configured source path.");
            return new DetectorToolResult();
        }

        DirectoryFindResult findResult = findResultOptional.get();

        DirectoryEvaluation evaluation = directoryEvaluator.evaluate(findResult, detectorRuleSet);
        logger.debug("Finished detectors.");

        List<DetectorDirectoryReport> reports = new DetectorReporter().generateReport(evaluation);
        DetectorToolResult toolResult = publishAllResults(reports, directory, projectDetector, requiredDetectors, requiredAccuracyTypes);

        checkAndHandleOutOfMemoryIssue(reports);

        //Completed.
        logger.debug("Finished running detectors.");
        detectorEventPublisher.publishDetectorsComplete(toolResult);

        return toolResult;
    }

    private void checkAndHandleOutOfMemoryIssue(List<DetectorDirectoryReport> reports) {
        if (detectorIssuePublisher.hasOutOfMemoryIssue(reports)) {
            logger.error("Detected an issue: EXECUTABLE_TERMINATED_LIKELY_OUT_OF_MEMORY");
            exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_OUT_OF_MEMORY, "Executable terminated likely due to out of memory.");
        }
    }

    private DetectorToolResult publishAllResults(
        List<DetectorDirectoryReport> reports,
        File directory,
        String projectDetector,
        List<DetectorType> requiredDetectors,
        ExcludeIncludeEnumFilter<DetectorType> requiredAccuracyTypes
    ) {

        printExplanations(reports);
        Map<DetectorType, StatusType> statusMap = extractStatus(reports);
        publishStatusEvents(statusMap);
        publishFileEvents(reports);

        detectorIssuePublisher.publishIssues(statusEventPublisher, reports);
        boolean accuracyMet = checkAccuracyMet(reports, requiredAccuracyTypes);

        Set<DetectorType> allFoundTypes = statusMap.keySet();
        publishMissingDetectorEvents(requiredDetectors, allFoundTypes);

        Map<CodeLocation, DetectCodeLocation> codeLocationMap = createCodeLocationMap(reports, directory, accuracyMet);

        DetectorEvaluationNameVersionDecider detectorEvaluationNameVersionDecider = new DetectorEvaluationNameVersionDecider(new DetectorNameVersionDecider());
        Optional<NameVersion> bomToolProjectNameVersion = detectorEvaluationNameVersionDecider.decideSuggestion(reports, projectDetector);

        logger.debug("Finished evaluating detectors for project info.");

        DetectorGitProjectInfoDecider detectorGitProjectInfoDecider = new DetectorGitProjectInfoDecider();
        GitInfo gitInfo = detectorGitProjectInfoDecider.decideSuggestion(reports).orElse(GitInfo.none());

        return new DetectorToolResult(
            bomToolProjectNameVersion.orElse(null),
            gitInfo,
            new ArrayList<>(codeLocationMap.values()),
            allFoundTypes,
            new HashSet<>(),
            reports,
            codeLocationMap
        );
    }

    private void printExplanations(List<DetectorDirectoryReport> reports) {
        logger.info(ReportConstants.HEADING);
        logger.info("Detector Report");
        logger.info(ReportConstants.HEADING);
        boolean anyFound = false;
        for (DetectorDirectoryReport report : reports) {
            if (report.anyFound()) {
                anyFound = true;
                logger.info("\t" + report.getDirectory() + " (depth " + report.getDepth() + ")");
                report.getExtractedDetectors().forEach(extracted -> {
                    logger.info(TWO_TABS + extracted.getExtractedDetectable().getDetectable().getName() + ": SUCCESS");
                    extracted.getExtractedDetectable().getExplanations().forEach(explanation -> {
                        logger.info(THREE_TABS + explanation.describeSelf());
                    });

                    extracted.getAttemptedDetectables().forEach(attempted -> {
                        logger.info(TWO_TABS + attempted.getDetectable().getName() + ": ATTEMPTED");
                        logger.info(THREE_TABS + attempted.getStatusReason());
                        attempted.getExplanations().forEach(explanation -> {
                            logger.info(THREE_TABS + explanation.describeSelf());
                        });
                    });
                });
                report.getNotExtractedDetectors().forEach(notExtracted -> {
                    notExtracted.getAttemptedDetectables().forEach(attempted -> {
                        logger.info(TWO_TABS + attempted.getDetectable().getName() + ": FAILED");
                        attempted.getExplanations().forEach(explanation -> {
                            logger.info(THREE_TABS + explanation.describeSelf());
                        });
                    });
                });
            }
        }
        if (!anyFound) {
            logger.info("No detectors found.");
        }
        logger.info(ReportConstants.RUN_SEPARATOR);
    }

    private Map<DetectorType, StatusType> extractStatus(List<DetectorDirectoryReport> reports) {
        Set<DetectorType> success = reports.stream()
            .flatMap(directory -> directory.getExtractedDetectors().stream())
            .map(ExtractedDetectorRuleReport::getRule)
            .map(DetectorRule::getDetectorType)
            .collect(Collectors.toSet());

        Set<DetectorType> failure = reports.stream()
            .flatMap(directory -> directory.getNotExtractedDetectors().stream())
            .map(EvaluatedDetectorRuleReport::getRule)
            .map(DetectorRule::getDetectorType)
            .collect(Collectors.toSet());

        EnumMap<DetectorType, StatusType> statusMap = new EnumMap<>(DetectorType.class);

        success.forEach(detector -> statusMap.put(detector, StatusType.SUCCESS));
        failure.forEach(detector -> statusMap.put(detector, StatusType.FAILURE));

        return statusMap;
    }

    private Map<CodeLocation, DetectCodeLocation> createCodeLocationMap(List<DetectorDirectoryReport> reports, File sourcePath, boolean accuracyMet) {
        Map<CodeLocation, DetectCodeLocation> codeLocations = new HashMap<>();
        if (!accuracyMet) {
            logger.debug("Accuracy requirements were not met, so DetectorTool is suppressing any generated codelocations.");
            return codeLocations;
        }
        reports.forEach(report -> report.getExtractedDetectors().stream()
            .map(extracted -> codeLocationConverter.toDetectCodeLocation(
                sourcePath,
                extracted.getExtractedDetectable().getExtraction(),
                report.getDirectory(),
                extracted.getRule().getDetectorType().toString()
            ))
            .forEach(codeLocations::putAll));

        return codeLocations;
    }

    private void publishStatusEvents(Map<DetectorType, StatusType> statusMap) {
        statusMap.forEach((detectorType, statusType) ->
            statusEventPublisher.publishStatusSummary(new DetectorStatus(detectorType, statusType)));
        if (statusMap.containsValue(StatusType.FAILURE)) {
            exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_DETECTOR, "One or more detectors were not successful.");
        }
    }

    private void publishFileEvents(List<DetectorDirectoryReport> reports) { //TODO (detectors): What to publish here? Only succesfull? For now publishing only extracted.
        logger.debug("Publishing file events.");
        reports.stream()
            .flatMap(directory -> directory.getExtractedDetectors().stream())
            .forEach(extracted -> {
                extracted.getExtractedDetectable().getRelevantFiles().forEach(detectorEventPublisher::publishCustomerFileOfInterest);
                extracted.getExtractedDetectable().getExtraction().getRelevantFiles()
                    .forEach(detectorEventPublisher::publishCustomerFileOfInterest); //TODO (detectors): Is it weird i have to seperately publish extraction?

                List<File> paths = extracted.getExtractedDetectable().getExtraction().getUnrecognizedPaths();
                if ((paths != null) && (!paths.isEmpty())) {
                    detectorEventPublisher.publishUnrecognizedPaths(new UnrecognizedPaths(extracted.getRule().getDetectorType().toString(), paths));
                }
                extracted.getAttemptedDetectables().forEach(attempted -> {
                    attempted.getRelevantFiles().forEach(detectorEventPublisher::publishCustomerFileOfInterest);
                });
            });

    }

    private boolean checkAccuracyMet(List<DetectorDirectoryReport> reports, ExcludeIncludeEnumFilter<DetectorType> requiredAccuracyTypes) {
        //TODO (detectors): Should this check not-extracted too? Here is the potential for remediation, was there an attempted one that met accuracy?
        AtomicBoolean accuracyMet = new AtomicBoolean(true);
        for (DetectorDirectoryReport report : reports) {
            report.getExtractedDetectors().forEach(extracted -> {
                if (requiredAccuracyTypes.shouldInclude(extracted.getRule().getDetectorType())) {
                    DetectableDefinition extractedDetectable = extracted.getExtractedDetectable().getDetectable();
                    if (extractedDetectable.getAccuracyType() != DetectableAccuracyType.HIGH) {
                        accuracyMet.set(false);
                        exitCodePublisher.publishExitCode(new ExitCodeRequest(ExitCodeType.FAILURE_ACCURACY_NOT_MET));
                        List<String> messages = new ArrayList<>();

                        messages.add("Accuracy Not Met: " + extracted.getRule().getDetectorType());
                        messages.add("\tExtraction for " + extractedDetectable.getName() + " has accuracy of " + extractedDetectable.getAccuracyType()
                            + " but HIGH is required by the current detect.accuracy.required configuration.");
                        statusEventPublisher.publishIssue(new DetectIssue(DetectIssueType.DETECTOR, "Detector Issue", messages));
                    }
                }
            });
        }
        return accuracyMet.get();
    }

    private void publishMissingDetectorEvents(List<DetectorType> requiredDetectors, Set<DetectorType> applicable) {
        Set<DetectorType> missingDetectors = requiredDetectors.stream()
            .filter(it -> !applicable.contains(it))
            .collect(Collectors.toSet());
        if (!missingDetectors.isEmpty()) {
            String missingDetectorDisplay = missingDetectors.stream().map(Enum::toString).collect(Collectors.joining(","));
            logger.error("One or more required detector types were not found: {}", missingDetectorDisplay);
            exitCodePublisher.publishExitCode(new ExitCodeRequest(ExitCodeType.FAILURE_DETECTOR_REQUIRED));
        }
    }
}
