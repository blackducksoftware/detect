package com.blackduck.integration.detectable.detectables.bazel.functional.bazel.pipeline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.bazel.query.BazelCommandArguments;

/**
 * Verifies the module-extension sub-repo exclusion regex used by the bzlmod HTTP Archive pipeline.
 *
 * The regex is built in Pipelines.java as:
 *   "^(?!.*" + Pattern.quote(BazelCommandArguments.MODULE_EXTENSION_MARKER) + ").*$"
 *
 * Regression test for: StringIndexOutOfBoundsException "Range [0, -1) out of bounds for length 9"
 * caused by rules_rust module-extension sub-repos (e.g. rules_rs++crate+...) reaching
 * GithubUrlParser.parseRepo() with a bare .git clone URL.
 */
class PipelinesModuleExtensionFilterTest {

    // Reproduce the same regex that Pipelines.java builds for the bzlmod pipeline filter step.
    private static final String FILTER_REGEX =
        "^(?!.*" + Pattern.quote(BazelCommandArguments.MODULE_EXTENSION_MARKER) + ").*$";

    @Test
    void moduleExtensionSubRepos_doNotMatchKeepRegex() {
        // These names must be EXCLUDED (must NOT match the keep-regex).
        // The triggering case from the customer bug report (Bazel 8.6.0 / rules_rust):
        assertFalse("rules_rs++crate+crates_zenoh_c__github.com_eclipse-zenoh_zenoh.git_81c6c933".matches(FILTER_REGEX));
        // Other representative module-extension sub-repo names:
        assertFalse("rules_jvm_external++maven+guava".matches(FILTER_REGEX));
        assertFalse("gazelle++_go_repos+org_golang_x_net".matches(FILTER_REGEX));
        assertFalse("rules_ros2++pip+some_package".matches(FILTER_REGEX));
    }

    @Test
    void normalRepos_matchKeepRegex() {
        // Standard BCR module names must pass through (must match the keep-regex).
        assertTrue("abseil-cpp".matches(FILTER_REGEX));
        assertTrue("protobuf".matches(FILTER_REGEX));
        assertTrue("rules_cc".matches(FILTER_REGEX));
        assertTrue("rules_rust".matches(FILTER_REGEX));
    }

    @Test
    void canonicalSuffixAlone_doesNotTriggerExclusion() {
        // A single trailing "+" (Bazel canonical version suffix) must NOT be excluded.
        // Only the double "++" module-extension marker triggers exclusion.
        assertTrue("protobuf+".matches(FILTER_REGEX));
        assertTrue("rules_rust+".matches(FILTER_REGEX));
        assertTrue("abseil-cpp~".matches(FILTER_REGEX));
    }
}
