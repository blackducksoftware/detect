package com.blackduck.integration.detectable.detectables.bazel.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(BazelInfrastructureModules.class);

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
     * Patterns support glob wildcards and alternation groups, e.g. {@code rules_(cc|shell|python)}.
     */
    public static boolean isExcluded(String name, List<String> userPatterns) {
        if (isHardExcluded(name)) {
            return true;
        }
        if (userPatterns == null || userPatterns.isEmpty()) {
            return false;
        }
        for (String pattern : userPatterns) {
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }
            for (String expanded : expandPattern(pattern)) {
                if (FilenameUtils.wildcardMatch(name, expanded)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Expands alternation groups in a pattern so that {@code rules_(cc|shell|python)} becomes
     * {@code ["rules_cc", "rules_shell", "rules_python"]}. Handles multiple and nested groups.
     * A malformed pattern with an unmatched {@code (} is returned as-is with a WARN.
     */
    public static List<String> expandPattern(String pattern) {
        int open = pattern.indexOf('(');
        if (open < 0) {
            return Collections.singletonList(pattern);
        }
        int close = findMatchingClose(pattern, open);
        if (close < 0) {
            logger.warn("detect.bazel.modules.excluded: pattern '{}' has an unmatched '(' — it will not match any module. " +
                "Use glob wildcards (*) or alternation groups with matching parentheses, e.g. rules_(cc|shell).", pattern);
            return Collections.singletonList(pattern);
        }
        String prefix = pattern.substring(0, open);
        String group  = pattern.substring(open + 1, close);
        String suffix = pattern.substring(close + 1);
        List<String> result = new ArrayList<>();
        for (String alt : splitTopLevelAlternatives(group)) {
            result.addAll(expandPattern(prefix + alt + suffix));
        }
        return result;
    }

    // Finds the ) that closes the ( at openPos, respecting nested groups.
    private static int findMatchingClose(String pattern, int openPos) {
        int depth = 0;
        for (int i = openPos; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    // Splits on | only at the top level (depth 0), so (cc|shell)|python splits into
    // ["(cc|shell)", "python"] rather than ["(cc", "shell)", "python"].
    private static List<String> splitTopLevelAlternatives(String group) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < group.length(); i++) {
            char c = group.charAt(i);
            if (c == '(')      depth++;
            else if (c == ')') depth--;
            else if (c == '|' && depth == 0) {
                parts.add(group.substring(start, i).trim());
                start = i + 1;
            }
        }
        parts.add(group.substring(start).trim());
        return parts;
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
                    for (String expanded : expandPattern(pattern.trim())) {
                        alternatives.add(globToRegexAlternative(expanded));
                    }
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
            // Allow an optional Bazel canonical suffix (+/~) so that "protobuf" matches both
            // "protobuf" and the canonical form "protobuf+" or "protobuf~" that appears in
            // bazel query output labels.
            sb.append("[+~]?$");
        }
        return sb.toString();
    }
}
