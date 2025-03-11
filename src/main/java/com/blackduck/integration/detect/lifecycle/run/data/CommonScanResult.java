package com.blackduck.integration.detect.lifecycle.run.data;

import java.util.UUID;

public class CommonScanResult {
    private final UUID scanId;
    private final String codeLocationName;

    public CommonScanResult(UUID scanId, String codeLocationName) {
        this.scanId = scanId;
        this.codeLocationName = codeLocationName;
    }

    public UUID getScanId() {
        return scanId;
    }

    public String getCodeLocationName() {
        return codeLocationName;
    }
}
