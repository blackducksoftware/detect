package com.blackduck.integration.detectable.detectables.bazel.v2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

/**
 * Two-tier Bazel module exclusion:
 * <ul>
 *   <li>Hard-coded: always excluded, not overridable (synthetic/generated repos with no SCA identity).</li>
 *   <li>User-configured: {@code detect.bazel.modules.excluded} patterns appended on top.</li>
 * </ul>
 * All matching uses glob wildcards — {@code *} matches any sequence of characters.
 * Exact patterns match only the full name ({@code maven} does not exclude {@code maven-utils}).
 */
public final class BazelInfrastructureModules {

    private BazelInfrastructureModules() {
        throw new IllegalStateException("Utility class - do not instantiate");
    }

    // Always excluded — auto-generated, machine-local, or synthetic repos that have no stable
    // source URL and cause show_repo batch failures when included (e.g. platforms on Bazel 7.x).
    private static final List<String> HARD_EXCLUDED_PATTERNS = Collections.unmodifiableList(Arrays.asList(
        "bazel_tools",
        "local_config_*",   // auto-generated machine-local config repos (local_config_cc, etc.)
        "remotejdk*",       // JDK toolchain repos injected by Bazel
        "platforms",        // Bazel constraint infrastructure; no-suffix label breaks show_repo batching
        "maven",            // synthetic internal repo from rules_jvm_external; handled by MAVEN_INSTALL pipeline
        "unpinned_maven"    // same as maven
    ));

    /** Returns {@code true} if {@code name} matches a hard-coded exclusion pattern. */
    public static boolean isHardExcluded(String name) {
        if (name == null) {
            return false;
        }
        for (String pattern : HARD_EXCLUDED_PATTERNS) {
            if (FilenameUtils.wildcardMatch(name, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if {@code name} should be excluded — by the hard-coded list or
     * by a user pattern from {@code detect.bazel.modules.excluded}.
     */
    public static boolean isExcluded(String name, List<String> userPatterns) {
        if (isHardExcluded(name)) {
            return true;
        }
        if (userPatterns == null || userPatterns.isEmpty()) {
            return false;
        }
        for (String pattern : userPatterns) {
            if (pattern != null && !pattern.isEmpty() && FilenameUtils.wildcardMatch(name, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds a negative-lookahead regex from the combined hard-coded + user patterns.
     * Used by the HTTP_ARCHIVE bzlmod pipeline's line filter.
     */
    public static String buildCombinedExclusionRegex(List<String> userPatterns) {
        List<String> alternatives = new java.util.ArrayList<>();
        for (String pattern : HARD_EXCLUDED_PATTERNS) {
            alternatives.add(globToRegexAlternative(pattern));
        }
        if (userPatterns != null) {
            for (String pattern : userPatterns) {
                if (pattern != null && !pattern.trim().isEmpty()) {
                    alternatives.add(globToRegexAlternative(pattern.trim()));
                }
            }
        }
        return "^(?!(" + String.join("|", alternatives) + ")).*$";
    }

    // Converts a glob pattern to a regex alternative for use in a negative lookahead.
    // Exact patterns (no *) get a trailing $ so "maven" doesn't match "maven-utils".
    private static String globToRegexAlternative(String glob) {
        StringBuilder sb = new StringBuilder();
        boolean hasWildcard = false;
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                sb.append(".*");
                hasWildcard = true;
            } else if (".+?[](){}^$|\\".indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        if (!hasWildcard) {
            sb.append('$');
        }
        return sb.toString();
    }
}
