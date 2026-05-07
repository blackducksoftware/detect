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
import com.blackduck.integration.bdio.model.Forge;
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

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size());
        assertTrue(hasDependency(graph.getRootDependencies(), "requests", "2.31.0"));
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

        assertEquals(2, codeLocations.size());
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

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(2, graph.getRootDependencies().size());
        assertTrue(hasDependency(graph.getRootDependencies(), "requests", "2.31.0"));
        assertTrue(hasDependency(graph.getRootDependencies(), "pytest", "7.4.0"));
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

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size());
        assertTrue(hasDependency(graph.getRootDependencies(), "requests", "2.31.0"));
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

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(2, graph.getRootDependencies().size());
        assertTrue(hasDependency(graph.getRootDependencies(), "requests", "2.31.0"));
        assertTrue(hasDependency(graph.getRootDependencies(), "boto3", "1.28.0"));
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

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size());
        assertTrue(hasDependency(graph.getRootDependencies(), "requests", "2.31.0"));
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

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertTrue(hasDependency(graph.getRootDependencies(), "torch", "2.0.0"));
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

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertTrue(hasDependency(graph.getRootDependencies(), "some-package", "1.0.0"));
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

        assertEquals(1, codeLocations.size());
        assertEquals("lib-a", codeLocations.get(0).getExternalId().get().getName());
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

        assertEquals(1, codeLocations.size());
        assertEquals("lib-a", codeLocations.get(0).getExternalId().get().getName());
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

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertTrue(hasDependency(graph.getRootDependencies(), "no-version-pkg", "defaultVersion"));
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

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();

        assertEquals(1, graph.getRootDependencies().size());
        assertTrue(hasDependency(graph.getRootDependencies(), "requests", "2.31.0"));

        Dependency requestsDep = findDependency(graph.getRootDependencies(), "requests");
        Set<Dependency> requestsChildren = graph.getChildrenForParent(requestsDep);
        assertEquals(2, requestsChildren.size());
        assertTrue(hasDependency(requestsChildren, "urllib3", "2.0.4"));
        assertTrue(hasDependency(requestsChildren, "charset-normalizer", "3.2.0"));
    }

    private UVDetectorOptions createDefaultOptions() {
        return new UVDetectorOptions(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );
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
