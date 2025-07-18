package com.blackduck.integration.detect.workflow.blackduck.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodeRequestWithCustomDescription;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.blackduck.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType;
import com.blackduck.integration.blackduck.api.generated.view.ComponentPolicyRulesView;
import com.blackduck.integration.blackduck.api.generated.view.ProjectVersionComponentVersionView;
import com.blackduck.integration.blackduck.api.generated.view.ProjectVersionPolicyRulesView;
import com.blackduck.integration.blackduck.api.generated.view.ProjectVersionView;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.blackduck.service.dataservice.ProjectBomService;
import com.blackduck.integration.blackduck.service.model.PolicyStatusDescription;
import com.blackduck.integration.common.util.Bdo;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodePublisher;
import com.blackduck.integration.exception.IntegrationException;

public class PolicyChecker {
    private final Logger logger = LoggerFactory.getLogger(PolicyChecker.class);

    private final ExitCodePublisher exitCodePublisher;
    private final BlackDuckApiClient blackDuckApiClient;
    private final ProjectBomService projectBomService;

    public PolicyChecker(ExitCodePublisher exitCodePublisher, BlackDuckApiClient blackDuckApiClient, ProjectBomService projectBomService) {
        this.exitCodePublisher = exitCodePublisher;
        this.blackDuckApiClient = blackDuckApiClient;
        this.projectBomService = projectBomService;
    }

    public void checkPolicyByName(List<String> policyNamesToFailPolicyCheck, ProjectVersionView projectVersionView) throws IntegrationException {
        Optional<List<ProjectVersionPolicyRulesView>> activePolicyRulesOptional = projectBomService.getActivePoliciesForVersion(projectVersionView);

        if (activePolicyRulesOptional.isPresent()) {
            logger.info("Checking for policy violations by name.");
            List<ProjectVersionPolicyRulesView> activePolicyRules = activePolicyRulesOptional.get();

            List<String> allViolatedPolicyNames = activePolicyRules.stream()
                    .filter(rule -> ProjectVersionComponentPolicyStatusType.IN_VIOLATION.equals(rule.getStatus()))
                    .map(ProjectVersionPolicyRulesView::getName)
                    .collect(Collectors.toList());

            List<String> fatalViolatedPolicyNames = allViolatedPolicyNames.stream()
                .filter(policyNamesToFailPolicyCheck::contains)
                .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(allViolatedPolicyNames)) {
                AllPolicyViolations allComponentsWithViolations = collectComponentsWithPolicyViolations(projectVersionView, ComponentPolicyRulesView::getName, fatalViolatedPolicyNames::contains);
                logFatalViolationMessages(allComponentsWithViolations.fatalViolations);
                logNonFatalViolationMessages(allComponentsWithViolations.otherViolations);
                if (!fatalViolatedPolicyNames.isEmpty()) {
                    String violatedPolicyNames = StringUtils.join(fatalViolatedPolicyNames, ", ");
                    exitCodePublisher.publishExitCode(new ExitCodeRequestWithCustomDescription(ExitCodeType.FAILURE_POLICY_VIOLATION, "Detect found policy violations by name. The following policies were violated: " + violatedPolicyNames));
                }
            }
        } else {
            String availableLinks = StringUtils.join(projectVersionView.getAvailableLinks(), ", ");
            logger.warn(String.format(
                "It is not possible to check the active policy rules for this project/version. The active-policy-rules link must be present. The available links are: %s",
                availableLinks
            ));
        }
    }

    public void checkPolicyBySeverity(List<PolicyRuleSeverityType> severitiesToFailPolicyCheck, ProjectVersionView projectVersionView) throws IntegrationException {
        logger.info("Checking for policy violations by severity.");
        Optional<PolicyStatusDescription> policyStatusDescriptionOptional = fetchPolicyStatusDescription(projectVersionView);

        if (policyStatusDescriptionOptional.isPresent()) {
            PolicyStatusDescription policyStatusDescription = policyStatusDescriptionOptional.get();
            logger.info(policyStatusDescription.getPolicyStatusMessage());

            boolean atLeastOneViolationFound = areAnyPolicySeveritiesViolated(policyStatusDescription);

            if (atLeastOneViolationFound) {
                AllPolicyViolations allComponentsWithViolations = collectComponentsWithPolicyViolations(projectVersionView, ComponentPolicyRulesView::getSeverity, severitiesToFailPolicyCheck::contains);
                logFatalViolationMessages(allComponentsWithViolations.fatalViolations);
                logNonFatalViolationMessages(allComponentsWithViolations.otherViolations);

                // If Black Duck has reported policy violations in status description (fatalPolicySeveritiesAreViolated),
                // or we have noticed violations while examining components in the BOM (fatalRulesViolated),
                // fail the scan.
                if (!allComponentsWithViolations.fatalViolations.isEmpty() || areFatalPolicySeveritiesViolated(policyStatusDescription, severitiesToFailPolicyCheck)) {
                    exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_POLICY_VIOLATION);
                }
            }
        } else {
            String availableLinks = StringUtils.join(projectVersionView.getAvailableLinks(), ", ");
            logger.warn(String.format(
                "It is not possible to check the policy status for this project/version. The policy-status link must be present. The available links are: %s",
                availableLinks
            ));
        }
    }

    private Optional<PolicyStatusDescription> fetchPolicyStatusDescription(ProjectVersionView version) throws IntegrationException {
        return Bdo.of(projectBomService.getPolicyStatusForVersion(version))
            .peek(policyStatus -> logger.info(String.format("Overall Policy Status of project version as reported by Black Duck: %s", policyStatus.getOverallStatus().name())))
            .map(PolicyStatusDescription::new)
            .toOptional();
    }

    private <T> AllPolicyViolations collectComponentsWithPolicyViolations(ProjectVersionView projectVersionView, Function<ComponentPolicyRulesView, T> policyNameOrSeverityExtractor, Predicate<T> fatalPolicyCondition)
            throws IntegrationException {
        List<PolicyViolationInfo> fatalRulesViolated = new ArrayList<>();
        List<PolicyViolationInfo> otherRulesViolated = new ArrayList<>();
        logger.info("Searching BOM for components in violation of policy rules.");

        List<ProjectVersionComponentVersionView> bomComponents = projectBomService.getComponentsForProjectVersion(projectVersionView);
        for (ProjectVersionComponentVersionView component : bomComponents) {
            if (!component.getPolicyStatus().equals(ProjectVersionComponentPolicyStatusType.IN_VIOLATION)) {
                continue;
            }

            for (ComponentPolicyRulesView policyRule : blackDuckApiClient.getAllResponses(component.metaPolicyRulesLink())) {
                if (policyRule.getPolicyApprovalStatus().equals(ProjectVersionComponentPolicyStatusType.IN_VIOLATION)) {
                    PolicyViolationInfo violationInfo = new PolicyViolationInfo(component, policyRule);
                    if (fatalPolicyCondition.test(policyNameOrSeverityExtractor.apply(policyRule))) {
                        fatalRulesViolated.add(violationInfo);
                    } else {
                        otherRulesViolated.add(violationInfo);
                    }
                }
            }
        }

        return new AllPolicyViolations(fatalRulesViolated, otherRulesViolated);
    }

    private void logFatalViolationMessages(List<PolicyViolationInfo> fatalRulesViolated) {
        if (fatalRulesViolated.isEmpty()) {
            logger.info("No BOM components in violation of fatal policy rules found.");
            return;
        }
        logger.info("Fatal:");
        logViolationMessages(fatalRulesViolated, false);
    }

    private void logNonFatalViolationMessages(List<PolicyViolationInfo> otherRulesViolated) {
        if (otherRulesViolated.isEmpty()) {
            return;
        }
        logger.debug("Other:");
        logViolationMessages(otherRulesViolated, true);
    }

    private void logViolationMessages(List<PolicyViolationInfo> ruleViolationsToLog, boolean logAtDebugLevel) {
        for (PolicyViolationInfo ruleViolation : ruleViolationsToLog) {
            logMessagesForPolicyViolation(ruleViolation.getProjectVersionComponentVersionView(), ruleViolation.getComponentPolicyRulesView(), logAtDebugLevel);
        }
    }

    private void logMessagesForPolicyViolation(
        ProjectVersionComponentVersionView projectVersionComponentView,
        ComponentPolicyRulesView componentPolicyRulesView,
        boolean logAtDebugLevel
    ) {
        LogAction loggerHelper = logAtDebugLevel ? logger::debug : logger::info;
        String componentId = projectVersionComponentView.getComponentName();
        if (StringUtils.isNotBlank(projectVersionComponentView.getComponentVersionName())) {
            componentId += ":" + projectVersionComponentView.getComponentVersionName();
        }

        String policyRuleComponentVersionSuffix = ".";
        if (StringUtils.isNotBlank(projectVersionComponentView.getComponentVersion())) {
            policyRuleComponentVersionSuffix = String.format(" (%s).", projectVersionComponentView.getComponentVersion());
        }
        loggerHelper.logAtAppropriateLevel(String.format(
            "Policy rule \"%s\" was violated by component \"%s\"%s",
            componentPolicyRulesView.getName(),
            componentId,
            policyRuleComponentVersionSuffix
        ));

        String policyRuleSuffix = ".";
        if (StringUtils.isNotBlank(componentPolicyRulesView.getDescription())) {
            policyRuleSuffix = String.format(" with description: %s", componentPolicyRulesView.getDescription());
        }

        loggerHelper.logAtAppropriateLevel(String.format(
            "Policy rule \"%s\" has severity type: %s%s",
            componentPolicyRulesView.getName(),
            componentPolicyRulesView.getSeverity().prettyPrint(),
            policyRuleSuffix
        ));
    }


    private boolean areAnyPolicySeveritiesViolated(PolicyStatusDescription policyStatusDescription) {
     return Arrays.stream(PolicyRuleSeverityType.values())
         .map(policyStatusDescription::getCountOfSeverity)
         .anyMatch(count -> count > 0);
    }

    // OLD METHOD
    private boolean areFatalPolicySeveritiesViolated(PolicyStatusDescription policyStatusDescription, List<PolicyRuleSeverityType> fatalPolicySeverities) {
        return fatalPolicySeverities.stream()
                .map(policyStatusDescription::getCountOfSeverity)
                .anyMatch(severityCount -> severityCount > 0);
    }


    private static class AllPolicyViolations {
        public final List<PolicyViolationInfo> fatalViolations;
        public final List<PolicyViolationInfo> otherViolations;

        public AllPolicyViolations(List<PolicyViolationInfo> fatalViolations, List<PolicyViolationInfo> otherViolations) {
            this.fatalViolations = fatalViolations;
            this.otherViolations = otherViolations;
        }
    }

    @FunctionalInterface
    private interface LogAction {
        void logAtAppropriateLevel(String message);
    }
}
