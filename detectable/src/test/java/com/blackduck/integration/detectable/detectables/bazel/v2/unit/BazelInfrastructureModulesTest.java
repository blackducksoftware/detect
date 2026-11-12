package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.bazel.v2.BazelInfrastructureModules;

class BazelInfrastructureModulesTest {

    // -------------------------------------------------------------------------
    // isHardExcluded
    // -------------------------------------------------------------------------

    @Test
    void hardExcluded_exactMatches() {
        assertTrue(BazelInfrastructureModules.isHardExcluded("bazel_tools"));
        assertTrue(BazelInfrastructureModules.isHardExcluded("platforms"));
        assertTrue(BazelInfrastructureModules.isHardExcluded("maven"));
        assertTrue(BazelInfrastructureModules.isHardExcluded("unpinned_maven"));
    }

    @Test
    void hardExcluded_wildcardPatterns() {
        assertTrue(BazelInfrastructureModules.isHardExcluded("local_config_cc"));
        assertTrue(BazelInfrastructureModules.isHardExcluded("local_config_platform"));
        assertTrue(BazelInfrastructureModules.isHardExcluded("remotejdk17"));
        assertTrue(BazelInfrastructureModules.isHardExcluded("remotejdk_linux_amd64"));
    }

    @Test
    void hardExcluded_exactDoesNotActAsPrefix() {
        // W2 fix: exact patterns must not exclude names that merely start with the same string
        assertFalse(BazelInfrastructureModules.isHardExcluded("maven-utils"));
        assertFalse(BazelInfrastructureModules.isHardExcluded("bazel_tools_extra"));
        assertFalse(BazelInfrastructureModules.isHardExcluded("platforms_extra"));
    }

    @Test
    void hardExcluded_nullReturnsFalse() {
        assertFalse(BazelInfrastructureModules.isHardExcluded(null));
    }

    @Test
    void hardExcluded_legitimateModulesNotExcluded() {
        assertFalse(BazelInfrastructureModules.isHardExcluded("protobuf"));
        assertFalse(BazelInfrastructureModules.isHardExcluded("rules_cc"));
        assertFalse(BazelInfrastructureModules.isHardExcluded("abseil-cpp"));
        assertFalse(BazelInfrastructureModules.isHardExcluded("rules_jvm_external"));
    }

    // -------------------------------------------------------------------------
    // isExcluded with user patterns
    // -------------------------------------------------------------------------

    @Test
    void isExcluded_hardListTakesPrecedenceOverEmptyUserPatterns() {
        assertTrue(BazelInfrastructureModules.isExcluded("bazel_tools", Collections.emptyList()));
        assertFalse(BazelInfrastructureModules.isExcluded("rules_cc", Collections.emptyList()));
    }

    @Test
    void isExcluded_userExactPatternMatchesFullNameOnly() {
        List<String> patterns = Collections.singletonList("rules_java");
        assertTrue(BazelInfrastructureModules.isExcluded("rules_java", patterns));
        // W2 fix: exact user pattern must not exclude names that merely start with it
        assertFalse(BazelInfrastructureModules.isExcluded("rules_javacc", patterns));
        assertFalse(BazelInfrastructureModules.isExcluded("rules_java_extra", patterns));
    }

    @Test
    void isExcluded_userWildcardPattern() {
        List<String> patterns = Collections.singletonList("rules_*");
        assertTrue(BazelInfrastructureModules.isExcluded("rules_cc", patterns));
        assertTrue(BazelInfrastructureModules.isExcluded("rules_java", patterns));
        assertTrue(BazelInfrastructureModules.isExcluded("rules_python", patterns));
        assertFalse(BazelInfrastructureModules.isExcluded("protobuf", patterns));
    }

    @Test
    void isExcluded_multipleUserPatterns() {
        List<String> patterns = Arrays.asList("rules_java", "rules_jvm_external");
        assertTrue(BazelInfrastructureModules.isExcluded("rules_java", patterns));
        assertTrue(BazelInfrastructureModules.isExcluded("rules_jvm_external", patterns));
        assertFalse(BazelInfrastructureModules.isExcluded("rules_cc", patterns));
    }

    @Test
    void isExcluded_nullUserPatternsIgnored() {
        assertFalse(BazelInfrastructureModules.isExcluded("rules_cc", null));
        assertTrue(BazelInfrastructureModules.isExcluded("bazel_tools", null));
    }

    // -------------------------------------------------------------------------
    // buildCombinedExclusionRegex
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
        assertTrue("maven-utils".matches(regex));    // NOT excluded — matches keep-regex
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
        assertFalse("protobuf".matches(regex));     // excluded
        assertFalse("protobuf+".matches(regex));    // excluded (canonical suffix)
        assertFalse("protobuf~".matches(regex));    // excluded (canonical suffix)
        assertTrue("protobuf-java".matches(regex)); // NOT excluded
    }

    @Test
    void regex_userWildcardAlsoMatchesCanonicalSuffix() {
        List<String> patterns = Collections.singletonList("rules_*");
        String regex = BazelInfrastructureModules.buildCombinedExclusionRegex(patterns);
        assertFalse("rules_cc".matches(regex));
        assertFalse("rules_cc+".matches(regex));    // canonical suffix variant
        assertFalse("rules_java".matches(regex));
        assertTrue("protobuf".matches(regex));      // not matched by rules_*
    }

    // -------------------------------------------------------------------------
    // expandPattern
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
    void expandPattern_malformedUnmatchedParen_returnsAsIs() {
        List<String> result = BazelInfrastructureModules.expandPattern("rules_(cc");
        assertEquals(1, result.size());
        assertEquals("rules_(cc", result.get(0));
    }

    @Test
    void isExcluded_alternationPatternExcludesMatchingModules() {
        List<String> patterns = Collections.singletonList("rules_(cc|shell|python)");
        assertTrue(BazelInfrastructureModules.isExcluded("rules_cc", patterns));
        assertTrue(BazelInfrastructureModules.isExcluded("rules_shell", patterns));
        assertTrue(BazelInfrastructureModules.isExcluded("rules_python", patterns));
        assertFalse(BazelInfrastructureModules.isExcluded("rules_java", patterns));
        assertFalse(BazelInfrastructureModules.isExcluded("protobuf", patterns));
    }

    @Test
    void regex_userPatternsAppendedToHardList() {
        List<String> patterns = Collections.singletonList("protobuf");
        String regex = BazelInfrastructureModules.buildCombinedExclusionRegex(patterns);
        assertFalse("bazel_tools".matches(regex));  // still hard-excluded
        assertFalse("protobuf".matches(regex));     // user-excluded
        assertTrue("abseil-cpp".matches(regex));    // neither
    }
}
