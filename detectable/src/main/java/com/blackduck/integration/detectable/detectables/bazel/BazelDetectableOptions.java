package com.blackduck.integration.detectable.detectables.bazel;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.blackduck.integration.detectable.detectables.bazel.v2.BazelEnvironmentAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BazelDetectableOptions {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String targetName;
    private final Set<WorkspaceRule> workspaceRulesFromProperty;
    private final List<String> bazelCqueryAdditionalOptions;
    private final String modeOverride;
    private final int httpProbeLimit;

    public BazelDetectableOptions(
        String targetName,
        Set<WorkspaceRule> workspaceRulesFromProperty,
        List<String> bazelCqueryAdditionalOptions,
        String modeOverride,
        int httpProbeLimit
    ) {
        this.targetName = targetName;
        this.workspaceRulesFromProperty = workspaceRulesFromProperty;
        this.bazelCqueryAdditionalOptions = bazelCqueryAdditionalOptions;
        this.modeOverride = modeOverride;
        this.httpProbeLimit = httpProbeLimit;
    }

    public Optional<String> getTargetName() {
        return Optional.ofNullable(targetName);
    }

    public List<String> getBazelCqueryAdditionalOptions() {
        return bazelCqueryAdditionalOptions;
    }

    public Set<WorkspaceRule> getWorkspaceRulesFromProperty() {
        return workspaceRulesFromProperty;
    }

    public int getHttpProbeLimit() {
        return httpProbeLimit;
    }

    public Optional<BazelEnvironmentAnalyzer.Mode> getModeOverride() {
        if (modeOverride == null || modeOverride.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            BazelEnvironmentAnalyzer.Mode mode = BazelEnvironmentAnalyzer.Mode.valueOf(modeOverride.trim().toUpperCase());
            return Optional.of(mode);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid Bazel mode override value '{}'. Valid values: WORKSPACE, BZLMOD, UNKNOWN. Falling back to auto-detection.", modeOverride);
            return Optional.empty();
        }
    }
}
