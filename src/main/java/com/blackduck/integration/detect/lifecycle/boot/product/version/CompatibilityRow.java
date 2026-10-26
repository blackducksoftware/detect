package com.blackduck.integration.detect.lifecycle.boot.product.version;

import java.util.Collections;
import java.util.List;

public class CompatibilityRow {
    private String blackDuckScaRelease;
    private List<String> detectVersions = Collections.emptyList();

    public String getBlackDuckScaRelease() {
        return blackDuckScaRelease;
    }

    public List<String> getDetectVersions() {
        return detectVersions;
    }
}
