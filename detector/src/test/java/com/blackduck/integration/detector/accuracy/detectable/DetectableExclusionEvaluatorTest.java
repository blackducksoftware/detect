package com.blackduck.integration.detector.accuracy.detectable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.blackduck.integration.detector.rule.DetectableDefinition;

public class DetectableExclusionEvaluatorTest {

    @Test
    void testEmptyExclusionsDoesNotExcludeAnything() {
        DetectableExclusionEvaluator evaluator = new DetectableExclusionEvaluator(Collections.emptyList());
        DetectableDefinition detectableDefinition = createMockDetectableDefinition("Maven Pom");

        assertFalse(evaluator.isDetectableExcluded(detectableDefinition));
    }

    @ParameterizedTest
    @MethodSource("provideExclusionTestCases")
    void testDetectableExclusion(List<String> exclusions, String detectableName, boolean expectedExcluded) {
        DetectableExclusionEvaluator evaluator = new DetectableExclusionEvaluator(exclusions);
        DetectableDefinition detectableDefinition = createMockDetectableDefinition(detectableName);

        assertEquals(expectedExcluded, evaluator.isDetectableExcluded(detectableDefinition));
    }

    private static Stream<Arguments> provideExclusionTestCases() {
        return Stream.of(
            // Exact match
            Arguments.of(Arrays.asList("Maven Pom"), "Maven Pom", true),
            Arguments.of(Arrays.asList("Maven Pom"), "Gradle Inspector", false),

            // Case insensitive matching
            Arguments.of(Arrays.asList("maven pom"), "Maven Pom", true),
            Arguments.of(Arrays.asList("MAVEN POM"), "Maven Pom", true),
            Arguments.of(Arrays.asList("Maven Pom"), "maven pom", true),

            // Space handling - spaces are removed for matching
            Arguments.of(Arrays.asList("MavenPom"), "Maven Pom", true),
            Arguments.of(Arrays.asList("Maven Pom"), "MavenPom", true),
            Arguments.of(Arrays.asList("maven pom"), "MavenPom", true),
            Arguments.of(Arrays.asList("mavenpom"), "Maven Pom", true),

            // Multiple exclusions
            Arguments.of(Arrays.asList("Maven Pom", "Gradle Inspector"), "Maven Pom", true),
            Arguments.of(Arrays.asList("Maven Pom", "Gradle Inspector"), "Gradle Inspector", true),
            Arguments.of(Arrays.asList("Maven Pom", "Gradle Inspector"), "NPM Package Lock", false),

            // Complex names
            Arguments.of(Arrays.asList("Go Mod"), "Go Mod", true),
            Arguments.of(Arrays.asList("gomod"), "Go Mod", true),
            Arguments.of(Arrays.asList("NPM Package Lock"), "npm package lock", true),
            Arguments.of(Arrays.asList("npmpackagelock"), "NPM Package Lock", true),

            // Partial matches should not work
            Arguments.of(Arrays.asList("Maven"), "Maven Pom", false),
            Arguments.of(Arrays.asList("Pom"), "Maven Pom", false),

            // Empty detectable name
            Arguments.of(Arrays.asList(""), "", true),
            Arguments.of(Arrays.asList("Maven Pom"), "", false)
        );
    }

    private DetectableDefinition createMockDetectableDefinition(String name) {
        DetectableDefinition detectableDefinition = mock(DetectableDefinition.class);
        when(detectableDefinition.getName()).thenReturn(name);
        return detectableDefinition;
    }
}