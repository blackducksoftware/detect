package com.blackduck.integration.detect.lifecycle.run.data;

import java.util.UUID;

public class CommonScanResult {
    private final UUID scanId;
    private final String codeLocationName;
    private boolean isPackageManagerScassPossible = true;

    public CommonScanResult(UUID scanId, String codeLocationName, boolean isSCASSPossible) {
        this.scanId = scanId;
        this.codeLocationName = codeLocationName;
        this.isPackageManagerScassPossible = isSCASSPossible;
    }

    public UUID getScanId() {
        return scanId;
    }

    public String getCodeLocationName() {
        return codeLocationName;
    }

    public boolean isPackageManagerScassPossible() { return isPackageManagerScassPossible; }
}
