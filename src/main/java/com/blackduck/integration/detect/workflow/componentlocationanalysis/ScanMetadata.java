package com.blackduck.integration.detect.workflow.componentlocationanalysis;

import com.blackduck.integration.blackduck.api.generated.component.*;

import java.util.List;

/**
 * Corresponds to the data Detect chooses to include from Rapid/Stateless Detector scan results when generating the
 * component location analysis file to aid in vulnerability remediation.
 */
public class ScanMetadata {
    private List<DeveloperScansScanItemsPolicyViolationVulnerabilitiesView> policyViolationVulnerabilities;
    private DeveloperScansScanItemsShortTermUpgradeGuidanceView shortTermUpgradeGuidance;
    private DeveloperScansScanItemsLongTermUpgradeGuidanceView longTermUpgradeGuidance;
    private List<DeveloperScansScanItemsTransitiveUpgradeGuidanceView> transitiveUpgradeGuidance;
    private List<DeveloperScansScanItemsComponentViolatingPoliciesView> componentViolatingPolicies;
    private List<List<String>> dependencyTrees;;

    public void setPolicyViolationVulnerabilities(List<DeveloperScansScanItemsPolicyViolationVulnerabilitiesView> policyViolationVulnerabilities) {
        this.policyViolationVulnerabilities = policyViolationVulnerabilities;
    }

    public void setShortTermUpgradeGuidance(DeveloperScansScanItemsShortTermUpgradeGuidanceView shortTermUpgradeGuidance) {
        this.shortTermUpgradeGuidance = shortTermUpgradeGuidance;
    }

    public void setLongTermUpgradeGuidance(DeveloperScansScanItemsLongTermUpgradeGuidanceView longTermUpgradeGuidance) {
        this.longTermUpgradeGuidance = longTermUpgradeGuidance;
    }

    public void setTransitiveUpgradeGuidance(List<DeveloperScansScanItemsTransitiveUpgradeGuidanceView> transitiveUpgradeGuidance) {
        this.transitiveUpgradeGuidance = transitiveUpgradeGuidance;
    }

    public void setComponentViolatingPolicies(List<DeveloperScansScanItemsComponentViolatingPoliciesView> componentViolatingPolicies) {
        this.componentViolatingPolicies = componentViolatingPolicies;
    }

    public void setDependencyTrees(List<List<String>> dependencyTrees) {
        this.dependencyTrees = dependencyTrees;
    }
}
