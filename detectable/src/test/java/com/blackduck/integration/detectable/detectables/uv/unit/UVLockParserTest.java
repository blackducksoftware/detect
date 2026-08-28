package com.blackduck.integration.detectable.detectables.uv.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void normalizeVersionWithPlusSuffix() {
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
        assertTrue(hasDependency(graph.getRootDependencies(), "torch", "2.0.0"), "Expected version '2.0.0' after stripping '+cu118' suffix");
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
}
