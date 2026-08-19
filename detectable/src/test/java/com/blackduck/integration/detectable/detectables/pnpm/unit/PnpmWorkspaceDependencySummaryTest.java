package com.blackduck.integration.detectable.detectables.pnpm.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.dependency.DependencyFactory;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmWorkspaceDependencySummary;

class PnpmWorkspaceDependencySummaryTest {

    private final DependencyFactory dependencyFactory = new DependencyFactory(new ExternalIdFactory());
    private final Forge forge = Forge.NPMJS;

    private Dependency dep(String name, String version) {
        return dependencyFactory.createNameVersionDependency(forge, name, version);
    }

    /** An empty graph (no deps at all) should produce all-zero counts and an empty list. */
    @Test
    void testEmptyGraph() {
        DependencyGraph graph = new BasicDependencyGraph();

        PnpmWorkspaceDependencySummary summary = PnpmWorkspaceDependencySummary.from(graph, true);

        assertEquals(0, summary.directCount);
        assertEquals(0, summary.transitiveCount);
        assertTrue(summary.depNames.isEmpty());
    }

    /** A graph with only root-level deps and no children: direct=2, transitive=0. */
    @Test
    void testOnlyDirectDependencies() {
        DependencyGraph graph = new BasicDependencyGraph();
        graph.addChildrenToRoot(dep("react", "18.0.0"), dep("lodash", "4.17.21"));

        PnpmWorkspaceDependencySummary summary = PnpmWorkspaceDependencySummary.from(graph, true);

        assertEquals(2, summary.directCount);
        assertEquals(0, summary.transitiveCount);
        assertEquals(2, summary.depNames.size());
        assertTrue(summary.depNames.contains("lodash@4.17.21"));
        assertTrue(summary.depNames.contains("react@18.0.0"));
    }

    /** A graph with one direct dep that itself has one child: direct=1, transitive=1. */
    @Test
    void testDirectAndTransitiveDependencies() {
        Dependency webpack = dep("webpack", "5.0.0");
        Dependency acorn = dep("acorn", "8.0.0");

        DependencyGraph graph = new BasicDependencyGraph();
        graph.addChildrenToRoot(webpack);
        graph.addParentWithChild(webpack, acorn);

        PnpmWorkspaceDependencySummary summary = PnpmWorkspaceDependencySummary.from(graph, true);

        assertEquals(1, summary.directCount);
        assertEquals(1, summary.transitiveCount);
        assertEquals(2, summary.depNames.size());
        assertTrue(summary.depNames.contains("webpack@5.0.0"));
        assertTrue(summary.depNames.contains("acorn@8.0.0"));
    }

    /**
     * Diamond dependency: A and B are both direct deps, and both depend on D.
     * D must be counted exactly ONCE — proves the HashSet dedup in BFS works.
     * root → A → D
     * root → B → D
     * Expected: directCount=2, transitiveCount=1, total depNames size=3.
     */
    @Test
    void testDiamondDependencyIsCountedOnce() {
        Dependency a = dep("pkg-a", "1.0.0");
        Dependency b = dep("pkg-b", "1.0.0");
        Dependency d = dep("shared-dep", "1.0.0");

        DependencyGraph graph = new BasicDependencyGraph();
        graph.addChildrenToRoot(a, b);
        graph.addParentWithChild(a, d);
        graph.addParentWithChild(b, d);

        PnpmWorkspaceDependencySummary summary = PnpmWorkspaceDependencySummary.from(graph, true);

        assertEquals(2, summary.directCount, "Expected 2 direct deps (pkg-a, pkg-b)");
        assertEquals(1, summary.transitiveCount, "Expected 1 transitive dep (shared-dep counted once, not twice)");
        assertEquals(3, summary.depNames.size(), "Expected 3 total unique deps");
        assertTrue(summary.depNames.contains("shared-dep@1.0.0"));
    }

    /** The depNames list must be sorted alphabetically so log output is stable and readable. */
    @Test
    void testDepNamesAreSortedAlphabetically() {
        DependencyGraph graph = new BasicDependencyGraph();
        graph.addChildrenToRoot(
            dep("zlib", "1.0.0"),
            dep("acorn", "1.0.0"),
            dep("moment", "1.0.0")
        );

        PnpmWorkspaceDependencySummary summary = PnpmWorkspaceDependencySummary.from(graph, true);

        List<String> names = summary.depNames;
        assertEquals(3, names.size());
        assertEquals("acorn@1.0.0", names.get(0));
        assertEquals("moment@1.0.0", names.get(1));
        assertEquals("zlib@1.0.0", names.get(2));
    }
}
