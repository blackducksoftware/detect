package com.blackduck.integration.detect.workflow.result;

import java.util.Collections;
import java.util.List;

public class QuackPatchResult implements DetectResult {
    private final String quackPatchOutputDir;


    public QuackPatchResult(String quackPatchOutputDir) { this.quackPatchOutputDir = quackPatchOutputDir; }
    @Override
    public String getResultLocation() {
        return quackPatchOutputDir;
    }

    @Override
    public String getResultMessage() {
        return String.format("Quack Patch results directory: %s", quackPatchOutputDir);
    }

    @Override
    public List<String> getResultSubMessages() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getTransitiveUpgradeGuidanceSubMessages() {
        return Collections.emptyList();
    }
}
