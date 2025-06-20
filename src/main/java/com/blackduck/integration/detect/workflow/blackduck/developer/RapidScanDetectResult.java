package com.blackduck.integration.detect.workflow.blackduck.developer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.blackduck.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.configuration.enumeration.BlackduckScanMode;
import com.blackduck.integration.detect.workflow.blackduck.developer.aggregate.RapidScanDetailGroup;
import com.blackduck.integration.detect.workflow.blackduck.developer.aggregate.RapidScanResultSummary;
import com.blackduck.integration.detect.workflow.result.DetectResult;

public class RapidScanDetectResult implements DetectResult {
    public static final String NONPERSISTENT_SCAN_RESULT_HEADING = " Scan Result";
    public static final String NONPERSISTENT_SCAN_RESULT_DETAILS_HEADING = " Scan Result Details";
    private final String jsonFilePath;
    private final List<String> subMessages;
    private final List<String> transitiveGuidanceSubMessages;
    public static String scanMode;

    public RapidScanDetectResult(String jsonFilePath, RapidScanResultSummary resultSummary, BlackduckScanMode mode, List<PolicyRuleSeverityType> errorPolicies) {
        this.jsonFilePath = jsonFilePath;
        this.subMessages = createResultMessages(resultSummary, errorPolicies);
        this.transitiveGuidanceSubMessages = createTransitiveGuidanceMessages(resultSummary);
        scanMode = mode.displayName();
    }

    private List<String> createTransitiveGuidanceMessages(RapidScanResultSummary summary) {
        String indentedMessageFormat = "\t\t%s";
        List<String> resultMessages = new LinkedList<>();
        if (summary.getTransitiveGuidances().size() > 0) {
            resultMessages.add("");
            resultMessages.add("\tTransitive upgrade guidance:");
            summary.getTransitiveGuidances().stream().sorted().forEach(component -> resultMessages.add(String.format(indentedMessageFormat, component)));
        }
        return resultMessages;
    }

    @Override
    public String getResultLocation() {
        return jsonFilePath;
    }

    @Override
    public String getResultMessage() {
        return String.format("%s: (for more detail look in the log for %s)", scanMode + NONPERSISTENT_SCAN_RESULT_HEADING, scanMode + NONPERSISTENT_SCAN_RESULT_DETAILS_HEADING);
    }

    @Override
    public List<String> getResultSubMessages() {
        return subMessages;
    }
    

    private List<String> createResultMessages(RapidScanResultSummary summary, List<PolicyRuleSeverityType> errorPolicies) {
        String policyGroupName = RapidScanDetailGroup.POLICY.getDisplayName();
        String securityGroupName = RapidScanDetailGroup.SECURITY.getDisplayName();
        String licenseGroupName = RapidScanDetailGroup.LICENSE.getDisplayName();
        String violatingPoliciesSupersetGroupName = RapidScanDetailGroup.VIOLATING_POLICIES.getDisplayName();
        String countFormat = "\t\t* %s: %d";
        String indentedMessageFormat = "\t\t%s";

        List<String> resultMessages = new LinkedList<>();
        resultMessages.add("");
        addErrorViolationHeader(resultMessages, errorPolicies);
        resultMessages.add(String.format(countFormat, policyGroupName, summary.getPolicyErrorCount()));
        resultMessages.add(String.format(countFormat, securityGroupName, summary.getSecurityErrorCount()));
        resultMessages.add(String.format(countFormat, licenseGroupName, summary.getLicenseErrorCount()));
        resultMessages.add(String.format(countFormat, violatingPoliciesSupersetGroupName, summary.getAllOtherPolicyErrorCount()));
        resultMessages.add("");
        resultMessages.add("\tOther policy violations");
        resultMessages.add(String.format(countFormat, policyGroupName, summary.getPolicyWarningCount()));
        resultMessages.add(String.format(countFormat, securityGroupName, summary.getSecurityWarningCount()));
        resultMessages.add(String.format(countFormat, licenseGroupName, summary.getLicenseWarningCount()));
        resultMessages.add(String.format(countFormat, violatingPoliciesSupersetGroupName, summary.getAllOtherPolicyWarningCount()));
        resultMessages.add("");
        resultMessages.add("\tPolicies Violated:");
        summary.getPolicyViolationNames().stream()
            .sorted()
            .forEach(policy -> resultMessages.add(String.format(indentedMessageFormat, policy)));
        resultMessages.add("");
        resultMessages.add("\tComponents with Policy Violations:");
        summary.getComponentsViolatingPolicy().stream()
            .sorted()
            .forEach(component -> resultMessages.add(String.format(indentedMessageFormat, component)));
        resultMessages.add("");
        resultMessages.add("\tComponents with Policy Violation Warnings:");
        summary.getComponentsViolatingPolicyWarnings().stream()
            .sorted()
            .forEach(component -> resultMessages.add(String.format(indentedMessageFormat, component)));

        return resultMessages;
    }

    private void addErrorViolationHeader(List<String> resultMessages, List<PolicyRuleSeverityType> errorPolicies) {
        if (errorPolicies == null || checkForDefaultPolicies(errorPolicies)) {
            resultMessages.add("\tCritical and blocking policy violations for");
        } else {
            String violationMessage;
            if (errorPolicies.size() == 1) {
                violationMessage = errorPolicies.get(0).name().toLowerCase();
            } else {
                violationMessage = String.join(", ", errorPolicies.subList(0, errorPolicies.size() - 1).stream()
                        .map(policy -> policy.name().toLowerCase())
                        .toArray(String[]::new)) + " and " + errorPolicies.get(errorPolicies.size() - 1).name().toLowerCase();
            }
            resultMessages.add(String.format("\t%s %s policy violations for", DetectProperties.DETECT_STATELESS_POLICY_CHECK_FAIL_ON_SEVERITIES.getKey(), violationMessage));
        }
    }

    private boolean checkForDefaultPolicies(List<PolicyRuleSeverityType> errorPolicies) {
        List<PolicyRuleSeverityType> defaultPolicies = Arrays.asList(PolicyRuleSeverityType.CRITICAL, PolicyRuleSeverityType.BLOCKER);
        
        Set<PolicyRuleSeverityType> errorSet = new HashSet<>(errorPolicies);
        Set<PolicyRuleSeverityType> defaultSet = new HashSet<>(defaultPolicies);
        
        return errorSet.equals(defaultSet);
    }

    @Override
    public List<String> getTransitiveUpgradeGuidanceSubMessages() {
        return this.transitiveGuidanceSubMessages;
    }
}
