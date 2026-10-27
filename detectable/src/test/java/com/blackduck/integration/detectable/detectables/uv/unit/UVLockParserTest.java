package com.blackduck.integration.detectable.detectables.uv.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import com.blackduck.integration.detectable.detectables.uv.transform.UVLockParser;

class UVLockParserTest {

    private ExternalIdFactory externalIdFactory;

    @BeforeEach
    void setUp() {
        externalIdFactory = new ExternalIdFactory();
    }

    @Test
    void parseSimpleLockFile() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"requests\" },",
            "]",
            "",
            "[[package]]",
            "name = \"requests\"",
            "version = \"2.31.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "my-project", options);

        assertEquals(1, codeLocations.size(), "Expected exactly one code location for single-project lock file");
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size(), "Expected one root dependency (requests)");
        assertTrue(hasDependency(graph.getRootDependencies(), "requests", "2.31.0"), "Expected 'requests' v2.31.0 as root dependency");
    }

    @Test
    void parseWorkspaceMembers() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[manifest]",
            "members = [\"lib-a\", \"lib-b\"]",
            "",
            "[[package]]",
            "name = \"lib-a\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"shared-dep\" },",
            "]",
            "",
            "[[package]]",
            "name = \"lib-b\"",
            "version = \"2.0.0\"",
            "dependencies = [",
            "    { name = \"shared-dep\" },",
            "]",
            "",
            "[[package]]",
            "name = \"shared-dep\"",
            "version = \"3.0.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "lib-a", options);

        assertEquals(2, codeLocations.size(), "Expected two code locations for two workspace members");
    }

    @Test
    void parseDevDependencies() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"requests\" },",
            "]",
            "",
            "[package.dev-dependencies]",
            "dev = [",
            "    { name = \"pytest\" },",
            "]",
            "",
            "[[package]]",
            "name = \"requests\"",
            "version = \"2.31.0\"",
            "",
            "[[package]]",
            "name = \"pytest\"",
            "version = \"7.4.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "my-project", options);

        assertEquals(1, codeLocations.size(), "Expected one code location");
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(2, graph.getRootDependencies().size(), "Expected two root dependencies (requests + pytest)");
        assertTrue(hasDependency(graph.getRootDependencies(), "requests", "2.31.0"), "Expected 'requests' v2.31.0 as root dependency");
        assertTrue(hasDependency(graph.getRootDependencies(), "pytest", "7.4.0"), "Expected 'pytest' v7.4.0 as root dependency from dev group");
    }

    @Test
    void excludeDevDependencyGroup() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"requests\" },",
            "]",
            "",
            "[package.dev-dependencies]",
            "dev = [",
            "    { name = \"pytest\" },",
            "]",
            "",
            "[[package]]",
            "name = \"requests\"",
            "version = \"2.31.0\"",
            "",
            "[[package]]",
            "name = \"pytest\"",
            "version = \"7.4.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "my-project", options);

        assertEquals(1, codeLocations.size(), "Expected one code location");
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size(), "Expected only one root dependency after excluding dev group");
        assertTrue(hasDependency(graph.getRootDependencies(), "requests", "2.31.0"), "Expected 'requests' to remain after excluding dev group");
    }

    @Test
    void parseOptionalDependencies() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"requests\" },",
            "]",
            "",
            "[package.optional-dependencies]",
            "extras = [",
            "    { name = \"boto3\" },",
            "]",
            "",
            "[[package]]",
            "name = \"requests\"",
            "version = \"2.31.0\"",
            "",
            "[[package]]",
            "name = \"boto3\"",
            "version = \"1.28.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "my-project", options);

        assertEquals(1, codeLocations.size(), "Expected one code location");
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(2, graph.getRootDependencies().size(), "Expected two root dependencies (requests + boto3)");
        assertTrue(hasDependency(graph.getRootDependencies(), "requests", "2.31.0"), "Expected 'requests' as root dependency");
        assertTrue(hasDependency(graph.getRootDependencies(), "boto3", "1.28.0"), "Expected 'boto3' as root dependency from optional group");
    }

    @Test
    void excludeOptionalDependencyGroup() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"requests\" },",
            "]",
            "",
            "[package.optional-dependencies]",
            "extras = [",
            "    { name = \"boto3\" },",
            "]",
            "",
            "[[package]]",
            "name = \"requests\"",
            "version = \"2.31.0\"",
            "",
            "[[package]]",
            "name = \"boto3\"",
            "version = \"1.28.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("extras"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "my-project", options);

        assertEquals(1, codeLocations.size(), "Expected one code location");
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size(), "Expected only one root dependency after excluding extras group");
        assertTrue(hasDependency(graph.getRootDependencies(), "requests", "2.31.0"), "Expected 'requests' to remain after excluding extras");
    }

    @Test
    void keepVersionWithPlusSuffix() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"torch\" },",
            "]",
            "",
            "[[package]]",
            "name = \"torch\"",
            "version = \"2.0.0+cu118\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "my-project", options);

        assertEquals(1, codeLocations.size(), "Expected one code location");
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertTrue(hasDependency(graph.getRootDependencies(), "torch", "2.0.0+cu118"), "Expected the '+cu118' local version suffix to be kept");
        assertFalse(hasDependency(graph.getRootDependencies(), "torch", "2.0.0"), "Expected the version not to be stripped to '2.0.0'");

        Dependency torch = findDependency(graph.getRootDependencies(), "torch", "2.0.0+cu118");
        assertEquals("2.0.0+cu118", torch.getExternalId().getVersion(), "Expected the ExternalId to carry the full version including '+cu118'");
    }

    @Test
    void normalizePackageNameInDependencyOutput() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"some_package\" },",
            "]",
            "",
            "[[package]]",
            "name = \"some_package\"",
            "version = \"1.0.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "my-project", options);

        assertEquals(1, codeLocations.size(), "Expected one code location");
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertTrue(hasDependency(graph.getRootDependencies(), "some-package", "1.0.0"), "Expected underscore in 'some_package' to be normalized to hyphen");
    }

    @Test
    void excludeWorkspaceMember() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[manifest]",
            "members = [\"lib-a\", \"lib-b\"]",
            "",
            "[[package]]",
            "name = \"lib-a\"",
            "version = \"1.0.0\"",
            "",
            "[[package]]",
            "name = \"lib-b\"",
            "version = \"2.0.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Collections.emptyList(),
            Arrays.asList("lib-b")
        );

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "lib-a", options);

        assertEquals(1, codeLocations.size(), "Expected one code location after excluding lib-b");
        assertEquals("lib-a", codeLocations.get(0).getExternalId().get().getName(), "Expected lib-a as the remaining workspace member");
    }

    @Test
    void includeOnlySpecificWorkspaceMember() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[manifest]",
            "members = [\"lib-a\", \"lib-b\", \"lib-c\"]",
            "",
            "[[package]]",
            "name = \"lib-a\"",
            "version = \"1.0.0\"",
            "",
            "[[package]]",
            "name = \"lib-b\"",
            "version = \"2.0.0\"",
            "",
            "[[package]]",
            "name = \"lib-c\"",
            "version = \"3.0.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Arrays.asList("lib-a"),
            Collections.emptyList()
        );

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "lib-a", options);

        assertEquals(1, codeLocations.size(), "Expected one code location for included member lib-a");
        assertEquals("lib-a", codeLocations.get(0).getExternalId().get().getName(), "Expected lib-a as the only included workspace member");
    }

    @Test
    void excludedMemberWithUnderscoreMatchesNormalizedMemberName() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[manifest]",
            "members = [\"lib-a\", \"lib-b\"]",
            "",
            "[[package]]",
            "name = \"lib-a\"",
            "version = \"1.0.0\"",
            "",
            "[[package]]",
            "name = \"lib-b\"",
            "version = \"2.0.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Collections.emptyList(),
            Arrays.asList("lib_a")
        );

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "lib-a", options);

        assertEquals(1, codeLocations.size(), "Expected lib-a excluded and lib-b included");
        assertEquals("lib-b", codeLocations.get(0).getExternalId().get().getName(), "Expected lib-b as the remaining workspace member");
    }

    @Test
    void includedMemberWithMixedCaseAndDotsMatchesNormalizedMemberName() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[manifest]",
            "members = [\"lib-a\", \"lib-b\"]",
            "",
            "[[package]]",
            "name = \"lib-a\"",
            "version = \"1.0.0\"",
            "",
            "[[package]]",
            "name = \"lib-b\"",
            "version = \"2.0.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Arrays.asList("Lib.A"),
            Collections.emptyList()
        );

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "lib-a", options);

        assertEquals(1, codeLocations.size(), "Expected only lib-a included");
        assertEquals("lib-a", codeLocations.get(0).getExternalId().get().getName(), "Expected lib-a as the only included workspace member");
    }

    @Test
    void excludeFilterMatchesMemberWhenLockFileNamesAreNotNormalized() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[manifest]",
            "members = [\"lib_a\", \"lib-b\"]",
            "",
            "[[package]]",
            "name = \"lib_a\"",
            "version = \"1.0.0\"",
            "",
            "[[package]]",
            "name = \"lib-b\"",
            "version = \"2.0.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Collections.emptyList(),
            Arrays.asList("Lib.A")
        );

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "lib_a", options);

        assertEquals(1, codeLocations.size(), "Expected lib_a to be excluded by 'Lib.A', leaving only lib-b");
        assertEquals("lib-b", codeLocations.get(0).getExternalId().get().getName(), "Expected lib-b as the only remaining workspace member");
    }

    @Test
    void includeFilterWithRepeatedSeparatorsMatchesNormalizedMemberName() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[manifest]",
            "members = [\"lib-a\", \"lib-b\"]",
            "",
            "[[package]]",
            "name = \"lib-a\"",
            "version = \"1.0.0\"",
            "",
            "[[package]]",
            "name = \"lib-b\"",
            "version = \"2.0.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Arrays.asList("lib.-a"),
            Collections.emptyList()
        );

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "lib-a", options);

        assertEquals(1, codeLocations.size(), "Expected only lib-a to be included by 'lib.-a'");
        assertEquals("lib-a", codeLocations.get(0).getExternalId().get().getName(), "Expected lib-a as the only included workspace member");
    }

    @Test
    void handleMissingVersion() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "dependencies = [",
            "    { name = \"no-version-pkg\" },",
            "]",
            "",
            "[[package]]",
            "name = \"no-version-pkg\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "my-project", options);

        assertEquals(1, codeLocations.size(), "Expected one code location");
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertTrue(hasDependency(graph.getRootDependencies(), "no-version-pkg", "defaultVersion"), "Expected 'defaultVersion' when version key is missing");
    }

    @Test
    void parseTransitiveDependencies() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"requests\" },",
            "]",
            "",
            "[[package]]",
            "name = \"requests\"",
            "version = \"2.31.0\"",
            "dependencies = [",
            "    { name = \"urllib3\" },",
            "    { name = \"charset-normalizer\" },",
            "]",
            "",
            "[[package]]",
            "name = \"urllib3\"",
            "version = \"2.0.4\"",
            "",
            "[[package]]",
            "name = \"charset-normalizer\"",
            "version = \"3.2.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "my-project", options);

        assertEquals(1, codeLocations.size(), "Expected one code location");
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();

        assertEquals(1, graph.getRootDependencies().size(), "Expected one root dependency (requests)");
        assertTrue(hasDependency(graph.getRootDependencies(), "requests", "2.31.0"), "Expected 'requests' as root dependency");

        Dependency requestsDep = findDependency(graph.getRootDependencies(), "requests");
        Set<Dependency> requestsChildren = graph.getChildrenForParent(requestsDep);
        assertEquals(2, requestsChildren.size(), "Expected two transitive dependencies under requests");
        assertTrue(hasDependency(requestsChildren, "urllib3", "2.0.4"), "Expected 'urllib3' as transitive dependency of requests");
        assertTrue(hasDependency(requestsChildren, "charset-normalizer", "3.2.0"), "Expected 'charset-normalizer' as transitive dependency of requests");
    }

    private UVDetectorOptions createDefaultOptions() {
        return new UVDetectorOptions(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );
    }

    // -----------------------------------------------------------------------
    // Tests for detect.uv.dependency.groups.only in the lockfile detector
    // -----------------------------------------------------------------------

    @Test
    void onlyDependencyGroupsIncludesOnlySpecifiedDevGroup() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "",
            "[package.dev-dependencies]",
            "dev = [",
            "    { name = \"pytest\" },",
            "]",
            "lint = [",
            "    { name = \"ruff\" },",
            "]",
            "",
            "[[package]]",
            "name = \"pytest\"",
            "version = \"8.3.0\"",
            "",
            "[[package]]",
            "name = \"ruff\"",
            "version = \"0.4.1\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Arrays.asList("dev"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "my-project", options);

        assertEquals(1, codeLocations.size(), "Expected one code location");
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size(), "Expected only one root dependency from the 'dev' group");
        assertTrue(hasDependency(graph.getRootDependencies(), "pytest", "8.3.0"), "Expected 'pytest' from dev group to be included");
        assertTrue(graph.getRootDependencies().stream().noneMatch(d -> d.getName().equals("ruff")), "Expected 'ruff' from lint group to NOT be included");
    }

    @Test
    void onlyDependencyGroupsSkipsRegularDependencies() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"requests\" },",
            "]",
            "",
            "[package.dev-dependencies]",
            "dev = [",
            "    { name = \"pytest\" },",
            "]",
            "",
            "[[package]]",
            "name = \"requests\"",
            "version = \"2.31.0\"",
            "",
            "[[package]]",
            "name = \"pytest\"",
            "version = \"8.3.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Arrays.asList("dev"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "my-project", options);

        assertEquals(1, codeLocations.size(), "Expected one code location");
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size(), "Expected only one root dependency (from dev group, not regular deps)");
        assertTrue(hasDependency(graph.getRootDependencies(), "pytest", "8.3.0"), "Expected 'pytest' from dev group to be included");
        assertTrue(graph.getRootDependencies().stream().noneMatch(d -> d.getName().equals("requests")), "Expected regular [dependencies] to be skipped when onlyGroups is set");
    }

    @Test
    void onlyDependencyGroupsSkipsOptionalDependencies() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "",
            "[package.dev-dependencies]",
            "dev = [",
            "    { name = \"pytest\" },",
            "]",
            "",
            "[package.optional-dependencies]",
            "extras = [",
            "    { name = \"boto3\" },",
            "]",
            "",
            "[[package]]",
            "name = \"pytest\"",
            "version = \"8.3.0\"",
            "",
            "[[package]]",
            "name = \"boto3\"",
            "version = \"1.28.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Arrays.asList("dev"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "my-project", options);

        assertEquals(1, codeLocations.size(), "Expected one code location");
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size(), "Expected only one root dependency (from dev group, not optional)");
        assertTrue(hasDependency(graph.getRootDependencies(), "pytest", "8.3.0"), "Expected 'pytest' from dev group to be included");
        assertTrue(graph.getRootDependencies().stream().noneMatch(d -> d.getName().equals("boto3")), "Expected [optional-dependencies] to be skipped when onlyGroups is set");
    }

    @Test
    void onlyAndExcludedGroupsPartialOverlapExcludedWins() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "",
            "[package.dev-dependencies]",
            "dev = [",
            "    { name = \"pytest\" },",
            "]",
            "lint = [",
            "    { name = \"ruff\" },",
            "]",
            "docs = [",
            "    { name = \"sphinx\" },",
            "]",
            "",
            "[[package]]",
            "name = \"pytest\"",
            "version = \"8.3.0\"",
            "",
            "[[package]]",
            "name = \"ruff\"",
            "version = \"0.4.1\"",
            "",
            "[[package]]",
            "name = \"sphinx\"",
            "version = \"7.0.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("lint"),
            Arrays.asList("dev", "lint"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "my-project", options);

        assertEquals(1, codeLocations.size(), "Expected one code location");
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size(), "Expected only 'pytest' — 'lint' excluded, 'docs' not in onlyGroups");
        assertTrue(hasDependency(graph.getRootDependencies(), "pytest", "8.3.0"), "Expected 'pytest' (dev group in only, not excluded)");
        assertTrue(graph.getRootDependencies().stream().noneMatch(d -> d.getName().equals("ruff")), "Expected 'ruff' excluded (lint in both only and excluded)");
        assertTrue(graph.getRootDependencies().stream().noneMatch(d -> d.getName().equals("sphinx")), "Expected 'sphinx' excluded (docs not in onlyGroups list)");
    }

    @Test
    void onlyAndExcludedGroupsAllSameReturnsEmptyBom() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "",
            "[package.dev-dependencies]",
            "dev = [",
            "    { name = \"pytest\" },",
            "]",
            "lint = [",
            "    { name = \"ruff\" },",
            "]",
            "",
            "[[package]]",
            "name = \"pytest\"",
            "version = \"8.3.0\"",
            "",
            "[[package]]",
            "name = \"ruff\"",
            "version = \"0.4.1\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev", "lint"),
            Arrays.asList("dev", "lint"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "my-project", options);

        assertTrue(codeLocations.isEmpty(), "All only-groups are also excluded: parser should return empty list (extractor handles empty BOM creation)");
    }

    // Regression test for: with detect.uv.dependency.groups.only=dev set, a selected group's
    // direct dependency (e.g. "httpx") had its own `dependencies = [...]` block skipped, so its
    // transitive dependencies (anyio, certifi, httpcore, idna, sniffio) were dropped from the BOM.
    // Mirrors the real-world uv.lock example reported for httpx 0.27.0, plus an extra level of
    // nesting (httpcore -> certifi, h11) to confirm the fix isn't limited to a single level.
    @Test
    void onlyDependencyGroupsIncludesTransitiveDependenciesOfSelectedGroupMembers() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"requests\" },",
            "]",
            "",
            "[package.dev-dependencies]",
            "dev = [",
            "    { name = \"httpx\" },",
            "]",
            "",
            "[[package]]",
            "name = \"requests\"",
            "version = \"2.31.0\"",
            "",
            "[[package]]",
            "name = \"httpx\"",
            "version = \"0.27.0\"",
            "dependencies = [",
            "    { name = \"anyio\" },",
            "    { name = \"certifi\" },",
            "    { name = \"httpcore\" },",
            "    { name = \"idna\" },",
            "    { name = \"sniffio\" },",
            "]",
            "",
            "[[package]]",
            "name = \"anyio\"",
            "version = \"4.3.0\"",
            "dependencies = [",
            "    { name = \"idna\" },",
            "    { name = \"sniffio\" },",
            "]",
            "",
            "[[package]]",
            "name = \"certifi\"",
            "version = \"2024.2.2\"",
            "",
            "[[package]]",
            "name = \"httpcore\"",
            "version = \"1.0.2\"",
            "dependencies = [",
            "    { name = \"certifi\" },",
            "    { name = \"h11\" },",
            "]",
            "",
            "[[package]]",
            "name = \"idna\"",
            "version = \"3.6\"",
            "",
            "[[package]]",
            "name = \"sniffio\"",
            "version = \"1.3.1\"",
            "",
            "[[package]]",
            "name = \"h11\"",
            "version = \"0.14.0\""
        );

        UVLockParser parser = new UVLockParser(externalIdFactory);
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Arrays.asList("dev"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        List<CodeLocation> codeLocations = parser.parseLockFile(lockContent, "my-project", options);

        assertEquals(1, codeLocations.size(), "Expected one code location");
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();

        assertEquals(1, graph.getRootDependencies().size(), "Expected only 'httpx' as root dependency; 'requests' must remain excluded since onlyGroups=dev");
        assertTrue(hasDependency(graph.getRootDependencies(), "httpx", "0.27.0"), "Expected 'httpx' from the selected 'dev' group to be a root dependency");
        assertTrue(graph.getRootDependencies().stream().noneMatch(d -> d.getName().equals("requests")), "Expected regular [dependencies] ('requests') to remain excluded when onlyGroups is set");

        Dependency httpxDep = findDependency(graph.getRootDependencies(), "httpx");
        Set<Dependency> httpxChildren = graph.getChildrenForParent(httpxDep);
        assertEquals(5, httpxChildren.size(), "Expected httpx's own dependencies = [...] block to be parsed even though onlyGroups is set");
        assertTrue(hasDependency(httpxChildren, "anyio", "4.3.0"), "Expected 'anyio' as a transitive dependency of httpx");
        assertTrue(hasDependency(httpxChildren, "certifi", "2024.2.2"), "Expected 'certifi' as a transitive dependency of httpx");
        assertTrue(hasDependency(httpxChildren, "httpcore", "1.0.2"), "Expected 'httpcore' as a transitive dependency of httpx");
        assertTrue(hasDependency(httpxChildren, "idna", "3.6"), "Expected 'idna' as a transitive dependency of httpx");
        assertTrue(hasDependency(httpxChildren, "sniffio", "1.3.1"), "Expected 'sniffio' as a transitive dependency of httpx");

        Dependency httpcoreDep = findDependency(httpxChildren, "httpcore");
        Set<Dependency> httpcoreChildren = graph.getChildrenForParent(httpcoreDep);
        assertEquals(2, httpcoreChildren.size(), "Expected httpcore's own dependencies (certifi, h11) to also be parsed, confirming the fix isn't limited to a single level of transitivity");
        assertTrue(hasDependency(httpcoreChildren, "certifi", "2024.2.2"), "Expected 'certifi' as a transitive dependency of httpcore");
        assertTrue(hasDependency(httpcoreChildren, "h11", "0.14.0"), "Expected 'h11' as a transitive dependency of httpcore");
    }

    // -----------------------------------------------------------------------
    // Tests for packages that are resolved to multiple versions in uv.lock:
    // a package is identified by normalized name + version, not by name alone
    // -----------------------------------------------------------------------

    @Test
    void multipleVersionsOfSamePackageAreAllReported() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"ipython\", version = \"8.18.1\", marker = \"python_full_version < '3.10'\" },",
            "    { name = \"ipython\", version = \"9.0.0\", marker = \"python_full_version >= '3.10'\" },",
            "]",
            "",
            "[[package]]",
            "name = \"ipython\"",
            "version = \"8.18.1\"",
            "",
            "[[package]]",
            "name = \"ipython\"",
            "version = \"9.0.0\""
        );

        List<CodeLocation> codeLocations = new UVLockParser(externalIdFactory).parseLockFile(lockContent, "my-project", createDefaultOptions());

        assertEquals(1, codeLocations.size(), "Expected one code location");
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(2, graph.getRootDependencies().size(), "Expected both ipython versions as root dependencies");
        assertTrue(hasDependency(graph.getRootDependencies(), "ipython", "8.18.1"), "Expected 'ipython' v8.18.1 in the graph");
        assertTrue(hasDependency(graph.getRootDependencies(), "ipython", "9.0.0"), "Expected 'ipython' v9.0.0 in the graph");
    }

    @Test
    void multipleVersionsKeepTheirOwnTransitiveDependencies() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"ipython\", version = \"8.18.1\" },",
            "    { name = \"ipython\", version = \"9.0.0\" },",
            "]",
            "",
            "[[package]]",
            "name = \"ipython\"",
            "version = \"8.18.1\"",
            "dependencies = [",
            "    { name = \"jedi\" },",
            "]",
            "",
            "[[package]]",
            "name = \"ipython\"",
            "version = \"9.0.0\"",
            "dependencies = [",
            "    { name = \"ipython-pygments-lexers\" },",
            "]",
            "",
            "[[package]]",
            "name = \"jedi\"",
            "version = \"0.19.1\"",
            "",
            "[[package]]",
            "name = \"ipython-pygments-lexers\"",
            "version = \"1.1.1\""
        );

        List<CodeLocation> codeLocations = new UVLockParser(externalIdFactory).parseLockFile(lockContent, "my-project", createDefaultOptions());

        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        Dependency ipython8 = findDependency(graph.getRootDependencies(), "ipython", "8.18.1");
        Dependency ipython9 = findDependency(graph.getRootDependencies(), "ipython", "9.0.0");
        assertNotNull(ipython8, "Expected 'ipython' v8.18.1");
        assertNotNull(ipython9, "Expected 'ipython' v9.0.0");

        Set<Dependency> ipython8Children = graph.getChildrenForParent(ipython8);
        assertEquals(1, ipython8Children.size(), "ipython 8.x must only have its own dependency");
        assertTrue(hasDependency(ipython8Children, "jedi", "0.19.1"), "Expected 'jedi' under ipython 8.x");

        Set<Dependency> ipython9Children = graph.getChildrenForParent(ipython9);
        assertEquals(1, ipython9Children.size(), "ipython 9.x must only have its own dependency");
        assertTrue(hasDependency(ipython9Children, "ipython-pygments-lexers", "1.1.1"), "Expected 'ipython-pygments-lexers' under ipython 9.x");
        assertFalse(hasDependency(ipython9Children, "jedi", "0.19.1"), "Children of ipython 8.x must not leak into ipython 9.x");
    }

    @Test
    void sharedNameAndVersionIsReferencedByAllParents() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"parent-a\" },",
            "    { name = \"parent-b\" },",
            "]",
            "",
            "[[package]]",
            "name = \"parent-a\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"shared\", version = \"1.0.0\" },",
            "]",
            "",
            "[[package]]",
            "name = \"parent-b\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"shared\", version = \"1.0.0\" },",
            "]",
            "",
            "[[package]]",
            "name = \"shared\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"leaf\" },",
            "]",
            "",
            "[[package]]",
            "name = \"shared\"",
            "version = \"2.0.0\"",
            "",
            "[[package]]",
            "name = \"leaf\"",
            "version = \"0.1.0\""
        );

        List<CodeLocation> codeLocations = new UVLockParser(externalIdFactory).parseLockFile(lockContent, "my-project", createDefaultOptions());

        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        Dependency parentA = findDependency(graph.getRootDependencies(), "parent-a", "1.0.0");
        Dependency parentB = findDependency(graph.getRootDependencies(), "parent-b", "1.0.0");

        Dependency sharedFromA = findDependency(graph.getChildrenForParent(parentA), "shared", "1.0.0");
        Dependency sharedFromB = findDependency(graph.getChildrenForParent(parentB), "shared", "1.0.0");
        assertNotNull(sharedFromA, "Expected 'shared' v1.0.0 under parent-a");
        assertNotNull(sharedFromB, "Expected 'shared' v1.0.0 under parent-b");
        assertEquals(sharedFromA.getExternalId(), sharedFromB.getExternalId(), "A shared name + version must be the same component for all parents");

        // The shared node is expanded for every parent and does not receive the dependencies of another version
        assertTrue(hasDependency(graph.getChildrenForParent(sharedFromA), "leaf", "0.1.0"), "Expected 'leaf' under shared v1.0.0");
        assertEquals(1, graph.getChildrenForParent(sharedFromB).size(), "Expected the same single child for the shared node");
        assertFalse(hasDependency(graph.getChildrenForParent(parentA), "shared", "2.0.0"), "Version 2.0.0 was never referenced by parent-a");
    }

    @Test
    void unversionedReferenceResolvesToTheOnlyVersion() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"requests\" },",
            "]",
            "",
            "[[package]]",
            "name = \"requests\"",
            "version = \"2.31.0\""
        );

        List<CodeLocation> codeLocations = new UVLockParser(externalIdFactory).parseLockFile(lockContent, "my-project", createDefaultOptions());

        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size(), "Expected exactly one root dependency");
        assertTrue(hasDependency(graph.getRootDependencies(), "requests", "2.31.0"), "Expected the only available version of 'requests'");
    }

    @Test
    void unversionedReferenceIsAttachedToAllAvailableVersions() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"ipython\" },",
            "]",
            "",
            "[[package]]",
            "name = \"ipython\"",
            "version = \"8.18.1\"",
            "dependencies = [",
            "    { name = \"jedi\" },",
            "]",
            "",
            "[[package]]",
            "name = \"ipython\"",
            "version = \"9.0.0\"",
            "",
            "[[package]]",
            "name = \"jedi\"",
            "version = \"0.19.1\""
        );

        List<CodeLocation> codeLocations = new UVLockParser(externalIdFactory).parseLockFile(lockContent, "my-project", createDefaultOptions());

        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(2, graph.getRootDependencies().size(), "Expected the reference without version to be attached to both versions");
        assertTrue(hasDependency(graph.getRootDependencies(), "ipython", "8.18.1"), "Expected 'ipython' v8.18.1");
        assertTrue(hasDependency(graph.getRootDependencies(), "ipython", "9.0.0"), "Expected 'ipython' v9.0.0");

        Dependency ipython8 = findDependency(graph.getRootDependencies(), "ipython", "8.18.1");
        assertTrue(hasDependency(graph.getChildrenForParent(ipython8), "jedi", "0.19.1"), "Expected the children of ipython 8.x to be kept");
    }

    @Test
    void versionedReferenceWithLocalSuffixResolvesToExactVersion() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"torch\", version = \"2.0.0+cu118\" },",
            "]",
            "",
            "[[package]]",
            "name = \"torch\"",
            "version = \"2.0.0+cu118\"",
            "",
            "[[package]]",
            "name = \"torch\"",
            "version = \"2.1.0\""
        );

        List<CodeLocation> codeLocations = new UVLockParser(externalIdFactory).parseLockFile(lockContent, "my-project", createDefaultOptions());

        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size(), "Only the referenced torch version is expected");
        assertTrue(hasDependency(graph.getRootDependencies(), "torch", "2.0.0+cu118"), "Expected the reference to resolve to the exact locked version '2.0.0+cu118', suffix kept");
        assertFalse(hasDependency(graph.getRootDependencies(), "torch", "2.1.0"), "The other torch version must not be reported");
    }

    @Test
    void referenceToVersionThatIsNotInLockFileIsIgnored() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"requests\", version = \"9.9.9\" },",
            "]",
            "",
            "[[package]]",
            "name = \"requests\"",
            "version = \"2.31.0\""
        );

        List<CodeLocation> codeLocations = new UVLockParser(externalIdFactory).parseLockFile(lockContent, "my-project", createDefaultOptions());

        assertEquals(1, codeLocations.size(), "Expected one code location");
        assertTrue(codeLocations.get(0).getDependencyGraph().getRootDependencies().isEmpty(), "A version that is not in the lock file must not be replaced by another one");
    }

    @Test
    void multipleVersionsAcrossDependencyLevelsAndGroups() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"parent\" },",
            "]",
            "",
            "[package.dev-dependencies]",
            "dev = [",
            "    { name = \"ipython\", version = \"9.0.0\" },",
            "]",
            "",
            "[[package]]",
            "name = \"parent\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"ipython\", version = \"8.18.1\" },",
            "]",
            "",
            "[[package]]",
            "name = \"ipython\"",
            "version = \"8.18.1\"",
            "dependencies = [",
            "    { name = \"jedi\" },",
            "]",
            "",
            "[[package]]",
            "name = \"ipython\"",
            "version = \"9.0.0\"",
            "dependencies = [",
            "    { name = \"ipython-pygments-lexers\" },",
            "]",
            "",
            "[[package]]",
            "name = \"jedi\"",
            "version = \"0.19.1\"",
            "",
            "[[package]]",
            "name = \"ipython-pygments-lexers\"",
            "version = \"1.1.1\""
        );

        List<CodeLocation> codeLocations = new UVLockParser(externalIdFactory).parseLockFile(lockContent, "my-project", createDefaultOptions());

        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        Dependency parent = findDependency(graph.getRootDependencies(), "parent", "1.0.0");
        Dependency devIpython = findDependency(graph.getRootDependencies(), "ipython", "9.0.0");
        assertNotNull(parent, "Expected 'parent' as root dependency");
        assertNotNull(devIpython, "Expected 'ipython' v9.0.0 from the dev group as root dependency");

        Dependency transitiveIpython = findDependency(graph.getChildrenForParent(parent), "ipython", "8.18.1");
        assertNotNull(transitiveIpython, "Expected 'ipython' v8.18.1 as transitive dependency of 'parent'");

        assertTrue(hasDependency(graph.getChildrenForParent(transitiveIpython), "jedi", "0.19.1"), "Expected 'jedi' under ipython 8.x");
        assertEquals(1, graph.getChildrenForParent(transitiveIpython).size(), "ipython 8.x must only have its own dependency");
        assertTrue(hasDependency(graph.getChildrenForParent(devIpython), "ipython-pygments-lexers", "1.1.1"), "Expected 'ipython-pygments-lexers' under ipython 9.x");
        assertEquals(1, graph.getChildrenForParent(devIpython).size(), "ipython 9.x must only have its own dependency");
    }

    @Test
    void circularDependencyBetweenMultipleVersionsTerminates() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[[package]]",
            "name = \"my-project\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"pkg-a\", version = \"1.0.0\" },",
            "    { name = \"pkg-a\", version = \"2.0.0\" },",
            "]",
            "",
            "[[package]]",
            "name = \"pkg-a\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"pkg-b\" },",
            "]",
            "",
            "[[package]]",
            "name = \"pkg-a\"",
            "version = \"2.0.0\"",
            "dependencies = [",
            "    { name = \"pkg-c\" },",
            "]",
            "",
            "[[package]]",
            "name = \"pkg-b\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"pkg-a\", version = \"1.0.0\" },",
            "]",
            "",
            "[[package]]",
            "name = \"pkg-c\"",
            "version = \"1.0.0\""
        );

        List<CodeLocation> codeLocations = new UVLockParser(externalIdFactory).parseLockFile(lockContent, "my-project", createDefaultOptions());

        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        Dependency pkgA1 = findDependency(graph.getRootDependencies(), "pkg-a", "1.0.0");
        Dependency pkgA2 = findDependency(graph.getRootDependencies(), "pkg-a", "2.0.0");
        assertNotNull(pkgA1, "Expected 'pkg-a' v1.0.0");
        assertNotNull(pkgA2, "Expected 'pkg-a' v2.0.0");

        Dependency pkgB = findDependency(graph.getChildrenForParent(pkgA1), "pkg-b", "1.0.0");
        assertNotNull(pkgB, "Expected 'pkg-b' under pkg-a v1.0.0");
        assertTrue(hasDependency(graph.getChildrenForParent(pkgB), "pkg-a", "1.0.0"), "Expected the cycle back to pkg-a v1.0.0");
        assertTrue(hasDependency(graph.getChildrenForParent(pkgA2), "pkg-c", "1.0.0"), "Expected 'pkg-c' under pkg-a v2.0.0");
        assertEquals(1, graph.getChildrenForParent(pkgA2).size(), "pkg-a v2.0.0 must not get the dependencies of pkg-a v1.0.0");
    }

    @Test
    void everyWorkspaceMemberGetsTheCompleteTreeOfItsOwnVersions() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[manifest]",
            "members = [\"lib-a\", \"lib-b\"]",
            "",
            "[[package]]",
            "name = \"lib-a\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"shared\", version = \"1.0.0\" },",
            "]",
            "",
            "[[package]]",
            "name = \"lib-b\"",
            "version = \"2.0.0\"",
            "dependencies = [",
            "    { name = \"shared\", version = \"2.0.0\" },",
            "]",
            "",
            "[[package]]",
            "name = \"shared\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"leaf-old\" },",
            "]",
            "",
            "[[package]]",
            "name = \"shared\"",
            "version = \"2.0.0\"",
            "dependencies = [",
            "    { name = \"leaf-new\" },",
            "]",
            "",
            "[[package]]",
            "name = \"leaf-old\"",
            "version = \"0.1.0\"",
            "",
            "[[package]]",
            "name = \"leaf-new\"",
            "version = \"0.2.0\""
        );

        List<CodeLocation> codeLocations = new UVLockParser(externalIdFactory).parseLockFile(lockContent, "lib-a", createDefaultOptions());

        assertEquals(2, codeLocations.size(), "Expected one code location per workspace member");

        DependencyGraph graphA = findCodeLocation(codeLocations, "lib-a").getDependencyGraph();
        Dependency sharedInA = findDependency(graphA.getRootDependencies(), "shared", "1.0.0");
        assertNotNull(sharedInA, "Expected 'shared' v1.0.0 for lib-a");
        assertEquals(1, graphA.getRootDependencies().size(), "lib-a only depends on shared v1.0.0");
        assertTrue(hasDependency(graphA.getChildrenForParent(sharedInA), "leaf-old", "0.1.0"), "Expected 'leaf-old' under shared v1.0.0 for lib-a");

        DependencyGraph graphB = findCodeLocation(codeLocations, "lib-b").getDependencyGraph();
        Dependency sharedInB = findDependency(graphB.getRootDependencies(), "shared", "2.0.0");
        assertNotNull(sharedInB, "Expected 'shared' v2.0.0 for lib-b");
        assertEquals(1, graphB.getRootDependencies().size(), "lib-b only depends on shared v2.0.0");
        assertTrue(hasDependency(graphB.getChildrenForParent(sharedInB), "leaf-new", "0.2.0"), "Expected 'leaf-new' under shared v2.0.0 for lib-b");
    }

    @Test
    void sameDependencyOfMultipleWorkspaceMembersIsExpandedInEachGraph() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[manifest]",
            "members = [\"lib-a\", \"lib-b\"]",
            "",
            "[[package]]",
            "name = \"lib-a\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"shared\" },",
            "]",
            "",
            "[[package]]",
            "name = \"lib-b\"",
            "version = \"2.0.0\"",
            "dependencies = [",
            "    { name = \"shared\" },",
            "]",
            "",
            "[[package]]",
            "name = \"shared\"",
            "version = \"3.0.0\"",
            "dependencies = [",
            "    { name = \"leaf\" },",
            "]",
            "",
            "[[package]]",
            "name = \"leaf\"",
            "version = \"0.1.0\""
        );

        List<CodeLocation> codeLocations = new UVLockParser(externalIdFactory).parseLockFile(lockContent, "lib-a", createDefaultOptions());

        assertEquals(2, codeLocations.size(), "Expected one code location per workspace member");
        for (String member : Arrays.asList("lib-a", "lib-b")) {
            DependencyGraph graph = findCodeLocation(codeLocations, member).getDependencyGraph();
            Dependency shared = findDependency(graph.getRootDependencies(), "shared", "3.0.0");
            assertNotNull(shared, "Expected 'shared' as root dependency of " + member);
            assertTrue(hasDependency(graph.getChildrenForParent(shared), "leaf", "0.1.0"), "Expected 'leaf' under 'shared' in the graph of " + member);
        }
    }

    @Test
    void localVersionVariantsReferencedByDifferentWorkspaceMembersStaySeparate() {
        String lockContent = String.join("\n",
            "version = 1",
            "",
            "[manifest]",
            "members = [\"lib-a\", \"lib-b\"]",
            "",
            "[[package]]",
            "name = \"lib-a\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"torch\", version = \"2.6.0+cpu\", source = { registry = \"https://download.pytorch.org/whl/cpu\" } },",
            "]",
            "",
            "[[package]]",
            "name = \"lib-b\"",
            "version = \"1.0.0\"",
            "dependencies = [",
            "    { name = \"torch\", version = \"2.6.0+cu124\", source = { registry = \"https://download.pytorch.org/whl/cu124\" } },",
            "]",
            "",
            "[[package]]",
            "name = \"torch\"",
            "version = \"2.6.0+cpu\"",
            "source = { registry = \"https://download.pytorch.org/whl/cpu\" }",
            "dependencies = [",
            "    { name = \"cpu-only-dep\" },",
            "]",
            "",
            "[[package]]",
            "name = \"torch\"",
            "version = \"2.6.0+cu124\"",
            "source = { registry = \"https://download.pytorch.org/whl/cu124\" }",
            "dependencies = [",
            "    { name = \"cuda-only-dep\" },",
            "]",
            "",
            "[[package]]",
            "name = \"cpu-only-dep\"",
            "version = \"1.0.0\"",
            "",
            "[[package]]",
            "name = \"cuda-only-dep\"",
            "version = \"2.0.0\""
        );

        List<CodeLocation> codeLocations = new UVLockParser(externalIdFactory).parseLockFile(lockContent, "lib-a", createDefaultOptions());

        assertEquals(2, codeLocations.size(), "Expected one code location per workspace member");

        // lib-a: only the CPU build of torch and only its own child
        DependencyGraph graphA = findCodeLocation(codeLocations, "lib-a").getDependencyGraph();
        assertEquals(1, graphA.getRootDependencies().size(), "lib-a must only have one root dependency (torch cpu)");
        Dependency torchCpu = findDependency(graphA.getRootDependencies(), "torch", "2.6.0+cpu");
        assertNotNull(torchCpu, "Expected 'torch' v2.6.0+cpu in the graph of lib-a");
        assertEquals("2.6.0+cpu", torchCpu.getExternalId().getVersion(), "Expected the ExternalId of lib-a's torch to carry '+cpu'");
        assertFalse(hasDependency(graphA.getRootDependencies(), "torch", "2.6.0+cu124"), "lib-a must not contain the CUDA build of torch");
        assertFalse(hasDependency(graphA.getRootDependencies(), "torch", "2.6.0"), "The version must not be stripped to '2.6.0'");
        Set<Dependency> torchCpuChildren = graphA.getChildrenForParent(torchCpu);
        assertEquals(1, torchCpuChildren.size(), "torch 2.6.0+cpu must only have its own dependency");
        assertTrue(hasDependency(torchCpuChildren, "cpu-only-dep", "1.0.0"), "Expected 'cpu-only-dep' under torch 2.6.0+cpu");
        assertFalse(hasDependency(torchCpuChildren, "cuda-only-dep", "2.0.0"), "CUDA-only dependency must not leak into lib-a");

        // lib-b: only the CUDA build of torch and only its own child
        DependencyGraph graphB = findCodeLocation(codeLocations, "lib-b").getDependencyGraph();
        assertEquals(1, graphB.getRootDependencies().size(), "lib-b must only have one root dependency (torch cu124)");
        Dependency torchCuda = findDependency(graphB.getRootDependencies(), "torch", "2.6.0+cu124");
        assertNotNull(torchCuda, "Expected 'torch' v2.6.0+cu124 in the graph of lib-b");
        assertEquals("2.6.0+cu124", torchCuda.getExternalId().getVersion(), "Expected the ExternalId of lib-b's torch to carry '+cu124'");
        assertFalse(hasDependency(graphB.getRootDependencies(), "torch", "2.6.0+cpu"), "lib-b must not contain the CPU build of torch");
        assertFalse(hasDependency(graphB.getRootDependencies(), "torch", "2.6.0"), "The version must not be stripped to '2.6.0'");
        Set<Dependency> torchCudaChildren = graphB.getChildrenForParent(torchCuda);
        assertEquals(1, torchCudaChildren.size(), "torch 2.6.0+cu124 must only have its own dependency");
        assertTrue(hasDependency(torchCudaChildren, "cuda-only-dep", "2.0.0"), "Expected 'cuda-only-dep' under torch 2.6.0+cu124");
        assertFalse(hasDependency(torchCudaChildren, "cpu-only-dep", "1.0.0"), "CPU-only dependency must not leak into lib-b");
    }

    private CodeLocation findCodeLocation(List<CodeLocation> codeLocations, String projectName) {
        return codeLocations.stream()
            .filter(codeLocation -> codeLocation.getExternalId().isPresent() && codeLocation.getExternalId().get().getName().equals(projectName))
            .findFirst()
            .orElse(null);
    }

    private boolean hasDependency(Set<Dependency> dependencies, String name, String version) {
        return dependencies.stream()
            .anyMatch(dep -> dep.getName().equals(name) && dep.getVersion().equals(version));
    }

    private Dependency findDependency(Set<Dependency> dependencies, String name) {
        return dependencies.stream()
            .filter(dep -> dep.getName().equals(name))
            .findFirst()
            .orElse(null);
    }

    private Dependency findDependency(Set<Dependency> dependencies, String name, String version) {
        return dependencies.stream()
            .filter(dep -> dep.getName().equals(name) && dep.getVersion().equals(version))
            .findFirst()
            .orElse(null);
    }
}
