package com.blackduck.integration.detect.workflow.blackduck.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        logger.info("Checking for policy violations by name.");
        Optional<List<ProjectVersionPolicyRulesView>> activePolicyRulesOptional = projectBomService.getActivePoliciesForVersion(projectVersionView);

        if (activePolicyRulesOptional.isPresent()) {
            List<ProjectVersionPolicyRulesView> activePolicyRules = activePolicyRulesOptional.get();

            List<String> allViolatedPolicyNames = activePolicyRules.stream()
                    .filter(rule -> ProjectVersionComponentPolicyStatusType.IN_VIOLATION.equals(rule.getStatus()))
                    .map(ProjectVersionPolicyRulesView::getName)
                    .collect(Collectors.toList());

            List<String> fatalViolatedPolicyNames = allViolatedPolicyNames.stream()
                .filter(policyNamesToFailPolicyCheck::contains)
                .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(fatalViolatedPolicyNames)) {
                AllPolicyViolations allComponentsWithViolations = collectComponentsViolatingPolicies(projectVersionView, ComponentPolicyRulesView::getName, name -> fatalViolatedPolicyNames.contains(name));
                logFatalViolationMessages(allComponentsWithViolations.fatalViolations);
                logNonFatalViolationMessages(allComponentsWithViolations.otherViolations);
                String violationReason = StringUtils.join(fatalViolatedPolicyNames, ", ");
                exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_POLICY_VIOLATION, "Detect found policy violations by name. The following policies were violated: " + violationReason);
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
            AllPolicyViolations allComponentsWithViolations = collectComponentsViolatingPolicies(projectVersionView, ComponentPolicyRulesView::getSeverity, severitiesToFailPolicyCheck::contains);

            if (!allComponentsWithViolations.fatalViolations.isEmpty()) {
                logFatalViolationMessages(allComponentsWithViolations.fatalViolations);
            }
            if (!allComponentsWithViolations.otherViolations.isEmpty()) {
                logNonFatalViolationMessages(allComponentsWithViolations.otherViolations);
            }
            
            boolean policySeveritiesAreViolated = arePolicySeveritiesViolated(policyStatusDescription, severitiesToFailPolicyCheck);

            // If Black Duck has reported policy violations in status description (policySeveritiesAreViolated),
            // or we have noticed violations while examining components in the BOM (fatalRulesViolated),
            // fail the scan.
            if (policySeveritiesAreViolated || !allComponentsWithViolations.fatalViolations.isEmpty()) {
                exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_POLICY_VIOLATION, "Detect found policy violations.");
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

    private <T> AllPolicyViolations collectComponentsViolatingPolicies(ProjectVersionView projectVersionView, Function<ComponentPolicyRulesView, T> valueExtractor, Predicate<T> failFilter)
            throws IntegrationException {
        List<PolicyViolationInfo> fatalRulesViolated = new ArrayList<>();
        List<PolicyViolationInfo> otherRulesViolated = new ArrayList<>();
        logger.info("Searching BOM for components in violation of policy rules.");

        List<ProjectVersionComponentVersionView> bomComponents = projectBomService.getComponentsForProjectVersion(projectVersionView);
        for (ProjectVersionComponentVersionView component : bomComponents) {
            if (!component.getPolicyStatus().equals(ProjectVersionComponentPolicyStatusType.IN_VIOLATION)) {
                continue;
            }

            for (ComponentPolicyRulesView rule : blackDuckApiClient.getAllResponses(component.metaPolicyRulesLink())) {
                if (rule.getPolicyApprovalStatus().equals(ProjectVersionComponentPolicyStatusType.IN_VIOLATION)) {
                    PolicyViolationInfo violationInfo = new PolicyViolationInfo(component, rule);
                    if (failFilter.test(valueExtractor.apply(rule))) {
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
        logger.info("Fatal:");
        logViolationMessages(fatalRulesViolated, false);
    }

    private void logNonFatalViolationMessages(List<PolicyViolationInfo> otherRulesViolated) {
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
    
    private boolean arePolicySeveritiesViolated(PolicyStatusDescription policyStatusDescription, List<PolicyRuleSeverityType> policySeverities) {
        return policySeverities.stream()
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
