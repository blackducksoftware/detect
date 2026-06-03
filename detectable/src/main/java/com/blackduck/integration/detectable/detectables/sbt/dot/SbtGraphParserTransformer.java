package com.blackduck.integration.detectable.detectables.sbt.dot;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;

public class SbtGraphParserTransformer {
    private final SbtDotGraphNodeParser sbtDotGraphNodeParser;

    public SbtGraphParserTransformer(SbtDotGraphNodeParser sbtDotGraphNodeParser) {
        this.sbtDotGraphNodeParser = sbtDotGraphNodeParser;
    }

    public DependencyGraph transformDotToGraph(Set<String> projectIds, MutableGraph mutableGraph) {
        DependencyGraph graph = new BasicDependencyGraph();
        String projectNodeId = projectIds.stream().findFirst().get();
        boolean isOneRoot = projectIds.size() == 1;
        Set<String> evictedIds = getEvictedIds(mutableGraph);

        List<Link> links = mutableGraph.nodes().stream().map(MutableNode::links).flatMap(List::stream).collect(Collectors.toList());
        for (Link link : links) {
            String parentNode = normalizeDependency(link.asLinkTarget().name().toString());
            String childNode = normalizeDependency(link.asLinkSource().name().toString());

            Dependency parent = sbtDotGraphNodeParser.nodeToDependency(parentNode);
            Dependency child = sbtDotGraphNodeParser.nodeToDependency(childNode);

            if(isOneRoot) {
                if (projectNodeId.equals(parentNode)) {
                    graph.addDirectDependency(child);
                } else {
                    // Skip edges where either side is an evicted dependency.
                    // For eviction edges like "guava:27.0" -> "guava:30.1" [label="Evicted By"],
                    // parentNode is the evicted node (guava:27.0) which is in evictedIds.
                    // The replacement (guava:30.1) is NOT in evictedIds, so legitimate edges
                    // like "guice" -> "guava:30.1" are preserved.
                    if (!evictedIds.contains(childNode) && !evictedIds.contains(parentNode)) {
                        graph.addChildWithParent(child, parent);
                    }
                }
            } else {
                if (projectIds.contains(parentNode)) {
                    graph.addDirectDependency(parent);
                }

                // Same eviction check for the multi-root case.
                if (!evictedIds.contains(childNode) && !evictedIds.contains(parentNode)) {
                    graph.addChildWithParent(child, parent);
                }
            }
        }

        return graph;
    }

    private Set<String> getEvictedIds(MutableGraph mutableGraph) {
        Set<String> evictedIds = new HashSet<>();
        mutableGraph.nodes().forEach(node -> {
            node.attrs().forEach(attr -> addEvictedEntry(attr, node, evictedIds));
            node.links().forEach(link -> link.attrs().forEach(attr -> addEvictedEntry(attr, node, evictedIds)));
        });

        return evictedIds;
    }

    private void addEvictedEntry(Map.Entry<String, Object> attr, MutableNode node, Set<String> evictedIds) {
        if(attr.getKey().equals("label") && attr.getValue().toString().toLowerCase().contains("evicted")) {
            evictedIds.add(node.name().toString());
        }
    }

    private String normalizeDependency(String dependency) {
        if(dependency.startsWith("--")) {
            return dependency.substring(2);
        }
        return dependency;
    }
}
