package com.blackduck.integration.detectable.detectables.sbt.unit;

import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtDotGraphNodeParser;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtRootNodeFinder;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class SbtRootNodeFinderTest {

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

    private String node(String org, String name, String version) {
        return "    \"" + org + ":" + name + ":" + version + "\"[label=<" + org + "<BR/><B>" + name + "</B><BR/>" + version + "> style=\"\"]\n";
    }

    private String evictedNode(String org, String name, String version) {
        return "    \"" + org + ":" + name + ":" + version + "\"[label=<" + org + "<BR/><B>" + name + "</B><BR/>" + version + "> style=\"stroke-dasharray: 5,5\"]\n";
    }

    private String edge(String fromOrg, String fromName, String fromVersion, String toOrg, String toName, String toVersion) {
        return "    \"" + fromOrg + ":" + fromName + ":" + fromVersion + "\" -> \"" + toOrg + ":" + toName + ":" + toVersion + "\"\n";
    }

    private String evictedByEdge(String org, String name, String fromVersion, String toVersion) {
        return "    \"" + org + ":" + name + ":" + fromVersion + "\" -> \"" + org + ":" + name + ":" + toVersion + "\" [label=\"Evicted By\" style=\"stroke-dasharray: 5,5\"]\n";
    }

    @Test
    public void projectFoundFromSingleNode() throws DetectableException, IOException {
        MutableGraph mutableGraph = createMutableGraph(node("one-org", "one-name", "one-version"));
        SbtRootNodeFinder projectMatcher = new SbtRootNodeFinder(new SbtDotGraphNodeParser(new ExternalIdFactory()));
        Set<String> projectId = projectMatcher.determineRootIDs(mutableGraph);
        Assertions.assertEquals(1, projectId.size());
        Assertions.assertEquals("one-org:one-name:one-version", projectId.stream().findFirst().get());
    }

    @Test
    public void projectFoundFromTwoNodesWhereProjectIsSecond() throws DetectableException, IOException {
        MutableGraph mutableGraph = createMutableGraph(node("two-org", "two-name", "two-version") +
            node("one-org", "one-name", "one-version") +
            edge("one-org", "one-name", "one-version", "two-org", "two-name", "two-version"));
        SbtRootNodeFinder projectMatcher = new SbtRootNodeFinder(new SbtDotGraphNodeParser(new ExternalIdFactory()));
        Set<String> projectId = projectMatcher.determineRootIDs(mutableGraph);
        Assertions.assertEquals(1, projectId.size());
        Assertions.assertEquals("one-org:one-name:one-version", projectId.stream().findFirst().get());
    }

    @Test
    public void multipleFoundWithNoEdges() throws DetectableException, IOException {
        MutableGraph mutableGraph = createMutableGraph(node("one-org", "one-name", "one-version") +
            node("one-org", "one-name", "two-version"));
        SbtRootNodeFinder projectMatcher = new SbtRootNodeFinder(new SbtDotGraphNodeParser(new ExternalIdFactory()));
        Set<String> projectId = projectMatcher.determineRootIDs(mutableGraph);
        Assertions.assertEquals(2, projectId.size());
        Assertions.assertTrue(projectId.contains("one-org:one-name:one-version"));
        Assertions.assertTrue(projectId.contains("one-org:one-name:two-version"));
    }

    @Test
    public void danglingEvictedNodeIsNotReportedAsRoot() throws DetectableException, IOException {
        // Under Coursier (sbt 1.3+ default), a directly-declared dependency that loses conflict resolution keeps its
        // node but has no incoming edges; only the synthetic "Evicted By" edge leaves it. It must not look like a root.
        MutableGraph mutableGraph = createMutableGraph(node("project-org", "project-name", "project-version") +
            node("dep-org", "dep-name", "two-version") +
            evictedNode("dep-org", "dep-name", "one-version") +
            edge("project-org", "project-name", "project-version", "dep-org", "dep-name", "two-version") +
            evictedByEdge("dep-org", "dep-name", "one-version", "two-version"));
        SbtRootNodeFinder projectMatcher = new SbtRootNodeFinder(new SbtDotGraphNodeParser(new ExternalIdFactory()));
        Set<String> projectId = projectMatcher.determineRootIDs(mutableGraph);
        Assertions.assertEquals(1, projectId.size());
        Assertions.assertEquals("project-org:project-name:project-version", projectId.stream().findFirst().get());
    }

    @Test
    public void multipleFoundWithEdge() throws DetectableException, IOException {
        MutableGraph mutableGraph = createMutableGraph(node("one-org", "one-name", "one-version") +
            node("one-org", "one-name", "two-version") +
            node("one-org", "one-name", "three-version") + //should not be reported
            edge("one-org", "one-name", "one-version", "one-org", "one-name", "three-version") +
            edge("one-org", "one-name", "two-version", "one-org", "one-name", "three-version"));
        SbtRootNodeFinder projectMatcher = new SbtRootNodeFinder(new SbtDotGraphNodeParser(new ExternalIdFactory()));
        Set<String> projectId = projectMatcher.determineRootIDs(mutableGraph);
        Assertions.assertEquals(2, projectId.size());
        Assertions.assertTrue(projectId.contains("one-org:one-name:one-version"));
        Assertions.assertTrue(projectId.contains("one-org:one-name:two-version"));
    }

    @Test
    public void strandedNodesWithoutAnyEdgesAreNotRoots() throws DetectableException, IOException {
        // sbt with Coursier declares the winning version's transitive dependencies as nodes but can attribute their
        // incoming edges to the evicted version's id, which the renderer then drops. Such stranded, edge-less nodes
        // must not be mistaken for project roots (seen with guava/guice on sbt 1.11: jsr305, checker-qual, etc.).
        MutableGraph mutableGraph = createMutableGraph(node("project-org", "project-name", "project-version") +
            node("dep-org", "dep-name", "dep-version") +
            node("stranded-org", "stranded-one", "one-version") +
            node("stranded-org", "stranded-two", "two-version") +
            evictedNode("dep-org", "old-dep", "old-version") +
            edge("project-org", "project-name", "project-version", "dep-org", "dep-name", "dep-version") +
            evictedByEdge("dep-org", "old-dep", "old-version", "new-version"));
        SbtRootNodeFinder projectMatcher = new SbtRootNodeFinder(new SbtDotGraphNodeParser(new ExternalIdFactory()));
        Set<String> projectId = projectMatcher.determineRootIDs(mutableGraph);
        Assertions.assertEquals(1, projectId.size());
        Assertions.assertEquals("project-org:project-name:project-version", projectId.stream().findFirst().get());
    }

    @Test
    public void nodeWhoseOnlyEdgesPointAtEvictedNodesIsNotARoot() throws DetectableException, IOException {
        // Coursier range conflicts can produce a caller node whose only outgoing edge points at the evicted node,
        // while the project's own edge goes straight to the winner.
        MutableGraph mutableGraph = createMutableGraph(node("project-org", "project-name", "project-version") +
            node("dep-org", "dep-name", "winning-version") +
            evictedNode("dep-org", "dep-name", "requested-version") +
            edge("project-org", "project-name", "project-version", "dep-org", "dep-name", "winning-version") +
            edge("dep-org", "dep-name", "phantom-version", "dep-org", "dep-name", "requested-version") +
            evictedByEdge("dep-org", "dep-name", "requested-version", "winning-version"));
        SbtRootNodeFinder projectMatcher = new SbtRootNodeFinder(new SbtDotGraphNodeParser(new ExternalIdFactory()));
        Set<String> projectId = projectMatcher.determineRootIDs(mutableGraph);
        Assertions.assertEquals(1, projectId.size());
        Assertions.assertEquals("project-org:project-name:project-version", projectId.stream().findFirst().get());
    }

    @Test
    public void disconnectedWorkspaceSubProjectsReturnedAsRootsWhenNoEvictions() throws DetectableException, IOException {
        // sbt includes all workspace sub-projects in every dot file. Sub-projects with no compile
        // dependencies in the current scope appear as isolated nodes (no outgoing edges). Without evictions
        // these isolated nodes are legitimate projects, not Coursier stranded nodes — all root candidates
        // must be returned so the caller can emit the appropriate multi-root warning.
        // Regression: the nodesWithDependencyEdges filter must NOT be applied when evictedIds is empty.
        MutableGraph mutableGraph = createMutableGraph(
            node("services", "service1_2.12", "0.1.0-SNAPSHOT") +
            node("api", "api1_2.12", "0.1.0-SNAPSHOT") +
            node("api", "api2_2.12", "0.1.0-SNAPSHOT") +   // isolated — no edges
            node("services", "service2_2.12", "0.1.0-SNAPSHOT") + // isolated — no edges
            edge("services", "service1_2.12", "0.1.0-SNAPSHOT", "api", "api1_2.12", "0.1.0-SNAPSHOT")
        );
        SbtRootNodeFinder projectMatcher = new SbtRootNodeFinder(new SbtDotGraphNodeParser(new ExternalIdFactory()));
        Set<String> rootIds = projectMatcher.determineRootIDs(mutableGraph);
        // service1, api2, service2 are all root candidates (api1 is a dependency of service1)
        Assertions.assertEquals(3, rootIds.size());
        Assertions.assertTrue(rootIds.contains("services:service1_2.12:0.1.0-SNAPSHOT"));
        Assertions.assertTrue(rootIds.contains("api:api2_2.12:0.1.0-SNAPSHOT"));
        Assertions.assertTrue(rootIds.contains("services:service2_2.12:0.1.0-SNAPSHOT"));
    }

}
