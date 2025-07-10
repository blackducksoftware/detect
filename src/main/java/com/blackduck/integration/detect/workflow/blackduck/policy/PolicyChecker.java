package com.blackduck.integration.detect.workflow.blackduck.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        Optional<List<ProjectVersionPolicyRulesView>> activePolicyRulesOptional = projectBomService.getActivePoliciesForVersion(projectVersionView);

        if (activePolicyRulesOptional.isPresent()) {
            List<ProjectVersionPolicyRulesView> activePolicyRules = activePolicyRulesOptional.get();

            List<String> violatedPolicyNames = activePolicyRules.stream()
                .filter(rule -> ProjectVersionComponentPolicyStatusType.IN_VIOLATION.equals(rule.getStatus()))
                .map(ProjectVersionPolicyRulesView::getName)
                .filter(policyNamesToFailPolicyCheck::contains)
                .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(violatedPolicyNames)) {
                List<PolicyViolationInfo> fatalRulesViolated = collectFatalRulesViolated(projectVersionView, severity -> true);
                logViolationMessages(fatalRulesViolated);
                String violationReason = StringUtils.join(violatedPolicyNames, ", ");
                exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_POLICY_VIOLATION, "Violated policy by names: " + violationReason); // lost in translation
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
        Optional<PolicyStatusDescription> policyStatusDescriptionOptional = fetchPolicyStatusDescription(projectVersionView);

        if (policyStatusDescriptionOptional.isPresent()) {
            PolicyStatusDescription policyStatusDescription = policyStatusDescriptionOptional.get();
            logger.info(policyStatusDescription.getPolicyStatusMessage());
            List<PolicyViolationInfo> fatalRulesViolated = collectFatalRulesViolated(projectVersionView, severitiesToFailPolicyCheck::contains);
            if (!fatalRulesViolated.isEmpty()) {
                logViolationMessages(fatalRulesViolated);
            }
            
            boolean policySeveritiesAreViolated = arePolicySeveritiesViolated(policyStatusDescription, severitiesToFailPolicyCheck);

            // If Black Duck has reported policy violations in status description (policySeveritiesAreViolated),
            // or we have noticed violations while examining components in the BOM (fatalRulesViolated),
            // fail the scan.
            if (policySeveritiesAreViolated || !fatalRulesViolated.isEmpty()) {
                exitCodePublisher.publishExitCode(ExitCodeType.FAILURE_POLICY_VIOLATION, "Detect found policy violations."); // keep severity case same as before
            }
        } else {
            String availableLinks = StringUtils.join(projectVersionView.getAvailableLinks(), ", ");
            logger.warn(String.format(
                "It is not possible to check the active policy rules for this project/version. The active-policy-rules link must be present. The available links are: %s",
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

    private List<PolicyViolationInfo> collectFatalRulesViolated(ProjectVersionView projectVersionView, Predicate<PolicyRuleSeverityType> violationIsFatalCheck)
        throws IntegrationException {

        List<PolicyViolationInfo> fatalRulesViolated = new ArrayList<>();
        logger.info("Searching BOM for components in violation of fatal policy rules.");

        List<ProjectVersionComponentVersionView> bomComponents = projectBomService.getComponentsForProjectVersion(projectVersionView);
        for (ProjectVersionComponentVersionView projectVersionComponentView : bomComponents) {
            if (!projectVersionComponentView.getPolicyStatus().equals(ProjectVersionComponentPolicyStatusType.IN_VIOLATION)) {
                continue;
            }
            for (ComponentPolicyRulesView componentPolicyRulesView : blackDuckApiClient.getAllResponses(projectVersionComponentView.metaPolicyRulesLink())) {
                if (componentPolicyRulesView.getPolicyApprovalStatus().equals(ProjectVersionComponentPolicyStatusType.IN_VIOLATION)
                    && (violationIsFatalCheck.test(componentPolicyRulesView.getSeverity()))) {
                    fatalRulesViolated.add(new PolicyViolationInfo(projectVersionComponentView, componentPolicyRulesView));
                }
            }
        }
        return fatalRulesViolated;
    }

    private void logViolationMessages(List<PolicyViolationInfo> ruleViolationsToLog) {
        for (PolicyViolationInfo ruleViolation : ruleViolationsToLog) {
            logMessagesForPolicyViolation(ruleViolation.getProjectVersionComponentVersionView(), ruleViolation.getComponentPolicyRulesView());
        }
    }

    private void logMessagesForPolicyViolation(
        ProjectVersionComponentVersionView projectVersionComponentView,
        ComponentPolicyRulesView componentPolicyRulesView
    ) {
        String componentId = projectVersionComponentView.getComponentName();
        if (StringUtils.isNotBlank(projectVersionComponentView.getComponentVersionName())) {
            componentId += ":" + projectVersionComponentView.getComponentVersionName();
        }

        String policyRuleComponentVersionSuffix = ".";
        if (StringUtils.isNotBlank(projectVersionComponentView.getComponentVersion())) {
            policyRuleComponentVersionSuffix = String.format(" (%s).", projectVersionComponentView.getComponentVersion());
        }
        logger.info(String.format(
            "Policy rule \"%s\" was violated by component \"%s\"%s",
            componentPolicyRulesView.getName(),
            componentId,
            policyRuleComponentVersionSuffix
        ));

        String policyRuleSuffix = ".";
        if (StringUtils.isNotBlank(componentPolicyRulesView.getDescription())) {
            policyRuleSuffix = String.format(" with description: %s", componentPolicyRulesView.getDescription());
        }

        logger.info(String.format(
            "Policy rule \"%s\" has a fatal severity type: %s%s",
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
}
