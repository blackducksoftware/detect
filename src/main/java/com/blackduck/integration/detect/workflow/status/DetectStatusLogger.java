package com.blackduck.integration.detect.workflow.status;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.autonomous.AutonomousManager;
import com.blackduck.integration.detect.lifecycle.autonomous.ScanSettingsSerializer;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import com.blackduck.integration.detect.workflow.result.DetectResult;
import com.blackduck.integration.log.IntLogger;

public class DetectStatusLogger {

    private static final String EQUAL_SEPARATOR = "===============================";

    private static final List<ExitCodeType> doNotRequireAdvice = Arrays.asList(
        ExitCodeType.SUCCESS,
        ExitCodeType.FAILURE_POLICY_VIOLATION,
        ExitCodeType.FAILURE_POLICY_NAME_VIOLATION,
        ExitCodeType.FAILURE_BLACKDUCK_CONNECTIVITY,
        ExitCodeType.FAILURE_PROXY_CONNECTIVITY,
        ExitCodeType.FAILURE_DETECTOR_REQUIRED,
        ExitCodeType.FAILURE_BLACKDUCK_VERSION_NOT_SUPPORTED,
        ExitCodeType.FAILURE_BLACKDUCK_FEATURE_ERROR,
        ExitCodeType.FAILURE_TIMEOUT,
        ExitCodeType.FAILURE_CONFIGURATION
    );

    public void logDetectStatus(
        IntLogger logger,
        List<Status> statusSummaries,
        List<DetectResult> detectResults,
        List<DetectIssue> detectIssues,
        List<Operation> detectOperations,
        ExitCodeRequest exitCodeRequest,
        Optional<AutonomousManager> autonomousManagerOptional
    ) {
        logger.info("");
        logger.info("");
        logger.debug("=== Additional  Information ===");
        logger.debug("");
        logDetectOperations(logger, detectOperations);
        logger.debug("");
        if (autonomousManagerOptional.isPresent()) {
            logAutonomousDecisions(logger, autonomousManagerOptional.get());
        }

        logDetectIssues(logger, detectIssues);
        logDetectResults(logger, detectResults);
        logDetectStatus(logger, statusSummaries);

        Optional<String> gettingSupportAdvice = getAdvice(exitCodeRequest);

        String exitMessage = String.format("Overall Status: %s - %s", exitCodeRequest.getExitCodeType().toString(), exitCodeRequest.getReason());
        
        if (exitCodeRequest.getExitCodeType().isSuccess()) {
            logger.info(exitMessage);
        } else {
            logger.error(exitMessage); 
        }
        if (gettingSupportAdvice.isPresent()) {
            logger.info("");
            logger.info(gettingSupportAdvice.get());
        }
        logger.info("");
        logger.info(EQUAL_SEPARATOR);
        logger.info("");
    }

    private void logDetectIssues(IntLogger logger, List<DetectIssue> detectIssues) {
        if (!detectIssues.isEmpty()) {
            logger.info("======== Detect Issues ========");
            logger.info("");

            Predicate<DetectIssue> detectorsFilter = issue -> issue.getType() == DetectIssueType.DETECTOR;
            Predicate<DetectIssue> detectableToolsFilter = issue -> issue.getType() == DetectIssueType.DETECTABLE_TOOL;
            Predicate<DetectIssue> exceptionsFilter = issue -> issue.getType() == DetectIssueType.EXCEPTION;
            Predicate<DetectIssue> deprecationsFilter = issue -> issue.getType() == DetectIssueType.DEPRECATION;
            Predicate<DetectIssue> propertyKeysFilter = issue -> issue.getType() == DetectIssueType.PROPERTY_KEY;
            logIssuesInGroup(logger, "DETECTORS:", detectorsFilter, detectIssues);
            logIssuesInGroup(logger, "DETECTABLE TOOLS:", detectableToolsFilter, detectIssues);
            logIssuesInGroup(logger, "EXCEPTIONS:", exceptionsFilter, detectIssues);
            logIssuesInGroup(logger, "DEPRECATIONS:", deprecationsFilter, detectIssues);
            logIssuesInGroup(logger, "PROPERTY KEYS:", propertyKeysFilter, detectIssues);
        }
    }

    private void logIssuesInGroup(IntLogger logger, String groupHeading, Predicate<DetectIssue> issueFilter, List<DetectIssue> detectIssues) {
        List<DetectIssue> detectIssueList = detectIssues.stream().filter(issueFilter).collect(Collectors.toList());
        if (!detectIssueList.isEmpty()) {
            logger.info(groupHeading);
            for (DetectIssue issue : detectIssueList) {
                logger.info("\t" + issue.getTitle());
                issue.getMessages().forEach(message -> logger.info("\t\t" + message));
                logger.info("");
            }
        }
    }

    private void logDetectResults(IntLogger logger, List<DetectResult> detectResults) {
        if (!detectResults.isEmpty()) {
            logger.info("======== Detect Result ========");
            logger.info("");
            for (DetectResult detectResult : detectResults) {
                logger.info(detectResult.getResultMessage());
                if (!detectResult.getResultSubMessages().isEmpty()) {
                    detectResult.getResultSubMessages().forEach(subMessage -> logger.info(String.format("\t%s", subMessage)));
                }

                if (!detectResult.getTransitiveUpgradeGuidanceSubMessages().isEmpty()) {
                    logger.info("");
                    logger.info("===== Transitive Guidance =====");
                    detectResult.getTransitiveUpgradeGuidanceSubMessages().forEach(subMessage -> logger.info(String.format("\t%s", subMessage)));
                }
            }
            logger.info("");
        }
    }

    private void logDetectStatus(IntLogger logger, List<Status> statusSummaries) {
        // sort by type, and within type, sort by description
        statusSummaries.sort((left, right) -> {
            if (left.getClass() == right.getClass()) {
                return left.getDescriptionKey().compareTo(right.getDescriptionKey());
            } else {
                return left.getClass().getName().compareTo(right.getClass().getName());
            }
        });
        logger.info("======== Detect Status ========");
        logger.info("");
        Class<? extends Status> previousSummaryClass = null;

        for (Status status : statusSummaries) {
            if (previousSummaryClass != null && !previousSummaryClass.equals(status.getClass())) {
                logger.info("");
            }
            
            String detectorStatus = String.format("%s: %s", status.getDescriptionKey(), status.getStatusType().toString());
            
            if (status.getStatusType() == StatusType.SUCCESS) {
                logger.info(detectorStatus);
            } else {
                logger.error(detectorStatus);
            }

            previousSummaryClass = status.getClass();
        }
    }

    private void logDetectOperations(IntLogger logger, List<Operation> operations) {
        List<Operation> sortedOperations = operations.stream()
            .filter(operation -> operation.getOperationType() == OperationType.PUBLIC
                || operation.getStatusType() != StatusType.SUCCESS) //EITHER a public operation or a failed internal operation
            .sorted(Comparator.comparing(Operation::getEndTimeOrStartTime)
                .thenComparing(Operation::getName))
            .collect(Collectors.toList());
        logger.debug("====== Detect Operations ======");
        logger.debug("");
        for (Operation operation : sortedOperations) {
            logger.debug(String.format("%s: %s", operation.getName(), operation.getStatusType().toString()));
        }
        logger.debug("");
        logger.debug(EQUAL_SEPARATOR);
        logger.debug("");
    }

    private void logAutonomousDecisions(IntLogger logger, AutonomousManager autonomousManager) {
        if (autonomousManager.getAutonomousScanEnabled()) {
            logger.debug("====== Autonomous Scan ======");
            logger.debug("");
            logger.debug(String.format("Autonomous Scan Settings File Location: %s", autonomousManager.getScanSettingsTargetFile().getPath()));
            logger.debug("");
            logger.debug("Autonomous Scan Decisions:");
            logger.debug(ScanSettingsSerializer.serializeScanSettingsModel(autonomousManager.getScanSettingsModel()));
            logger.debug("");
            logger.debug(EQUAL_SEPARATOR);
            logger.debug("");
        }
    }

    private Optional<String> getAdvice(ExitCodeRequest exitCodeRequest) {
        if (!doNotRequireAdvice.contains(exitCodeRequest.getExitCodeType())) {
            return Optional.of(
                "If you need help troubleshooting this problem, generate a diagnostic zip file by adding '-d' to the command line, and provide it to Black Duck Technical Support. See 'Diagnostic Mode' in the Detect documentation for more information.");
        }
        return Optional.empty();
    }
}
