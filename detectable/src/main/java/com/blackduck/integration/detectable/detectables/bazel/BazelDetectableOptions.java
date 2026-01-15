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
    private final String eraOverride;

    public BazelDetectableOptions(
        String targetName,
        Set<WorkspaceRule> workspaceRulesFromProperty,
        List<String> bazelCqueryAdditionalOptions,
        String eraOverride
    ) {
        this.targetName = targetName;
        this.workspaceRulesFromProperty = workspaceRulesFromProperty;
        this.bazelCqueryAdditionalOptions = bazelCqueryAdditionalOptions;
        this.eraOverride = eraOverride;
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

    public Optional<BazelEnvironmentAnalyzer.Era> getEraOverride() {
        if (eraOverride == null || eraOverride.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            BazelEnvironmentAnalyzer.Era era = BazelEnvironmentAnalyzer.Era.valueOf(eraOverride.trim().toUpperCase());
            return Optional.of(era);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid Bazel era override value '{}'. Valid values: LEGACY, BZLMOD, UNKNOWN. Falling back to auto-detection.", eraOverride);
            return Optional.empty();
        }
    }
}
