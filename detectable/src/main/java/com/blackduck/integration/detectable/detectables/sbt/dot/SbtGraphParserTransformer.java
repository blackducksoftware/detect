package com.blackduck.integration.detectable.detectables.sbt.dot;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SbtGraphParserTransformer {
    private final SbtDotGraphNodeParser sbtDotGraphNodeParser;

    public SbtGraphParserTransformer(SbtDotGraphNodeParser sbtDotGraphNodeParser) {
        this.sbtDotGraphNodeParser = sbtDotGraphNodeParser;
    }

    public DependencyGraph transformDotToGraph(@NotNull Set<String> projectIds, @NotNull Map<String, String> evictions, @NotNull MutableGraph mutableGraph) {
        DependencyGraph graph = new BasicDependencyGraph();
        String projectNodeId = projectIds.stream().findFirst().orElseThrow(() -> new IllegalArgumentException("projectIds must not be empty"));
        boolean isOneRoot = projectIds.size() == 1;

        List<Link> links = mutableGraph.nodes().stream().map(MutableNode::links).flatMap(List::stream).collect(Collectors.toList());
        for (Link link : links) {
            String parentNode = normalizeDependency(link.asLinkTarget().name().toString());
            String childNode = normalizeDependency(link.asLinkSource().name().toString());

            // Skip the edge if the evicted node is the source/parent of the edge.
            if (evictions.containsKey(parentNode)) continue;

            // If child is evicted, substitute with the replacement.
            if (evictions.containsKey(childNode)) {
                childNode = evictions.get(childNode);
                if (childNode == null) continue;
            }

            if (isOneRoot) {
                processSingleRootLink(graph, projectNodeId, parentNode, childNode);
            } else {
                processMultiRootLink(graph, projectIds, parentNode, childNode);
            }
        }

        return graph;
    }

    public DependencyGraph transformDotToGraph(@NotNull Set<String> projectIds, @NotNull MutableGraph mutableGraph) {
        return transformDotToGraph(projectIds, SbtEvictionNodeUtil.findEvictions(mutableGraph), mutableGraph);
    }

    // Used when the graph has multiple root candidates.
    // Each root candidate is itself a sub-project component, so when a root node has outgoing
    // edges it is registered as a direct dependency and its children become transitive under it.
    private void processMultiRootLink(DependencyGraph graph, Set<String> projectIds, String parentNode, String childNode) {
        Dependency parent = sbtDotGraphNodeParser.nodeToDependency(parentNode);
        Dependency child = sbtDotGraphNodeParser.nodeToDependency(childNode);

        // If parent is a root candidate (sub-project module), register it as a direct dependency.
        if (projectIds.contains(parentNode)) {
            graph.addDirectDependency(parent);
        }

        // Always add the parent→child relationship so transitive deps are preserved.
        graph.addChildWithParent(child, parent);
    }

    // Used when the graph has exactly one project root.
    // Edges from the project node become direct dependencies; all other non-evicted edges become transitive.
    private void processSingleRootLink(DependencyGraph graph, String projectNodeId, String parentNode, String childNode) {
        Dependency parent = sbtDotGraphNodeParser.nodeToDependency(parentNode);
        Dependency child = sbtDotGraphNodeParser.nodeToDependency(childNode);

        if (projectNodeId.contains(parentNode)) {
            // e.g. "default:myproject:1.0" -> "com.google.inject:guice:5.1.0"
            // parentNode is the project root, so child is a direct dependency.
            graph.addDirectDependency(child);
        } else {
            graph.addChildWithParent(child, parent);
        }
    }

    private String normalizeDependency(String dependency) {
        if (dependency.startsWith("--")) {
            return dependency.substring(2);
        }
        return dependency;
    }
}
