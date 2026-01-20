package com.blackduck.integration.detect.workflow.result;

import java.util.Collections;
import java.util.List;

public class QuackPatchResult implements DetectResult {

    @Override
    public String getResultLocation() {
        return "";
    }

    @Override
    public String getResultMessage() {
        return "";
    }

    @Override
    public List<String> getResultSubMessages() {
        return List.of();
    }

    @Override
    public List<String> getTransitiveUpgradeGuidanceSubMessages() {
        return Collections.emptyList();
    }
}
