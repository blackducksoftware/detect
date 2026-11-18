package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.blackduck.integration.detectable.detectables.bazel.v2.BazelInfrastructureModules;

/**
 * Tests for BazelInfrastructureModules — the two-tier Bazel module exclusion logic.
 *
 * Three code paths are exercised:
 *   isHardExcluded  — hard-coded always-excluded patterns (no user override)
 *   isExcluded      — combines hard list + user patterns; used by BCR extractor and HttpFamilyProber
 *   buildCombinedExclusionRegex — same combined patterns as a regex; used by the HTTP_ARCHIVE pipeline line filter
 *
 * Both isExcluded and buildCombinedExclusionRegex call expandPattern internally, so alternation
 * and canonical-suffix tests are duplicated across both paths intentionally — a break in one
 * path would not be caught by tests for the other.
 */
class BazelInfrastructureModulesTest {

    // -------------------------------------------------------------------------
    // isHardExcluded — always-excluded set, cannot be overridden by user config
    // -------------------------------------------------------------------------

    static Stream<Arguments> hardExcludedCases() {
        return Stream.of(
            // exact matches
            Arguments.of("bazel_tools",          true),
            Arguments.of("platforms",             true),
            Arguments.of("maven",                 true),
            Arguments.of("unpinned_maven",        true),
            // wildcard matches (local_config_*, remotejdk*)
            Arguments.of("local_config_cc",       true),
            Arguments.of("local_config_platform", true),
            Arguments.of("remotejdk17",           true),
            Arguments.of("remotejdk_linux_amd64", true),
            // W2 fix: exact patterns must not act as prefixes
            Arguments.of("maven-utils",           false),
            Arguments.of("bazel_tools_extra",     false),
            Arguments.of("platforms_extra",       false),
            // legitimate BCR modules — must not be hard-excluded
            Arguments.of("protobuf",              false),
            Arguments.of("rules_cc",              false),
            Arguments.of("abseil-cpp",            false),
            Arguments.of("rules_jvm_external",    false)
        );
    }

    @ParameterizedTest(name = "isHardExcluded({0}) = {1}")
    @MethodSource("hardExcludedCases")
    void hardExcluded(String name, boolean expected) {
        assertEquals(expected, BazelInfrastructureModules.isHardExcluded(name));
    }

    @Test
    void hardExcluded_nullReturnsFalse() {
        assertFalse(BazelInfrastructureModules.isHardExcluded(null));
    }

    // -------------------------------------------------------------------------
    // isExcluded — hard list + user patterns; uses FilenameUtils.wildcardMatch
    // -------------------------------------------------------------------------

    static Stream<Arguments> isExcludedCases() {
        List<String> exactPattern    = Collections.singletonList("rules_java");
        List<String> wildcardPattern = Collections.singletonList("rules_*");
        List<String> multiPattern    = Arrays.asList("rules_java", "rules_jvm_external");

        return Stream.of(
            // hard list still active with empty user patterns
            Arguments.of("bazel_tools",        Collections.emptyList(), true),
            Arguments.of("rules_cc",           Collections.emptyList(), false),
            // exact user pattern — W2 fix: must not act as a prefix
            Arguments.of("rules_java",         exactPattern, true),
            Arguments.of("rules_javacc",       exactPattern, false),
            Arguments.of("rules_java_extra",   exactPattern, false),
            // wildcard user pattern
            Arguments.of("rules_cc",           wildcardPattern, true),
            Arguments.of("rules_java",         wildcardPattern, true),
            Arguments.of("rules_python",       wildcardPattern, true),
            Arguments.of("protobuf",           wildcardPattern, false),
            // multiple patterns
            Arguments.of("rules_java",         multiPattern, true),
            Arguments.of("rules_jvm_external", multiPattern, true),
            Arguments.of("rules_cc",           multiPattern, false)
        );
    }

    @ParameterizedTest(name = "isExcluded({0}) = {2}")
    @MethodSource("isExcludedCases")
    void isExcluded(String name, List<String> patterns, boolean expected) {
        assertEquals(expected, BazelInfrastructureModules.isExcluded(name, patterns));
    }

    @Test
    void isExcluded_nullUserPatternsIgnored() {
        assertFalse(BazelInfrastructureModules.isExcluded("rules_cc", null));
        assertTrue(BazelInfrastructureModules.isExcluded("bazel_tools", null));
    }

    // -------------------------------------------------------------------------
    // buildCombinedExclusionRegex — hard list + user patterns as a negative-lookahead regex;
    // used by the HTTP_ARCHIVE bzlmod pipeline line filter (separate code path from isExcluded)
    // -------------------------------------------------------------------------

    @Test
    void regex_hardPatternExcludesExactMatch() {
        String regex = BazelInfrastructureModules.buildCombinedExclusionRegex(Collections.emptyList());
        assertFalse("bazel_tools".matches(regex));   // excluded — does NOT match the keep-regex
        assertFalse("maven".matches(regex));
        assertFalse("platforms".matches(regex));
        assertFalse("local_config_cc".matches(regex));
        assertFalse("remotejdk17".matches(regex));
    }

    @Test
    void regex_hardExactPatternDoesNotExcludePrefix() {
        // W2 fix: "maven" exact pattern must not exclude "maven-utils"
        String regex = BazelInfrastructureModules.buildCombinedExclusionRegex(Collections.emptyList());
        assertTrue("maven-utils".matches(regex));
        assertTrue("bazel_tools_extra".matches(regex));
    }

    @Test
    void regex_legitimateModulesNotExcluded() {
        String regex = BazelInfrastructureModules.buildCombinedExclusionRegex(Collections.emptyList());
        assertTrue("protobuf".matches(regex));
        assertTrue("abseil-cpp".matches(regex));
        assertTrue("rules_cc".matches(regex));
    }

    @Test
    void regex_userExactPatternWithCanonicalSuffix() {
        // canonical suffix fix: user pattern "protobuf" must match "protobuf+" and "protobuf~"
        List<String> patterns = Collections.singletonList("protobuf");
        String regex = BazelInfrastructureModules.buildCombinedExclusionRegex(patterns);
        assertFalse("protobuf".matches(regex));
        assertFalse("protobuf+".matches(regex));    // canonical suffix
        assertFalse("protobuf~".matches(regex));    // canonical suffix
        assertTrue("protobuf-java".matches(regex)); // NOT excluded
    }

    @Test
    void regex_userWildcardAlsoMatchesCanonicalSuffix() {
        List<String> patterns = Collections.singletonList("rules_*");
        String regex = BazelInfrastructureModules.buildCombinedExclusionRegex(patterns);
        assertFalse("rules_cc".matches(regex));
        assertFalse("rules_cc+".matches(regex));    // canonical suffix variant
        assertFalse("rules_java".matches(regex));
        assertTrue("protobuf".matches(regex));
    }

    @Test
    void regex_userPatternsAppendedToHardList() {
        List<String> patterns = Collections.singletonList("protobuf");
        String regex = BazelInfrastructureModules.buildCombinedExclusionRegex(patterns);
        assertFalse("bazel_tools".matches(regex));  // still hard-excluded
        assertFalse("protobuf".matches(regex));     // user-excluded
        assertTrue("abseil-cpp".matches(regex));    // neither
    }

    // regex path — exercises globToRegexAlternative after expansion (HTTP_ARCHIVE pipeline)
    @Test
    void regex_userAlternationPatternExcludesEachAlternative() {
        List<String> patterns = BazelInfrastructureModules.expandUserPatterns(
            Collections.singletonList("rules_(cc|shell|python)"));
        String regex = BazelInfrastructureModules.buildCombinedExclusionRegex(patterns);
        assertFalse("rules_cc".matches(regex));
        assertFalse("rules_shell".matches(regex));
        assertFalse("rules_python".matches(regex));
        assertTrue("rules_java".matches(regex));    // not in the group
        assertTrue("protobuf".matches(regex));
    }

    @Test
    void regex_userAlternationPatternAlsoMatchesCanonicalSuffix() {
        // Each expanded alternative picks up canonical suffix handling via globToRegexAlternative
        List<String> patterns = BazelInfrastructureModules.expandUserPatterns(
            Collections.singletonList("rules_(cc|shell)"));
        String regex = BazelInfrastructureModules.buildCombinedExclusionRegex(patterns);
        assertFalse("rules_cc".matches(regex));
        assertFalse("rules_cc+".matches(regex));    // canonical suffix
        assertFalse("rules_cc~".matches(regex));    // canonical suffix
        assertFalse("rules_shell".matches(regex));
        assertFalse("rules_shell+".matches(regex)); // canonical suffix
        assertFalse("rules_shell~".matches(regex)); // canonical suffix
        assertTrue("rules_cc_extra".matches(regex)); // not excluded — exact match only
    }

    // -------------------------------------------------------------------------
    // expandPattern — expands alternation groups before matching or regex generation
    // -------------------------------------------------------------------------

    @Test
    void expandPattern_noGroup_returnsPatternUnchanged() {
        List<String> result = BazelInfrastructureModules.expandPattern("rules_*");
        assertEquals(1, result.size());
        assertEquals("rules_*", result.get(0));
    }

    @Test
    void expandPattern_singleGroup_expandsToMultiple() {
        List<String> result = BazelInfrastructureModules.expandPattern("rules_(cc|shell|python)");
        assertEquals(3, result.size());
        assertTrue(result.contains("rules_cc"));
        assertTrue(result.contains("rules_shell"));
        assertTrue(result.contains("rules_python"));
    }

    @Test
    void expandPattern_groupWithWildcard() {
        List<String> result = BazelInfrastructureModules.expandPattern("rules_(cc|shell)_*");
        assertEquals(2, result.size());
        assertTrue(result.contains("rules_cc_*"));
        assertTrue(result.contains("rules_shell_*"));
    }

    @Test
    void expandPattern_multipleGroups() {
        List<String> result = BazelInfrastructureModules.expandPattern("(a|b)_(x|y)");
        assertEquals(4, result.size());
        assertTrue(result.contains("a_x"));
        assertTrue(result.contains("a_y"));
        assertTrue(result.contains("b_x"));
        assertTrue(result.contains("b_y"));
    }

    @Test
    void expandPattern_nestedGroups_matchingCloseFoundCorrectly() {
        // rules_((cc|shell)|python) → rules_cc, rules_shell, rules_python
        List<String> result = BazelInfrastructureModules.expandPattern("rules_((cc|shell)|python)");
        assertEquals(3, result.size());
        assertTrue(result.contains("rules_cc"));
        assertTrue(result.contains("rules_shell"));
        assertTrue(result.contains("rules_python"));
    }

    @Test
    void expandPattern_malformedUnmatchedParen_returnsAsIs() {
        List<String> result = BazelInfrastructureModules.expandPattern("rules_(cc");
        assertEquals(1, result.size());
        assertEquals("rules_(cc", result.get(0));
    }

    // isExcluded path — exercises FilenameUtils.wildcardMatch after expansion
    // Patterns are pre-expanded via expandUserPatterns, as callers do at init time
    @Test
    void isExcluded_alternationPatternExcludesMatchingModules() {
        List<String> patterns = BazelInfrastructureModules.expandUserPatterns(
            Collections.singletonList("rules_(cc|shell|python)"));
        assertTrue(BazelInfrastructureModules.isExcluded("rules_cc", patterns));
        assertTrue(BazelInfrastructureModules.isExcluded("rules_shell", patterns));
        assertTrue(BazelInfrastructureModules.isExcluded("rules_python", patterns));
        assertFalse(BazelInfrastructureModules.isExcluded("rules_java", patterns));
        assertFalse(BazelInfrastructureModules.isExcluded("protobuf", patterns));
    }
}
