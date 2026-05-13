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
import com.blackduck.integration.detectable.detectables.uv.transform.UVTreeDependencyGraphTransformer;

class UVTreeDependencyGraphTransformerTest {

    private ExternalIdFactory externalIdFactory;

    @BeforeEach
    void setUp() {
        externalIdFactory = new ExternalIdFactory();
    }

    @Test
    void transformSimpleTree() {
        List<String> treeOutput = Arrays.asList(
            "my-project v1.0.0",
            "├── requests v2.31.0",
            "└── click v8.1.0"
        );

        UVTreeDependencyGraphTransformer transformer = new UVTreeDependencyGraphTransformer(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = transformer.transform(treeOutput, options);

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(2, graph.getRootDependencies().size());
        assertTrue(hasDependency(graph.getRootDependencies(), "requests", "2.31.0"));
        assertTrue(hasDependency(graph.getRootDependencies(), "click", "8.1.0"));
    }

    @Test
    void transformTreeWithTransitiveDependencies() {
        List<String> treeOutput = Arrays.asList(
            "my-project v1.0.0",
            "├── requests v2.31.0",
            "│   ├── urllib3 v2.0.4",
            "│   └── charset-normalizer v3.2.0",
            "└── click v8.1.0"
        );

        UVTreeDependencyGraphTransformer transformer = new UVTreeDependencyGraphTransformer(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = transformer.transform(treeOutput, options);

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();

        assertEquals(2, graph.getRootDependencies().size());

        Dependency requestsDep = findDependency(graph.getRootDependencies(), "requests");
        Set<Dependency> requestsChildren = graph.getChildrenForParent(requestsDep);
        assertEquals(2, requestsChildren.size());
        assertTrue(hasDependency(requestsChildren, "urllib3", "2.0.4"));
        assertTrue(hasDependency(requestsChildren, "charset-normalizer", "3.2.0"));
    }

    @Test
    void transformTreeWithDeeplyNestedDependencies() {
        List<String> treeOutput = Arrays.asList(
            "my-project v1.0.0",
            "├── requests v2.31.0",
            "│   ├── urllib3 v2.0.4",
            "│   │   └── h2 v4.1.0",
            "│   │       └── hpack v4.0.0",
            "│   └── certifi v2023.7.22"
        );

        UVTreeDependencyGraphTransformer transformer = new UVTreeDependencyGraphTransformer(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = transformer.transform(treeOutput, options);

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();

        assertEquals(1, graph.getRootDependencies().size());

        Dependency requestsDep = findDependency(graph.getRootDependencies(), "requests");
        Set<Dependency> requestsChildren = graph.getChildrenForParent(requestsDep);
        assertEquals(2, requestsChildren.size());

        Dependency urllib3Dep = findDependency(requestsChildren, "urllib3");
        Set<Dependency> urllib3Children = graph.getChildrenForParent(urllib3Dep);
        assertEquals(1, urllib3Children.size());
        assertTrue(hasDependency(urllib3Children, "h2", "4.1.0"));

        Dependency h2Dep = findDependency(urllib3Children, "h2");
        Set<Dependency> h2Children = graph.getChildrenForParent(h2Dep);
        assertEquals(1, h2Children.size());
        assertTrue(hasDependency(h2Children, "hpack", "4.0.0"));
    }

    @Test
    void transformTreeWithMultipleWorkspaceMembers() {
        List<String> treeOutput = Arrays.asList(
            "lib-a v1.0.0",
            "├── requests v2.31.0",
            "lib-b v2.0.0",
            "├── click v8.1.0"
        );

        UVTreeDependencyGraphTransformer transformer = new UVTreeDependencyGraphTransformer(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = transformer.transform(treeOutput, options);

        assertEquals(2, codeLocations.size());
    }

    @Test
    void transformTreeStripsBrackets() {
        List<String> treeOutput = Arrays.asList(
            "my-project v1.0.0",
            "├── dbgpt[agent, cli, client] v0.7.0"
        );

        UVTreeDependencyGraphTransformer transformer = new UVTreeDependencyGraphTransformer(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = transformer.transform(treeOutput, options);

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size());
        assertTrue(hasDependency(graph.getRootDependencies(), "dbgpt", "0.7.0"));
    }

    @Test
    void transformTreeStripsParentheses() {
        List<String> treeOutput = Arrays.asList(
            "my-project v1.0.0",
            "├── pytest v8.3.4 (group: dev)"
        );

        UVTreeDependencyGraphTransformer transformer = new UVTreeDependencyGraphTransformer(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = transformer.transform(treeOutput, options);

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size());
        assertTrue(hasDependency(graph.getRootDependencies(), "pytest", "8.3.4"));
    }

    @Test
    void transformTreeStripsBracketsAndParentheses() {
        List<String> treeOutput = Arrays.asList(
            "my-project v1.0.0",
            "├── dbgpt[agent, cli] v0.7.0 (extra: base)"
        );

        UVTreeDependencyGraphTransformer transformer = new UVTreeDependencyGraphTransformer(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = transformer.transform(treeOutput, options);

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size());
        assertTrue(hasDependency(graph.getRootDependencies(), "dbgpt", "0.7.0"));
    }

    @Test
    void transformTreeNormalizesPackageNames() {
        List<String> treeOutput = Arrays.asList(
            "my-project v1.0.0",
            "├── some_package v1.0.0",
            "├── Another.Package v2.0.0",
            "├── UPPER_CASE v3.0.0"
        );

        UVTreeDependencyGraphTransformer transformer = new UVTreeDependencyGraphTransformer(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = transformer.transform(treeOutput, options);

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(3, graph.getRootDependencies().size());
        assertTrue(hasDependency(graph.getRootDependencies(), "some-package", "1.0.0"));
        assertTrue(hasDependency(graph.getRootDependencies(), "another-package", "2.0.0"));
        assertTrue(hasDependency(graph.getRootDependencies(), "upper-case", "3.0.0"));
    }

    @Test
    void transformTreeHandlesEmptyLines() {
        List<String> treeOutput = Arrays.asList(
            "my-project v1.0.0",
            "",
            "├── requests v2.31.0",
            "",
            "└── click v8.1.0"
        );

        UVTreeDependencyGraphTransformer transformer = new UVTreeDependencyGraphTransformer(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = transformer.transform(treeOutput, options);

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(2, graph.getRootDependencies().size());
    }

    @Test
    void transformTreeExcludesWorkspaceMemberDependency() {
        List<String> treeOutput = Arrays.asList(
            "lib-a v1.0.0",
            "├── lib-b v2.0.0",
            "│   └── nested-dep v1.0.0",
            "├── requests v2.31.0"
        );

        UVTreeDependencyGraphTransformer transformer = new UVTreeDependencyGraphTransformer(externalIdFactory);
        // Note: includedDependencyGroups (first param) is only supported for UV Build Detector (CLI mode).
        // Support for lockfile/tree transformer will be added later along with corresponding tests.
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),  // includedDependencyGroups - not yet supported for tree transformer
            Collections.emptyList(),
            Collections.emptyList(),
            Arrays.asList("lib-b")
        );

        List<CodeLocation> codeLocations = transformer.transform(treeOutput, options);

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(1, graph.getRootDependencies().size());
        assertTrue(hasDependency(graph.getRootDependencies(), "requests", "2.31.0"));
    }

    @Test
    void includedWorkspaceMembersFiltersDependenciesNotCodeLocations() {
        List<String> treeOutput = Arrays.asList(
            "main-app v1.0.0",
            "├── lib-a v1.0.0",
            "├── lib-b v2.0.0",
            "├── requests v2.31.0",
            "lib-a v1.0.0",
            "├── click v8.0.0",
            "lib-b v2.0.0",
            "├── flask v2.0.0"
        );

        UVTreeDependencyGraphTransformer transformer = new UVTreeDependencyGraphTransformer(externalIdFactory);
        // Note: includedDependencyGroups (first param) is only supported for UV Build Detector (CLI mode).
        // Support for lockfile/tree transformer will be added later along with corresponding tests.
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),  // includedDependencyGroups - not yet supported for tree transformer
            Collections.emptyList(),
            Arrays.asList("main-app", "lib-a"),  // Only include 2 workspace members
            Collections.emptyList()
        );

        List<CodeLocation> codeLocations = transformer.transform(treeOutput, options);

        // All 3 workspace members (main-app, lib-a, lib-b) create code locations.
        // The includedWorkspaceMembers setting only filters which members have their dependencies
        // populated in the graph - excluded members still get code locations created (with empty/minimal graphs).
        assertEquals(3, codeLocations.size());

        DependencyGraph mainGraph = codeLocations.get(0).getDependencyGraph();
        assertEquals(2, mainGraph.getRootDependencies().size());
        assertTrue(hasDependency(mainGraph.getRootDependencies(), "lib-a", "1.0.0"));
        assertTrue(hasDependency(mainGraph.getRootDependencies(), "requests", "2.31.0"));
    }

    @Test
    void transformTreeHandlesMissingVersion() {
        List<String> treeOutput = Arrays.asList(
            "my-project",
            "├── requests v2.31.0"
        );

        UVTreeDependencyGraphTransformer transformer = new UVTreeDependencyGraphTransformer(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = transformer.transform(treeOutput, options);

        assertEquals(1, codeLocations.size());
        assertEquals("defaultVersion", codeLocations.get(0).getExternalId().get().getVersion());
    }

    @Test
    void transformTreeHandlesAsteriskReference() {
        List<String> treeOutput = Arrays.asList(
            "my-project v1.0.0",
            "├── requests v2.31.0",
            "│   └── urllib3 v2.0.4",
            "├── httpx v0.24.0",
            "│   └── urllib3 v2.0.4 (*)"
        );

        UVTreeDependencyGraphTransformer transformer = new UVTreeDependencyGraphTransformer(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = transformer.transform(treeOutput, options);

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(2, graph.getRootDependencies().size());
    }

    @Test
    void transformTreeWithSpaceIndentation() {
        List<String> treeOutput = Arrays.asList(
            "my-project v1.0.0",
            "├── requests v2.31.0",
            "    └── urllib3 v2.0.4"
        );

        UVTreeDependencyGraphTransformer transformer = new UVTreeDependencyGraphTransformer(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = transformer.transform(treeOutput, options);

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();

        Dependency requestsDep = findDependency(graph.getRootDependencies(), "requests");
        Set<Dependency> requestsChildren = graph.getChildrenForParent(requestsDep);
        assertEquals(1, requestsChildren.size());
        assertTrue(hasDependency(requestsChildren, "urllib3", "2.0.4"));
    }

    @Test
    void transformTreeWithNoDependencies() {
        List<String> treeOutput = Arrays.asList(
            "empty-project v1.0.0"
        );

        UVTreeDependencyGraphTransformer transformer = new UVTreeDependencyGraphTransformer(externalIdFactory);
        UVDetectorOptions options = createDefaultOptions();

        List<CodeLocation> codeLocations = transformer.transform(treeOutput, options);

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        assertEquals(0, graph.getRootDependencies().size());
    }

    private UVDetectorOptions createDefaultOptions() {
        // Note: includedDependencyGroups (first param) is only supported for UV Build Detector (CLI mode).
        // Support for lockfile/tree transformer will be added later along with corresponding tests.
        return new UVDetectorOptions(
            Collections.emptyList(),  // includedDependencyGroups - not yet supported for tree transformer
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
