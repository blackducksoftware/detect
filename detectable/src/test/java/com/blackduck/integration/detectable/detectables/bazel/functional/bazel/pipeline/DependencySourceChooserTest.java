package com.blackduck.integration.detectable.detectables.bazel.functional.bazel.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.apache.commons.compress.utils.Sets;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.bazel.DependencySource;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.DependencySourceChooser;
import com.blackduck.integration.exception.IntegrationException;

class DependencySourceChooserTest {

    private static final Set<DependencySource> DEPENDENCY_SOURCES_JUST_MAVEN_INSTALL = Sets.newHashSet(DependencySource.MAVEN_INSTALL);
    private static final Set<DependencySource> DEPENDENCY_SOURCES_JUST_MAVEN_JAR = Sets.newHashSet(DependencySource.MAVEN_JAR);
    private static final Set<DependencySource> DEPENDENCY_SOURCES_THREE = Sets.newHashSet(
        DependencySource.MAVEN_INSTALL,
        DependencySource.HASKELL_CABAL_LIBRARY,
        DependencySource.MAVEN_JAR
    );
    private static final Set<DependencySource> DEPENDENCY_SOURCES_HASKELL = Sets.newHashSet(DependencySource.HASKELL_CABAL_LIBRARY);

    @Test
    void testOneRuleParsed() throws IntegrationException {
        Set<DependencySource> chosenDependencySources = run(null, DEPENDENCY_SOURCES_JUST_MAVEN_INSTALL);
        assertEquals(1, chosenDependencySources.size());
        assertEquals("maven_install", chosenDependencySources.iterator().next().getName());
    }

    @Test
    void testThreeRulesParsed() throws IntegrationException {
        Set<DependencySource> chosenDependencySources = run(null, DEPENDENCY_SOURCES_THREE);
        assertEquals(3, chosenDependencySources.size());
    }

    @Test
    void testOneProvidedSameOneParsed() throws IntegrationException {
        Set<DependencySource> chosenDependencySources = run(DEPENDENCY_SOURCES_JUST_MAVEN_INSTALL, DEPENDENCY_SOURCES_JUST_MAVEN_INSTALL);
        assertEquals(1, chosenDependencySources.size());
        assertEquals("maven_install", chosenDependencySources.iterator().next().getName());
    }

    @Test
    void testOneRuleProvidedDifferentOneParsed() throws IntegrationException {
        Set<DependencySource> chosenDependencySources = run(DEPENDENCY_SOURCES_JUST_MAVEN_JAR, DEPENDENCY_SOURCES_JUST_MAVEN_INSTALL);
        assertEquals(1, chosenDependencySources.size());
        assertEquals("maven_jar", chosenDependencySources.iterator().next().getName());
    }

    @Test
    void testThreeProvidedOneParsed() throws IntegrationException {
        Set<DependencySource> chosenDependencySources = run(DEPENDENCY_SOURCES_THREE, DEPENDENCY_SOURCES_JUST_MAVEN_INSTALL);
        assertEquals(3, chosenDependencySources.size());
    }

    private Set<DependencySource> run(Set<DependencySource> dependencySourcesFromProperty, Set<DependencySource> parsedDependencySources)
        throws IntegrationException {
        DependencySourceChooser dependencySourceChooser = new DependencySourceChooser();
        Set<DependencySource> chosenDependencySources = dependencySourceChooser.choose(parsedDependencySources, dependencySourcesFromProperty);
        return chosenDependencySources;
    }
}
