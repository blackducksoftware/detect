package com.blackduck.integration.detectable.detectables.sbt.dot;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SbtGraphParserTransformer {
    private final SbtDotGraphNodeParser sbtDotGraphNodeParser;

    public SbtGraphParserTransformer(SbtDotGraphNodeParser sbtDotGraphNodeParser) {
        this.sbtDotGraphNodeParser = sbtDotGraphNodeParser;
    }

    public DependencyGraph transformDotToGraph(Set<String> projectIds, MutableGraph mutableGraph) {
        DependencyGraph graph = new BasicDependencyGraph();
        String projectNodeId = projectIds.stream().findFirst().orElseThrow(() -> new IllegalArgumentException("projectIds must not be empty"));
        boolean isOneRoot = projectIds.size() == 1;
        Set<String> evictedIds = SbtEvictionNodeUtil.findEvictedNodeIds(mutableGraph);

        List<Link> links = mutableGraph.nodes().stream().map(MutableNode::links).flatMap(List::stream).collect(Collectors.toList());
        for (Link link : links) {
            String parentNode = normalizeDependency(link.asLinkTarget().name().toString());
            String childNode = normalizeDependency(link.asLinkSource().name().toString());

            Dependency parent = sbtDotGraphNodeParser.nodeToDependency(parentNode);
            Dependency child = sbtDotGraphNodeParser.nodeToDependency(childNode);

            if(isOneRoot) {
                processSingleRootLink(graph, projectNodeId, parentNode, childNode, parent, child, evictedIds);
            } else {
                processMultiRootLink(graph, projectIds, parentNode, childNode, parent, child, evictedIds);
            }
        }

        return graph;
    }

    // Used when the graph has exactly one project root.
    // Edges from the project node become direct dependencies; all other non-evicted edges become transitive.
    private void processSingleRootLink(DependencyGraph graph, String projectNodeId, String parentNode, String childNode,
                                       Dependency parent, Dependency child, Set<String> evictedIds) {
        processMultiRootLink(graph, Collections.singleton(projectNodeId), parentNode, childNode, parent, child, evictedIds);
    }

    // Used when the graph has multiple root candidates.
    // Edges from any project root node become direct dependencies; all other non-evicted edges become transitive.
    private void processMultiRootLink(DependencyGraph graph, Set<String> projectIds, String parentNode, String childNode,
                                      Dependency parent, Dependency child, Set<String> evictedIds) {
        if (!evictedIds.contains(childNode)) {
            if (projectIds.contains(parentNode)) {
                // e.g. "default:myproject:1.0" -> "com.google.inject:guice:5.1.0"
                // parentNode is a project root, so child (guice) is a direct dependency.
                // We add child — not parent — because parentNode is the project itself, not a real dependency.
                graph.addDirectDependency(child);
            } else if (!evictedIds.contains(parentNode)) {
                // e.g. "com.google.inject:guice:5.1.0" -> "com.google.guava:guava:30.1-jre"
                // Skip if either side is evicted:
                //   parentNode evicted: "com.google.guava:guava:27.0-jre" -> "com.google.guava:guava:30.1-jre" [label="Evicted By"]
                //   childNode evicted:  "com.google.inject:guice:5.1.0"   -> "com.google.guava:guava:27.0-jre"
                graph.addChildWithParent(child, parent);
            }
        }
    }

    private String normalizeDependency(String dependency) {
        if(dependency.startsWith("--")) {
            return dependency.substring(2);
        }
        return dependency;
    }
}
