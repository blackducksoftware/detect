package com.blackduck.integration.detect.workflow.result;

import java.util.Collections;
import java.util.List;

public class BlackDuckBomDetectResult implements DetectResult {
    private final String projectComponentsLink;

    public BlackDuckBomDetectResult(String projectComponentsLink) {
        this.projectComponentsLink = projectComponentsLink;
    }

    @Override
    public String getResultLocation() {
        return projectComponentsLink;
    }

    @Override
    public String getResultMessage() {
        return String.format("Black Duck SCA Project BOM: %s", projectComponentsLink);
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
