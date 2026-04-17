package com.blackduck.integration.detect.workflow.blackduck.settings;

import java.util.List;

import com.blackduck.integration.blackduck.api.core.BlackDuckResponse;

public class DetectPropertiesSetting extends BlackDuckResponse {
    private final boolean isCorrelatedScanningEnabled;
    private final List<String> correlatedScanningScanTypes;

    public DetectPropertiesSetting(boolean isCorrelatedScanningEnabled, List<String> correlatedScanningScanTypes) {
        this.isCorrelatedScanningEnabled = isCorrelatedScanningEnabled;
        this.correlatedScanningScanTypes = correlatedScanningScanTypes;
    }

    public boolean isCorrelatedScanningEnabled() {
        return isCorrelatedScanningEnabled;
    }

    public List<String> getCorrelatedScanningScanTypes() {
        return correlatedScanningScanTypes;
    }
}
