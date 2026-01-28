package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import java.util.List;
import java.util.Set;

import com.blackduck.integration.detectable.detectables.bazel.BazelDetectableOptions;
import com.blackduck.integration.detectable.detectables.bazel.DependencySource;

/**
 * Test-only builder to make creating BazelDetectableOptions in tests more readable.
 */
public final class BazelDetectableOptionsTestBuilder {
    private String targetName = null;
    private Set<DependencySource> dependencySourcesFromProperty = null;
    private List<String> bazelCqueryAdditionalOptions = null;
    private String modeOverride = null;
    private int httpProbeLimit = 30; // sensible default used across tests

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

    public BazelDetectableOptionsTestBuilder bazelCqueryAdditionalOptions(List<String> bazelCqueryAdditionalOptions) {
        this.bazelCqueryAdditionalOptions = bazelCqueryAdditionalOptions;
        return this;
    }

    public BazelDetectableOptionsTestBuilder modeOverride(String modeOverride) {
        this.modeOverride = modeOverride;
        return this;
    }

    public BazelDetectableOptionsTestBuilder httpProbeLimit(int httpProbeLimit) {
        this.httpProbeLimit = httpProbeLimit;
        return this;
    }

    public BazelDetectableOptions build() {
        return new BazelDetectableOptions(
            targetName,
            dependencySourcesFromProperty,
            bazelCqueryAdditionalOptions,
            modeOverride,
            httpProbeLimit
        );
    }

    // Convenience factory for a common pattern
    public static BazelDetectableOptions forTarget(String target) {
        return builder().target(target).build();
    }
}

