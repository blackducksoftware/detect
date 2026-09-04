package com.blackduck.integration.detectable.detectables.bazel.v2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Single source of truth for Bazel "infrastructure" repo/module name prefixes — toolchain repos,
 * build-rule rulesets, and Maven-plumbing repos that are NOT shipped software components and must
 * be excluded from every BOM path.
 *
 * <p>Previously this list was duplicated in three places with slight drift:
 * <ul>
 *   <li>{@code HttpFamilyProber.EXCLUDED_REPO_PREFIXES}</li>
 *   <li>{@code BzlmodBcrExtractor.EXCLUDED_REPO_PREFIXES}</li>
 *   <li>{@code Pipelines.EXCLUDE_BUILTINS_REGEX}</li>
 * </ul>
 * Consolidating them here prevents the "someone adds a ruleset to two of three copies" bug.
 *
 * <p>{@code rules_shell} is included: Bazel 9 implicitly injects it into every Java target as a
 * build toolchain module, so it must be excluded consistently across all paths.
 */
public final class BazelInfrastructureModules {

    private BazelInfrastructureModules() {
        throw new IllegalStateException("Utility class - do not instantiate");
    }

    private static final List<String> INFRASTRUCTURE_PREFIXES = Collections.unmodifiableList(Arrays.asList(
        "bazel_tools",
        "local_config_",
        "remotejdk",
        "platforms",
        "rules_python",
        "rules_java",
        "rules_cc",
        "rules_shell",
        "maven",
        "unpinned_maven",
        "rules_jvm_external"
    ));

    /**
     * Returns the immutable list of infrastructure name prefixes.
     */
    public static List<String> prefixes() {
        return INFRASTRUCTURE_PREFIXES;
    }

    /**
     * Returns true if {@code name} starts with any infrastructure prefix (i.e. is build
     * infrastructure, not a shipped software component).
     */
    public static boolean isInfrastructure(String name) {
        if (name == null) {
            return false;
        }
        for (String prefix : INFRASTRUCTURE_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the negative-lookahead regex used by the HTTP_ARCHIVE pipeline's line filter to drop
     * infrastructure repos. Equivalent to the previously hardcoded {@code EXCLUDE_BUILTINS_REGEX}.
     */
    public static String exclusionLookaheadRegex() {
        return "^(?!(" + String.join("|", INFRASTRUCTURE_PREFIXES) + ")).*$";
    }
}

