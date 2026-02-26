package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import java.util.List;
import java.util.Set;
import java.util.Collections;

import com.blackduck.integration.detectable.detectables.bazel.BazelDetectableOptions;
import com.blackduck.integration.detectable.detectables.bazel.DependencySource;
import com.blackduck.integration.detectable.detectables.bazel.WorkspaceRule;

/**
 * Test-only builder to make creating BazelDetectableOptions in tests more readable.
 */
public final class BazelDetectableOptionsTestBuilder {
    private String targetName = null;
    private Set<DependencySource> dependencySourcesFromProperty = Collections.emptySet();
    private List<String> bazelCqueryAdditionalOptions = Collections.emptyList();
    private List<String> bazelQueryAdditionalOptions = Collections.emptyList();
    private String modeOverride = null;
    private Set<WorkspaceRule> workspaceRulesFromProperty = Collections.emptySet();

    private BazelDetectableOptionsTestBuilder() {
    }

    public static BazelDetectableOptionsTestBuilder builder() {
        return new BazelDetectableOptionsTestBuilder();
    }

    public BazelDetectableOptionsTestBuilder target(String targetName) {
        this.targetName = targetName;
        return this;
    }

    public BazelDetectableOptionsTestBuilder dependencySources(Set<DependencySource> dependencySourcesFromProperty) {
        this.dependencySourcesFromProperty = dependencySourcesFromProperty;
        return this;
    }

    public BazelDetectableOptionsTestBuilder workspaceRules(Set<WorkspaceRule> workspaceRulesFromProperty) {
        this.workspaceRulesFromProperty = workspaceRulesFromProperty;
        return this;
    }

    public BazelDetectableOptionsTestBuilder bazelCqueryAdditionalOptions(List<String> bazelCqueryAdditionalOptions) {
        this.bazelCqueryAdditionalOptions = bazelCqueryAdditionalOptions;
        return this;
    }

    public BazelDetectableOptionsTestBuilder bazelQueryAdditionalOptions(List<String> bazelQueryAdditionalOptions) {
        this.bazelQueryAdditionalOptions = bazelQueryAdditionalOptions;
        return this;
    }

    public BazelDetectableOptionsTestBuilder modeOverride(String modeOverride) {
        this.modeOverride = modeOverride;
        return this;
    }

    public BazelDetectableOptions build() {
        return new BazelDetectableOptions(
            targetName,
            dependencySourcesFromProperty,
            bazelCqueryAdditionalOptions,
            bazelQueryAdditionalOptions,
            modeOverride,
            workspaceRulesFromProperty
        );
    }

    // Convenience factory for a common pattern
    public static BazelDetectableOptions forTarget(String target) {
        return builder().target(target).build();
    }
}
