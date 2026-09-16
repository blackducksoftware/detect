package com.blackduck.integration.detectable.detectables.bazel.v2;

import java.util.Collections;
import java.util.List;

/**
 * Immutable bundle of cross-cutting settings shared by the Bazel V2 extraction classes
 * ({@link com.blackduck.integration.detectable.detectables.bazel.pipeline.Pipelines},
 * {@link HttpFamilyProber}, {@link BazelGraphProber}, {@link BzlmodBcrExtractor}).
 *
 * <p>Centralizing these settings in one object keeps the extraction classes' constructors
 * stable as new Bazel-level settings are introduced.
 *
 * <p>Note: the Bazel target itself is intentionally NOT part of this object — it identifies
 * the specific thing being scanned for a given extraction, not a cross-cutting setting, so it
 * remains a separate parameter on the classes that need it.
 */
public final class BazelExtractionOptions {
    private final BazelEnvironmentAnalyzer.Mode mode;
    private final List<String> cqueryOptions;
    private final List<String> queryOptions;
    private final BazelVersion bazelVersion;

    private BazelExtractionOptions(BazelEnvironmentAnalyzer.Mode mode, List<String> cqueryOptions, List<String> queryOptions, BazelVersion bazelVersion) {
        this.mode = mode;
        this.cqueryOptions = cqueryOptions;
        this.queryOptions = queryOptions;
        this.bazelVersion = bazelVersion;
    }

    public BazelEnvironmentAnalyzer.Mode getMode() {
        return mode;
    }

    public List<String> getCqueryOptions() {
        return cqueryOptions;
    }

    public List<String> getQueryOptions() {
        return queryOptions;
    }

    /**
     * Detected Bazel version; may be {@code null} if unknown (treat as pre-7.1).
     */
    public BazelVersion getBazelVersion() {
        return bazelVersion;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private BazelEnvironmentAnalyzer.Mode mode = BazelEnvironmentAnalyzer.Mode.UNKNOWN;
        private List<String> cqueryOptions = Collections.emptyList();
        private List<String> queryOptions = Collections.emptyList();
        private BazelVersion bazelVersion = null;

        public Builder mode(BazelEnvironmentAnalyzer.Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder cqueryOptions(List<String> cqueryOptions) {
            this.cqueryOptions = cqueryOptions != null ? cqueryOptions : Collections.emptyList();
            return this;
        }

        public Builder queryOptions(List<String> queryOptions) {
            this.queryOptions = queryOptions != null ? queryOptions : Collections.emptyList();
            return this;
        }

        public Builder bazelVersion(BazelVersion bazelVersion) {
            this.bazelVersion = bazelVersion;
            return this;
        }

        public BazelExtractionOptions build() {
            return new BazelExtractionOptions(mode, cqueryOptions, queryOptions, bazelVersion);
        }
    }
}

