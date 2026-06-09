package com.blackduck.integration.detectable.detectables.sbt.unit;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtDotGraphNodeParser;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtGraphParserTransformer;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link SbtGraphParserTransformer}, focusing on SBT dependency eviction handling.
 * <p>
 * SBT represents eviction in DOT graphs with an edge like:
 *   "com.google.guava:guava:27.0-jre" -> "com.google.guava:guava:30.1-jre" [label="Evicted By"]
 * meaning guava 27.0 was evicted in favor of 30.1.
 * <p>
 * The evicted node (27.0) should be excluded from the dependency graph entirely,
 * while the replacement (30.1) and all other legitimate edges must be preserved.
 */
public class SbtGraphParserTransformerTest {

    private final ExternalIdFactory externalIdFactory = new ExternalIdFactory();
    private final SbtDotGraphNodeParser nodeParser = new SbtDotGraphNodeParser(externalIdFactory);
    private final SbtGraphParserTransformer transformer = new SbtGraphParserTransformer(nodeParser);

    private MutableGraph parseDot(String dotContent) throws IOException {
        InputStream stream = new ByteArrayInputStream(dotContent.getBytes(StandardCharsets.UTF_8));
        return new Parser().read(stream);
    }

    @Test
    public void evictedDependencyIsExcludedFromEntireGraph() throws IOException {
        // isOneRoot=true path: single projectId forces the isOneRoot branch.
        // The eviction check in the else-branch ("guava:27.0 -> guava:30.1 [Evicted By]") must
        // prevent the evicted node from being introduced as a graph parent via addChildWithParent.
        // DOT graph with guava 27.0 evicted by 30.1.
        // Edges: eviction marker (27.0 -> 30.1), guice -> 30.1, project -> 30.1, project -> guice.
        // Expected: guava 27.0 excluded, guava 30.1 and guice preserved.
        String dot = "digraph \"dependency-graph\" {\n"
            + "    graph[rankdir=\"LR\"]\n"
            + "    edge [arrowtail=\"none\"]\n"
            + "    \"com.google.guava:guava:27.0-jre\"[shape=box label=<com.google.guava<BR/><B>guava</B><BR/>27.0-jre> style=\"dashed\" penwidth=\"3\"]\n"
            + "    \"com.google.guava:guava:30.1-jre\"[shape=box label=<com.google.guava<BR/><B>guava</B><BR/>30.1-jre> style=\"\"]\n"
            + "    \"com.google.inject:guice:5.1.0\"[shape=box label=<com.google.inject<BR/><B>guice</B><BR/>5.1.0> style=\"\"]\n"
            + "    \"default:myproject:1.0\"[shape=box label=<default<BR/><B>myproject</B><BR/>1.0> style=\"\"]\n"
            + "    \"com.google.guava:guava:27.0-jre\" -> \"com.google.guava:guava:30.1-jre\" [label=\"Evicted By\" style=\"dashed\"]\n"
            + "    \"com.google.inject:guice:5.1.0\" -> \"com.google.guava:guava:30.1-jre\"\n"
            + "    \"default:myproject:1.0\" -> \"com.google.guava:guava:30.1-jre\"\n"
            + "    \"default:myproject:1.0\" -> \"com.google.inject:guice:5.1.0\"\n"
            + "}";

        MutableGraph mutableGraph = parseDot(dot);
        Set<String> projectIds = Collections.singleton("default:myproject:1.0");
        DependencyGraph graph = transformer.transformDotToGraph(projectIds, mutableGraph);

        // The evicted guava 27.0 must NOT appear anywhere in the graph.
        ExternalId evictedId = externalIdFactory.createMavenExternalId("com.google.guava", "guava", "27.0-jre");
        Assertions.assertFalse(graph.hasDependency(evictedId),
            "Evicted guava 27.0-jre should not exist anywhere in the dependency graph");

        // The replacement (guava 30.1) must still be present — it is NOT in evictedIds,
        // so edges like "project -> guava:30.1" and "guice -> guava:30.1" are preserved.
        ExternalId replacementId = externalIdFactory.createMavenExternalId("com.google.guava", "guava", "30.1-jre");
        Assertions.assertTrue(graph.hasDependency(replacementId),
            "Replacement guava 30.1-jre should exist in the dependency graph");

        // guice is unrelated to the eviction and must remain in the graph.
        ExternalId guiceId = externalIdFactory.createMavenExternalId("com.google.inject", "guice", "5.1.0");
        Assertions.assertTrue(graph.hasDependency(guiceId),
            "guice 5.1.0 should exist in the dependency graph");
    }

    @Test
    public void evictionDemoGraphExcludesEvictedAndPreservesDirectDependency() throws IOException {
        // !isOneRoot path: SbtRootNodeFinder returns multiple rootIds for this graph because orphan
        // nodes (transitive deps declared as DOT nodes but with no edges) are also never a link
        // destination, so they land in the root candidate set alongside the real project node.
        // This test simulates that by passing all those node IDs as projectIds, forcing isOneRoot=false.
        //
        // Full sbt-eviction-demo graph: guava 27.0-jre evicted by 30.1-jre, with orphan nodes.
        // Expected: guava 27.0-jre excluded, guice 5.1.0 is a direct dep, project node itself is not a dep.
        String dot = "digraph \"dependency-graph\" {\n"
            + "    graph[rankdir=\"LR\"]\n"
            + "    edge [arrowtail=\"none\"]\n"
            + "    \"aopalliance:aopalliance:1.0\"[shape=box label=<aopalliance<BR/><B>aopalliance</B><BR/>1.0> style=\"\"]\n"
            + "    \"com.google.code.findbugs:jsr305:3.0.2\"[shape=box label=<com.google.code.findbugs<BR/><B>jsr305</B><BR/>3.0.2> style=\"\"]\n"
            + "    \"com.google.errorprone:error_prone_annotations:2.3.4\"[shape=box label=<com.google.errorprone<BR/><B>error_prone_annotations</B><BR/>2.3.4> style=\"\"]\n"
            + "    \"com.google.guava:failureaccess:1.0.1\"[shape=box label=<com.google.guava<BR/><B>failureaccess</B><BR/>1.0.1> style=\"\"]\n"
            + "    \"com.google.guava:guava:27.0-jre\"[shape=box label=<com.google.guava<BR/><B>guava</B><BR/>27.0-jre> style=\"dashed\"]\n"
            + "    \"com.google.guava:guava:30.1-jre\"[shape=box label=<com.google.guava<BR/><B>guava</B><BR/>30.1-jre> style=\"\"]\n"
            + "    \"com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava\"[shape=box label=<com.google.guava<BR/><B>listenablefuture</B><BR/>9999.0-empty-to-avoid-conflict-with-guava> style=\"\"]\n"
            + "    \"com.google.inject:guice:5.1.0\"[shape=box label=<com.google.inject<BR/><B>guice</B><BR/>5.1.0> style=\"\"]\n"
            + "    \"com.google.j2objc:j2objc-annotations:1.3\"[shape=box label=<com.google.j2objc<BR/><B>j2objc-annotations</B><BR/>1.3> style=\"\"]\n"
            + "    \"default:sbt-eviction-demo:1.0\"[shape=box label=<default<BR/><B>sbt-eviction-demo</B><BR/>1.0> style=\"\"]\n"
            + "    \"javax.inject:javax.inject:1\"[shape=box label=<javax.inject<BR/><B>javax.inject</B><BR/>1> style=\"\"]\n"
            + "    \"org.checkerframework:checker-qual:3.5.0\"[shape=box label=<org.checkerframework<BR/><B>checker-qual</B><BR/>3.5.0> style=\"\"]\n"
            + "    \"com.google.guava:guava:27.0-jre\" -> \"com.google.guava:guava:30.1-jre\" [label=\"Evicted By\" style=\"dashed\"]\n"
            + "    \"com.google.inject:guice:5.1.0\" -> \"aopalliance:aopalliance:1.0\"\n"
            + "    \"com.google.inject:guice:5.1.0\" -> \"com.google.guava:guava:30.1-jre\"\n"
            + "    \"com.google.inject:guice:5.1.0\" -> \"javax.inject:javax.inject:1\"\n"
            + "    \"default:sbt-eviction-demo:1.0\" -> \"com.google.guava:guava:30.1-jre\"\n"
            + "    \"default:sbt-eviction-demo:1.0\" -> \"com.google.inject:guice:5.1.0\"\n"
            + "}";

        MutableGraph mutableGraph = parseDot(dot);
        // Mirrors what SbtRootNodeFinder returns for this graph after eviction filtering:
        // all nodes that are never a link destination, minus evicted nodes.
        // The orphan nodes (jsr305, failureaccess, etc.) are never destinations, so they're included.
        Set<String> projectIds = new HashSet<>(Arrays.asList(
            "default:sbt-eviction-demo:1.0",
            "com.google.code.findbugs:jsr305:3.0.2",
            "com.google.errorprone:error_prone_annotations:2.3.4",
            "com.google.guava:failureaccess:1.0.1",
            "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava",
            "com.google.j2objc:j2objc-annotations:1.3",
            "org.checkerframework:checker-qual:3.5.0"
        ));
        DependencyGraph graph = transformer.transformDotToGraph(projectIds, mutableGraph);

        // guava 27.0-jre was evicted and must not appear in the dependency graph.
        ExternalId evictedId = externalIdFactory.createMavenExternalId("com.google.guava", "guava", "27.0-jre");
        Assertions.assertFalse(graph.hasDependency(evictedId),
            "Evicted guava 27.0-jre should not exist anywhere in the dependency graph");

        // guice 5.1.0 is directly declared by the root project and must be a direct dependency.
        // In the !isOneRoot path the old code called addDirectDependency(parent) which added the
        // project node itself as a dep and left guice transitive — this assertion catches that regression.
        ExternalId guiceId = externalIdFactory.createMavenExternalId("com.google.inject", "guice", "5.1.0");
        boolean guiceIsDirectDep = graph.getRootDependencies().stream()
            .anyMatch(dep -> dep.getExternalId().equals(guiceId));
        Assertions.assertTrue(guiceIsDirectDep,
            "com.google.inject:guice:5.1.0 should be a direct dependency of the root project");

        // The project node itself must not appear as a dependency.
        ExternalId projectId = externalIdFactory.createMavenExternalId("default", "sbt-eviction-demo", "1.0");
        Assertions.assertFalse(graph.hasDependency(projectId),
            "The project node itself should not be added as a dependency");
    }

    @Test
    public void multiRootDirectAndTransitiveDependenciesAreClassifiedCorrectly() throws IOException {
        // !isOneRoot path: two projectIds (project + orphan node) force isOneRoot=false.
        // Verifies:
        //   - project->dep edge: child is added as a direct dependency (if branch)
        //   - dep->dep edge: child is added as transitive under its parent (else-if branch)
        String dot = "digraph \"dependency-graph\" {\n"
            + "    graph[rankdir=\"LR\"]\n"
            + "    \"default:myproject:1.0\"[shape=box style=\"\"]\n"
            + "    \"some:orphan:1.0\"[shape=box style=\"\"]\n"
            + "    \"com.google.inject:guice:5.1.0\"[shape=box style=\"\"]\n"
            + "    \"com.google.guava:guava:30.1-jre\"[shape=box style=\"\"]\n"
            + "    \"default:myproject:1.0\" -> \"com.google.inject:guice:5.1.0\"\n"
            + "    \"com.google.inject:guice:5.1.0\" -> \"com.google.guava:guava:30.1-jre\"\n"
            + "}";

        MutableGraph mutableGraph = parseDot(dot);
        Set<String> projectIds = new HashSet<>(Arrays.asList("default:myproject:1.0", "some:orphan:1.0"));
        DependencyGraph graph = transformer.transformDotToGraph(projectIds, mutableGraph);

        ExternalId guiceId = externalIdFactory.createMavenExternalId("com.google.inject", "guice", "5.1.0");
        boolean guiceIsDirectDep = graph.getRootDependencies().stream()
            .anyMatch(dep -> dep.getExternalId().equals(guiceId));
        Assertions.assertTrue(guiceIsDirectDep,
            "guice should be a direct dependency (project root -> guice edge)");

        ExternalId guavaId = externalIdFactory.createMavenExternalId("com.google.guava", "guava", "30.1-jre");
        Assertions.assertTrue(graph.hasDependency(guavaId),
            "guava should be present in the graph as a transitive dep");
        boolean guavaIsDirectDep = graph.getRootDependencies().stream()
            .anyMatch(dep -> dep.getExternalId().equals(guavaId));
        Assertions.assertFalse(guavaIsDirectDep,
            "guava should not be a direct dependency (it is transitive under guice)");
    }

    @Test
    public void evictedDependencyNotAddedAsDirectDependency() throws IOException {
        // Verifies eviction works even when the evicted node's only edge is the eviction marker.
        // DOT graph: lib 1.0 evicted by lib 2.0, project depends on lib 2.0.
        // Expected: lib 1.0 excluded, lib 2.0 is the sole direct dependency.
        String dot = "digraph \"dependency-graph\" {\n"
            + "    graph[rankdir=\"LR\"]\n"
            + "    edge [arrowtail=\"none\"]\n"
            + "    \"org.example:lib:1.0\"[shape=box label=<org.example<BR/><B>lib</B><BR/>1.0> style=\"dashed\"]\n"
            + "    \"org.example:lib:2.0\"[shape=box label=<org.example<BR/><B>lib</B><BR/>2.0> style=\"\"]\n"
            + "    \"default:myproject:1.0\"[shape=box label=<default<BR/><B>myproject</B><BR/>1.0> style=\"\"]\n"
            + "    \"org.example:lib:1.0\" -> \"org.example:lib:2.0\" [label=\"Evicted By\" style=\"dashed\"]\n"
            + "    \"default:myproject:1.0\" -> \"org.example:lib:2.0\"\n"
            + "}";

        MutableGraph mutableGraph = parseDot(dot);
        Set<String> projectIds = Collections.singleton("default:myproject:1.0");
        DependencyGraph graph = transformer.transformDotToGraph(projectIds, mutableGraph);

        // The evicted lib 1.0 must not appear anywhere (not as root, not as transitive).
        ExternalId evictedId = externalIdFactory.createMavenExternalId("org.example", "lib", "1.0");
        Assertions.assertFalse(graph.hasDependency(evictedId),
            "Evicted lib 1.0 should not exist anywhere in the dependency graph");

        // Only the replacement lib 2.0 should remain as a direct dependency.
        Assertions.assertEquals(1, graph.getRootDependencies().size());
        Dependency rootDep = graph.getRootDependencies().iterator().next();
        Assertions.assertEquals("lib", rootDep.getName());
        Assertions.assertEquals("2.0", rootDep.getVersion());
    }
}
