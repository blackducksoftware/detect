package com.blackduck.integration.detectable.detectables.sbt.unit;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtDotGraphNodeParser;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtGraphParserTransformer;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtRootNodeFinder;
import com.blackduck.integration.detectable.util.graph.MavenGraphAssert;
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
 * The dot graphs in these tests mirror the exact output of sbt's dependencyDot task (sbt-dependency-graph plugin,
 * built into sbt 1.4+ via addDependencyTreePlugin). Eviction facts about that output:
 * - An evicted module keeps its node, marked only by style ("stroke-dasharray: 5,5" before sbt 1.9.1, "dashed" after).
 * - The renderer adds a synthetic edge "evictedVersion" -> "winningVersion" [label="Evicted By"], which is not a dependency.
 * - Parents point at the version they requested (the evicted node); the parent -> winner edge is removed by the renderer,
 *   so the winner may only be reachable through the "Evicted By" edge.
 * - Under Coursier (sbt 1.3+ default), a directly-declared dependency that loses resolution becomes a dangling evicted
 *   node with no incoming edges, while the project points straight at the winner.
 */
public class SbtGraphParserTransformerTest {
    private static final String PROJECT = "com.example:project_2.13:1.0.0";

    private MutableGraph createMutableGraph(String actualGraph) throws IOException {
        String simpleGraph = "digraph \"dependency-graph\" {\n"
            + "    graph[rankdir=\"LR\"]\n"
            + "    edge [\n"
            + "        arrowtail=\"none\"\n"
            + "    ]\n"
            + actualGraph + "\n"
            + "\n"
            + "}";
        InputStream stream = new ByteArrayInputStream(simpleGraph.getBytes(StandardCharsets.UTF_8));
        return new Parser().read(stream);
    }

    private String node(String gav) {
        String[] pieces = gav.split(":", 3);
        return "    \"" + gav + "\"[label=<" + pieces[0] + "<BR/><B>" + pieces[1] + "</B><BR/>" + pieces[2] + "> style=\"\"]\n";
    }

    private String evictedNode(String gav) {
        String[] pieces = gav.split(":", 3);
        return "    \"" + gav + "\"[label=<" + pieces[0] + "<BR/><B>" + pieces[1] + "</B><BR/>" + pieces[2] + "> style=\"stroke-dasharray: 5,5\"]\n";
    }

    private String edge(String fromGav, String toGav) {
        return "    \"" + fromGav + "\" -> \"" + toGav + "\"\n";
    }

    private String evictedByEdge(String fromGav, String toGav) {
        return "    \"" + fromGav + "\" -> \"" + toGav + "\" [label=\"Evicted By\" style=\"stroke-dasharray: 5,5\"]\n";
    }

    // sbt 1.9.1+ writes nodes with shape/penwidth/color attributes and uses the "dashed" style for evictions.
    private String sbt191Node(String gav, boolean evicted) {
        String[] pieces = gav.split(":", 3);
        String style = evicted ? "dashed" : "";
        String penwidth = evicted ? "3" : "5";
        return "    \"" + gav + "\"[shape=box label=<" + pieces[0] + "<BR/><B>" + pieces[1] + "</B><BR/>" + pieces[2] + "> style=\"" + style + "\" penwidth=\""
            + penwidth + "\" color=\"#8A0B16\"]\n";
    }

    private String sbt191EvictedByEdge(String fromGav, String toGav) {
        return "    \"" + fromGav + "\" -> \"" + toGav + "\" [label=\"Evicted By\" style=\"dashed\"]\n";
    }

    private MavenGraphAssert transform(Set<String> projectIds, String... graphLines) throws IOException {
        MutableGraph mutableGraph = createMutableGraph(String.join("", graphLines));
        SbtGraphParserTransformer transformer = new SbtGraphParserTransformer(new SbtDotGraphNodeParser(new ExternalIdFactory()));
        DependencyGraph graph = transformer.transformDotToGraph(projectIds, mutableGraph);
        return new MavenGraphAssert(graph);
    }

    private MavenGraphAssert transformSingleRoot(String... graphLines) throws IOException {
        return transform(Collections.singleton(PROJECT), graphLines);
    }

    @Test
    public void graphWithoutEvictionsIsUnchanged() throws IOException {
        MavenGraphAssert graphAssert = transformSingleRoot(
            node(PROJECT),
            node("com.typesafe:config:1.4.0"),
            node("org.slf4j:slf4j-api:1.7.32"),
            edge(PROJECT, "com.typesafe:config:1.4.0"),
            edge("com.typesafe:config:1.4.0", "org.slf4j:slf4j-api:1.7.32")
        );
        graphAssert.hasRootSize(1);
        graphAssert.hasRootDependency("com.typesafe:config:1.4.0");
        graphAssert.hasParentChildRelationship(
            graphAssert.hasDependency("com.typesafe:config:1.4.0"),
            graphAssert.hasDependency("org.slf4j:slf4j-api:1.7.32")
        );
    }

    @Test
    public void evictedDirectDependencyReplacedByWinner() throws IOException {
        // Ivy resolution: the project declared slf4j-api 1.7.25 but 1.7.32 won; the project's edge points at the evicted version.
        MavenGraphAssert graphAssert = transformSingleRoot(
            node(PROJECT),
            evictedNode("org.slf4j:slf4j-api:1.7.25"),
            node("org.slf4j:slf4j-api:1.7.32"),
            edge(PROJECT, "org.slf4j:slf4j-api:1.7.25"),
            evictedByEdge("org.slf4j:slf4j-api:1.7.25", "org.slf4j:slf4j-api:1.7.32")
        );
        graphAssert.hasNoDependency("org.slf4j:slf4j-api:1.7.25");
        graphAssert.hasRootDependency("org.slf4j:slf4j-api:1.7.32");
        graphAssert.hasRootSize(1);
    }

    @Test
    public void evictedTransitiveDependencyReplacedByWinner() throws IOException {
        // fansi requested sourcecode 0.2.1, which was evicted by 0.2.3; fansi must end up depending on 0.2.3.
        MavenGraphAssert graphAssert = transformSingleRoot(
            node(PROJECT),
            node("com.lihaoyi:fansi_2.13:0.2.9"),
            evictedNode("com.lihaoyi:sourcecode_2.13:0.2.1"),
            node("com.lihaoyi:sourcecode_2.13:0.2.3"),
            edge(PROJECT, "com.lihaoyi:fansi_2.13:0.2.9"),
            edge("com.lihaoyi:fansi_2.13:0.2.9", "com.lihaoyi:sourcecode_2.13:0.2.1"),
            evictedByEdge("com.lihaoyi:sourcecode_2.13:0.2.1", "com.lihaoyi:sourcecode_2.13:0.2.3")
        );
        graphAssert.hasNoDependency("com.lihaoyi:sourcecode_2.13:0.2.1");
        graphAssert.hasParentChildRelationship(
            graphAssert.hasDependency("com.lihaoyi:fansi_2.13:0.2.9"),
            graphAssert.hasDependency("com.lihaoyi:sourcecode_2.13:0.2.3")
        );
    }

    @Test
    public void evictedByEdgeIsNotTreatedAsDependencyEdge() throws IOException {
        // Coursier (sbt 1.3+ default): a directly-declared loser dangles with no incoming edges while the project
        // points straight at the winner. The "Evicted By" edge must not re-introduce the evicted node into the graph.
        MavenGraphAssert graphAssert = transformSingleRoot(
            node(PROJECT),
            evictedNode("org.slf4j:slf4j-api:1.7.25"),
            node("org.slf4j:slf4j-api:1.7.32"),
            edge(PROJECT, "org.slf4j:slf4j-api:1.7.32"),
            evictedByEdge("org.slf4j:slf4j-api:1.7.25", "org.slf4j:slf4j-api:1.7.32")
        );
        graphAssert.hasNoDependency("org.slf4j:slf4j-api:1.7.25");
        graphAssert.hasRootDependency("org.slf4j:slf4j-api:1.7.32");
        graphAssert.hasRootSize(1);
    }

    @Test
    public void multipleVersionsEvictedBySameWinner() throws IOException {
        // Mirrors the scalafmt battery fixture: 0.2.1 and 0.1.7 both evicted by 0.2.3, each via several callers.
        MavenGraphAssert graphAssert = transformSingleRoot(
            node(PROJECT),
            node("com.lihaoyi:fansi_2.13:0.2.9"),
            node("org.scalameta:fastparse_2.13:1.0.1"),
            node("org.scalameta:common_2.13:4.4.10"),
            evictedNode("com.lihaoyi:sourcecode_2.13:0.2.1"),
            evictedNode("com.lihaoyi:sourcecode_2.13:0.1.7"),
            node("com.lihaoyi:sourcecode_2.13:0.2.3"),
            edge(PROJECT, "com.lihaoyi:fansi_2.13:0.2.9"),
            edge(PROJECT, "org.scalameta:fastparse_2.13:1.0.1"),
            edge(PROJECT, "org.scalameta:common_2.13:4.4.10"),
            edge("com.lihaoyi:fansi_2.13:0.2.9", "com.lihaoyi:sourcecode_2.13:0.2.1"),
            edge("org.scalameta:fastparse_2.13:1.0.1", "com.lihaoyi:sourcecode_2.13:0.1.7"),
            edge("org.scalameta:common_2.13:4.4.10", "com.lihaoyi:sourcecode_2.13:0.2.3"),
            evictedByEdge("com.lihaoyi:sourcecode_2.13:0.2.1", "com.lihaoyi:sourcecode_2.13:0.2.3"),
            evictedByEdge("com.lihaoyi:sourcecode_2.13:0.1.7", "com.lihaoyi:sourcecode_2.13:0.2.3")
        );
        graphAssert.hasNoDependency("com.lihaoyi:sourcecode_2.13:0.2.1");
        graphAssert.hasNoDependency("com.lihaoyi:sourcecode_2.13:0.1.7");
        graphAssert.hasParentChildRelationship(
            graphAssert.hasDependency("com.lihaoyi:fansi_2.13:0.2.9"),
            graphAssert.hasDependency("com.lihaoyi:sourcecode_2.13:0.2.3")
        );
        graphAssert.hasParentChildRelationship(
            graphAssert.hasDependency("org.scalameta:fastparse_2.13:1.0.1"),
            graphAssert.hasDependency("com.lihaoyi:sourcecode_2.13:0.2.3")
        );
        graphAssert.hasParentChildRelationship(
            graphAssert.hasDependency("org.scalameta:common_2.13:4.4.10"),
            graphAssert.hasDependency("com.lihaoyi:sourcecode_2.13:0.2.3")
        );
    }

    @Test
    public void evictionToLowerVersionIsHonored() throws IOException {
        // Ivy force() / strict conflict managers can evict to an OLDER version; "evicted by" is not always an upgrade.
        MavenGraphAssert graphAssert = transformSingleRoot(
            node(PROJECT),
            node("ch.qos.logback:logback-classic:1.2.11"),
            evictedNode("org.slf4j:slf4j-api:1.7.32"),
            node("org.slf4j:slf4j-api:1.7.25"),
            edge(PROJECT, "ch.qos.logback:logback-classic:1.2.11"),
            edge(PROJECT, "org.slf4j:slf4j-api:1.7.25"),
            edge("ch.qos.logback:logback-classic:1.2.11", "org.slf4j:slf4j-api:1.7.32"),
            evictedByEdge("org.slf4j:slf4j-api:1.7.32", "org.slf4j:slf4j-api:1.7.25")
        );
        graphAssert.hasNoDependency("org.slf4j:slf4j-api:1.7.32");
        graphAssert.hasRootDependency("org.slf4j:slf4j-api:1.7.25");
        graphAssert.hasParentChildRelationship(
            graphAssert.hasDependency("ch.qos.logback:logback-classic:1.2.11"),
            graphAssert.hasDependency("org.slf4j:slf4j-api:1.7.25")
        );
    }

    @Test
    public void evictedNodeWithRangeVersionIsExcluded() throws IOException {
        // Coursier keeps the raw requested range in the evicted node id, and the range node's caller edge can come
        // from an id that has no node declaration anywhere in the file.
        MavenGraphAssert graphAssert = transformSingleRoot(
            node(PROJECT),
            evictedNode("org.slf4j:slf4j-api:[1.7.20,1.7.30)"),
            node("org.slf4j:slf4j-api:1.7.32"),
            edge(PROJECT, "org.slf4j:slf4j-api:1.7.32"),
            edge("org.slf4j:slf4j-api:1.7.29", "org.slf4j:slf4j-api:[1.7.20,1.7.30)"),
            evictedByEdge("org.slf4j:slf4j-api:[1.7.20,1.7.30)", "org.slf4j:slf4j-api:1.7.32")
        );
        graphAssert.hasNoDependency("org.slf4j:slf4j-api:[1.7.20,1.7.30)");
        graphAssert.hasRootDependency("org.slf4j:slf4j-api:1.7.32");
    }

    @Test
    public void evictedNodeStaleOutgoingEdgesAreDropped() throws IOException {
        // Defensive: if an evicted node still has ordinary outgoing edges (seen in older plugin output), that subtree
        // belongs to the discarded version and must not be attached through the evicted node.
        MavenGraphAssert graphAssert = transformSingleRoot(
            node(PROJECT),
            node("com.lihaoyi:fansi_2.13:0.2.9"),
            evictedNode("com.lihaoyi:sourcecode_2.13:0.2.1"),
            node("com.lihaoyi:sourcecode_2.13:0.2.3"),
            node("org.example:stale-child:9.9.9"),
            edge(PROJECT, "com.lihaoyi:fansi_2.13:0.2.9"),
            edge("com.lihaoyi:fansi_2.13:0.2.9", "com.lihaoyi:sourcecode_2.13:0.2.1"),
            edge("com.lihaoyi:sourcecode_2.13:0.2.1", "org.example:stale-child:9.9.9"),
            evictedByEdge("com.lihaoyi:sourcecode_2.13:0.2.1", "com.lihaoyi:sourcecode_2.13:0.2.3")
        );
        graphAssert.hasNoDependency("com.lihaoyi:sourcecode_2.13:0.2.1");
        graphAssert.hasNoDependency("org.example:stale-child:9.9.9");
        graphAssert.hasParentChildRelationship(
            graphAssert.hasDependency("com.lihaoyi:fansi_2.13:0.2.9"),
            graphAssert.hasDependency("com.lihaoyi:sourcecode_2.13:0.2.3")
        );
    }

    @Test
    public void duplicatedNodesAndEdgesAreHandled() throws IOException {
        // Real dependencyDot files repeat node declarations, dependency edges and "Evicted By" edges.
        MavenGraphAssert graphAssert = transformSingleRoot(
            node(PROJECT),
            node("com.lihaoyi:fansi_2.13:0.2.9"),
            evictedNode("com.lihaoyi:sourcecode_2.13:0.2.1"),
            evictedNode("com.lihaoyi:sourcecode_2.13:0.2.1"),
            node("com.lihaoyi:sourcecode_2.13:0.2.3"),
            edge(PROJECT, "com.lihaoyi:fansi_2.13:0.2.9"),
            edge("com.lihaoyi:fansi_2.13:0.2.9", "com.lihaoyi:sourcecode_2.13:0.2.1"),
            edge("com.lihaoyi:fansi_2.13:0.2.9", "com.lihaoyi:sourcecode_2.13:0.2.1"),
            evictedByEdge("com.lihaoyi:sourcecode_2.13:0.2.1", "com.lihaoyi:sourcecode_2.13:0.2.3"),
            evictedByEdge("com.lihaoyi:sourcecode_2.13:0.2.1", "com.lihaoyi:sourcecode_2.13:0.2.3")
        );
        graphAssert.hasNoDependency("com.lihaoyi:sourcecode_2.13:0.2.1");
        graphAssert.hasRelationshipCount(graphAssert.hasDependency("com.lihaoyi:fansi_2.13:0.2.9"), 1);
        graphAssert.hasParentChildRelationship(
            graphAssert.hasDependency("com.lihaoyi:fansi_2.13:0.2.9"),
            graphAssert.hasDependency("com.lihaoyi:sourcecode_2.13:0.2.3")
        );
    }

    @Test
    public void dependencyWithEvictedInItsNameIsKept() throws IOException {
        // Eviction must be detected from the "Evicted By" edge, not from the substring "evicted" in node labels.
        MavenGraphAssert graphAssert = transformSingleRoot(
            node(PROJECT),
            node("com.lihaoyi:fansi_2.13:0.2.9"),
            node("com.foo:evicted-cache:1.0.0"),
            edge(PROJECT, "com.lihaoyi:fansi_2.13:0.2.9"),
            edge("com.lihaoyi:fansi_2.13:0.2.9", "com.foo:evicted-cache:1.0.0")
        );
        graphAssert.hasParentChildRelationship(
            graphAssert.hasDependency("com.lihaoyi:fansi_2.13:0.2.9"),
            graphAssert.hasDependency("com.foo:evicted-cache:1.0.0")
        );
    }

    @Test
    public void evictionParsedFromSbt191Format() throws IOException {
        // sbt 1.9.1+ node/edge attribute format: shape=box, penwidth, color, and the "dashed" eviction style.
        MavenGraphAssert graphAssert = transformSingleRoot(
            sbt191Node(PROJECT, false),
            sbt191Node("org.slf4j:slf4j-api:1.7.25", true),
            sbt191Node("org.slf4j:slf4j-api:1.7.32", false),
            edge(PROJECT, "org.slf4j:slf4j-api:1.7.25"),
            sbt191EvictedByEdge("org.slf4j:slf4j-api:1.7.25", "org.slf4j:slf4j-api:1.7.32")
        );
        graphAssert.hasNoDependency("org.slf4j:slf4j-api:1.7.25");
        graphAssert.hasRootDependency("org.slf4j:slf4j-api:1.7.32");
    }

    @Test
    public void coursierDirectEvictionKeepsDeclaredDependenciesDirect() throws IOException, DetectableException {
        // Mirrors the dot sbt 1.11 (Coursier) writes when a directly-declared guava 27.0-jre is evicted by guice's
        // guava 30.1-jre: the winner's own transitive dependencies are declared as stranded nodes without any edges,
        // the evicted node dangles, and the project must still be recognized as the single root so that guava and
        // guice stay direct dependencies, exactly as sbt dependencyTree reports them.
        String project = "default:sbt-eviction-demo:1.0";
        MutableGraph mutableGraph = createMutableGraph(String.join("",
            sbt191Node("aopalliance:aopalliance:1.0", false),
            sbt191Node("com.google.code.findbugs:jsr305:3.0.2", false),
            sbt191Node("com.google.errorprone:error_prone_annotations:2.3.4", false),
            sbt191Node("com.google.guava:failureaccess:1.0.1", false),
            sbt191Node("com.google.guava:guava:27.0-jre", true),
            sbt191Node("com.google.guava:guava:30.1-jre", false),
            sbt191Node("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava", false),
            sbt191Node("com.google.inject:guice:5.1.0", false),
            sbt191Node("com.google.j2objc:j2objc-annotations:1.3", false),
            sbt191Node(project, false),
            sbt191Node("javax.inject:javax.inject:1", false),
            sbt191Node("org.checkerframework:checker-qual:3.5.0", false),
            sbt191EvictedByEdge("com.google.guava:guava:27.0-jre", "com.google.guava:guava:30.1-jre"),
            edge("com.google.inject:guice:5.1.0", "aopalliance:aopalliance:1.0"),
            edge("com.google.inject:guice:5.1.0", "com.google.guava:guava:30.1-jre"),
            edge("com.google.inject:guice:5.1.0", "javax.inject:javax.inject:1"),
            edge(project, "com.google.guava:guava:30.1-jre"),
            edge(project, "com.google.inject:guice:5.1.0")
        ));
        SbtDotGraphNodeParser nodeParser = new SbtDotGraphNodeParser(new ExternalIdFactory());
        Set<String> rootIds = new SbtRootNodeFinder(nodeParser).determineRootIDs(mutableGraph);
        Assertions.assertEquals(Collections.singleton(project), rootIds);

        DependencyGraph graph = new SbtGraphParserTransformer(nodeParser).transformDotToGraph(rootIds, mutableGraph);
        MavenGraphAssert graphAssert = new MavenGraphAssert(graph);
        graphAssert.hasRootSize(2);
        graphAssert.hasRootDependency("com.google.guava:guava:30.1-jre");
        graphAssert.hasRootDependency("com.google.inject:guice:5.1.0");
        graphAssert.hasNoDependency("com.google.guava:guava:27.0-jre");
        graphAssert.hasNoDependency("com.google.code.findbugs:jsr305:3.0.2");
        graphAssert.hasParentChildRelationship(
            graphAssert.hasDependency("com.google.inject:guice:5.1.0"),
            graphAssert.hasDependency("com.google.guava:guava:30.1-jre")
        );
    }

    @Test
    public void multiRootGraphExcludesEvictedNodes() throws IOException {
        // When the project node cannot be determined, all roots become direct dependencies, but evicted nodes must
        // still be replaced by their winners.
        String rootOne = "com.example:api_2.13:1.0.0";
        String rootTwo = "com.example:util_2.13:1.0.0";
        Set<String> projectIds = new HashSet<>(Arrays.asList(rootOne, rootTwo));
        MavenGraphAssert graphAssert = transform(
            projectIds,
            node(rootOne),
            node(rootTwo),
            evictedNode("org.slf4j:slf4j-api:1.7.25"),
            node("org.slf4j:slf4j-api:1.7.32"),
            edge(rootOne, "org.slf4j:slf4j-api:1.7.25"),
            edge(rootTwo, "org.slf4j:slf4j-api:1.7.32"),
            evictedByEdge("org.slf4j:slf4j-api:1.7.25", "org.slf4j:slf4j-api:1.7.32")
        );
        graphAssert.hasNoDependency("org.slf4j:slf4j-api:1.7.25");
        graphAssert.hasParentChildRelationship(
            graphAssert.hasDependency(rootOne),
            graphAssert.hasDependency("org.slf4j:slf4j-api:1.7.32")
        );
        graphAssert.hasParentChildRelationship(
            graphAssert.hasDependency(rootTwo),
            graphAssert.hasDependency("org.slf4j:slf4j-api:1.7.32")
        );
    }
}
