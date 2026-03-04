package com.blackduck.integration.detect.workflow.componentlocationanalysis;

import com.blackduck.integration.blackduck.api.generated.component.*;
import com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView;

import java.util.List;

/**
 * Exposes desired fields from the Rapid/Stateless scan results {@link com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView}
 * for inclusion in the input file for component location analysis.
 */
public class ComponentMetadata {
    private List<DeveloperScansScanItemsComponentViolatingPoliciesView> componentViolatingPolicies;
    private List<DeveloperScansScanItemsPolicyViolationVulnerabilitiesView> policyViolationVulnerabilities;
    private DeveloperScansScanItemsLongTermUpgradeGuidanceView longTermUpgradeGuidance;
    private DeveloperScansScanItemsShortTermUpgradeGuidanceView shortTermUpgradeGuidance;
    private List<DeveloperScansScanItemsTransitiveUpgradeGuidanceView> transitiveUpgradeGuidance;
    private List<List<String>> dependencyTrees;

    public ComponentMetadata(DeveloperScansScanView scanView) {
        this.componentViolatingPolicies = scanView.getComponentViolatingPolicies();
        this.policyViolationVulnerabilities = scanView.getPolicyViolationVulnerabilities();
        this.longTermUpgradeGuidance = scanView.getLongTermUpgradeGuidance();
        this.shortTermUpgradeGuidance = scanView.getShortTermUpgradeGuidance();
        this.transitiveUpgradeGuidance = scanView.getTransitiveUpgradeGuidance();
        this.dependencyTrees = scanView.getDependencyTrees();
    }
}
