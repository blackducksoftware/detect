package com.blackduck.integration.detect.workflow.blackduck.integratedmatching.model;

import com.blackduck.integration.util.Stringable;

public class ScanCountsPayload extends Stringable {
    private final ScanCounts scanCounts;

    public ScanCountsPayload(ScanCounts scanCounts) {
        this.scanCounts = scanCounts;
    }

    public ScanCounts getScanCounts() {
        return scanCounts;
    }
    
    public boolean isValid() {
        if (scanCounts.getBinary() > 0 || scanCounts.getPackageManager() > 0 || scanCounts.getSignature() > 0) {
            return true;
        }
        return false;
    }
}
