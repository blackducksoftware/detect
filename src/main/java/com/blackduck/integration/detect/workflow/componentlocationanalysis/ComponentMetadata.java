package com.blackduck.integration.detect.workflow.componentlocationanalysis;

import com.blackduck.integration.blackduck.api.generated.component.*;
import com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView;

import java.util.List;

/**
 * Corresponds to the data Detect chooses to include from Rapid/Stateless Detector scan results when generating the
 * component location analysis file to aid in vulnerability remediation.
 */
public class ComponentMetadata {
    private final DeveloperScansScanView scanView;

    public ComponentMetadata(DeveloperScansScanView scanView) {
        this.scanView = scanView;
    }

    public List<DeveloperScansScanItemsComponentViolatingPoliciesView> getComponentViolatingPolicies() {
        return scanView.getComponentViolatingPolicies();
    }

    public List<DeveloperScansScanItemsPolicyViolationVulnerabilitiesView> getPolicyViolationVulnerabilities() {
        return scanView.getPolicyViolationVulnerabilities();
    }

    public Object getLongTermUpgradeGuidance() {
        return scanView.getLongTermUpgradeGuidance();
    }

    public Object getShortTermUpgradeGuidance() {
        return scanView.getShortTermUpgradeGuidance();
    }

    public Object getTransitiveUpgradeGuidance() {
        return scanView.getTransitiveUpgradeGuidance();
    }

    public Object getDependencyTrees() {
        return scanView.getDependencyTrees();
    }
}
