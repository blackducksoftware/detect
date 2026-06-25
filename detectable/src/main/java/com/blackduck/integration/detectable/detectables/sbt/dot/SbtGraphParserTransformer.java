package com.blackduck.integration.detectable.detectables.sbt.dot;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SbtGraphParserTransformer {
    private final SbtDotGraphNodeParser sbtDotGraphNodeParser;

    public SbtGraphParserTransformer(SbtDotGraphNodeParser sbtDotGraphNodeParser) {
        this.sbtDotGraphNodeParser = sbtDotGraphNodeParser;
    }

    public DependencyGraph transformDotToGraph(Set<String> projectIds, MutableGraph mutableGraph) {
        DependencyGraph graph = new BasicDependencyGraph();
        String projectNodeId = projectIds.stream().findFirst().orElse(null);
        boolean isOneRoot = projectIds.size() == 1;
        Map<String, String> evictedToWinner = SbtDotEvictionParser.parseEvictedToWinner(mutableGraph);

        List<Link> links = mutableGraph.nodes().stream().map(MutableNode::links).flatMap(List::stream).collect(Collectors.toList());
        for (Link link : links) {
            if (SbtDotEvictionParser.isEvictedByLink(link)) {
                continue; // links an evicted version to its replacement, not a parent to a child
            }
            String parentNode = SbtDotEvictionParser.normalizeNodeId(link.asLinkTarget().name().toString());
            if (evictedToWinner.containsKey(parentNode)) {
                continue; // resolution discarded the evicted version along with its subtree
            }
            String childNode = resolveEviction(SbtDotEvictionParser.normalizeNodeId(link.asLinkSource().name().toString()), evictedToWinner);

            Dependency parent = sbtDotGraphNodeParser.nodeToDependency(parentNode);
            Dependency child = sbtDotGraphNodeParser.nodeToDependency(childNode);

            if (isOneRoot) {
                if (parentNode.equals(projectNodeId)) {
                    graph.addDirectDependency(child);
                } else {
                    graph.addChildWithParent(child, parent);
                }
            } else {
                if (projectIds.contains(parentNode)) {
                    graph.addDirectDependency(parent);
                }
                graph.addChildWithParent(child, parent);
            }
        }

        return graph;
    }

    private String resolveEviction(String nodeId, Map<String, String> evictedToWinner) {
        Set<String> seen = new HashSet<>();
        while (evictedToWinner.containsKey(nodeId) && seen.add(nodeId)) {
            nodeId = evictedToWinner.get(nodeId);
        }
        return nodeId;
    }
}
