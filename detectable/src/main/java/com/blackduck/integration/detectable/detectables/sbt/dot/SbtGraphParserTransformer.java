package com.blackduck.integration.detectable.detectables.sbt.dot;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;

public class SbtGraphParserTransformer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SbtDotGraphNodeParser sbtDotGraphNodeParser;

    public SbtGraphParserTransformer(SbtDotGraphNodeParser sbtDotGraphNodeParser) {
        this.sbtDotGraphNodeParser = sbtDotGraphNodeParser;
    }

    public DependencyGraph transformDotToGraph(String projectNodeId, MutableGraph mutableGraph) {
        DependencyGraph graph = new BasicDependencyGraph();

        Set<String> evictedIds = new HashSet<>();
        mutableGraph.nodes().forEach(node -> {
            node.attrs().forEach(attr -> {
                if(attr.getKey().equals("label")) {
                    if(attr.getValue().toString().toLowerCase().contains("evicted")) {
                        evictedIds.add(node.name().toString());
                    }
                }
            });
            node.links().forEach(link -> {
                link.attrs().forEach(attr -> {
                    if(attr.getKey().equals("label")) {
                        if(attr.getValue().toString().toLowerCase().contains("evicted")) {
                            evictedIds.add(node.name().toString());
                        }
                    }
                });
            });
        });

        List<Link> links = mutableGraph.nodes().stream().map(MutableNode::links).flatMap(List::stream).collect(Collectors.toList());
        for (Link link : links) {
            String parentNode = normalizeDependency(link.asLinkTarget().name().toString());
            String childNode = normalizeDependency(link.asLinkSource().name().toString());

            Dependency parent = sbtDotGraphNodeParser.nodeToDependency(parentNode);
            Dependency child = sbtDotGraphNodeParser.nodeToDependency(childNode);

            if (projectNodeId.equals(parentNode)) {
                  graph.addDirectDependency(child);
            } else {
                if (!evictedIds.contains(childNode)) {
                    graph.addChildWithParent(child, parent);
                }
            }
        }

        return graph;
    }

    public DependencyGraph transformDotToGraph(Set<String> projectNodeIds, MutableGraph mutableGraph) {
        DependencyGraph graph = new BasicDependencyGraph();

        List<Link> links = mutableGraph.nodes().stream().map(MutableNode::links).flatMap(List::stream).collect(Collectors.toList());
        for (Link link : links) {
            String parentNode = normalizeDependency(link.asLinkTarget().name().toString());
            String childNode = normalizeDependency(link.asLinkSource().name().toString());

            Dependency parent = sbtDotGraphNodeParser.nodeToDependency(parentNode);
            Dependency child = sbtDotGraphNodeParser.nodeToDependency(childNode);

            if (projectNodeIds.contains(parentNode)) {
                graph.addDirectDependency(parent);
            }

            graph.addChildWithParent(child, parent);

        }

        return graph;
    }

    private String normalizeDependency(String dependency) {
        if(dependency.startsWith("--")) {
            return dependency.substring(2);
        }
        return dependency;
    }
}
