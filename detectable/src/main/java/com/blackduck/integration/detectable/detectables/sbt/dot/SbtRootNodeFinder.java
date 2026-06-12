package com.blackduck.integration.detectable.detectables.sbt.dot;

import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import org.apache.commons.collections4.SetUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SbtRootNodeFinder {
    private final SbtDotGraphNodeParser sbtDotGraphNodeParser;

    public SbtRootNodeFinder(SbtDotGraphNodeParser sbtDotGraphNodeParser) {
        this.sbtDotGraphNodeParser = sbtDotGraphNodeParser;
    }

    public Set<String> determineRootIDs(MutableGraph mutableGraph) throws DetectableException {
        Set<String> nodeIdsUsedInDestination = mutableGraph.nodes().stream()
                .map(MutableNode::links)
                .flatMap(List::stream)
                .map(Link::asLinkSource)
                .map(LinkSource::name)
                .map(Label::value)
                .collect(Collectors.toSet());

        Set<String> allNodeIds = mutableGraph.nodes().stream().map(MutableNode::name).map(Label::value).collect(Collectors.toSet());
        Set<String> rootIds = new HashSet<>(SetUtils.difference(allNodeIds, nodeIdsUsedInDestination));
        // An evicted version of a directly-declared dependency has no incoming edges (sbt with Coursier), so it would
        // otherwise be mistaken for a root.
        Set<String> evictedIds = SbtDotEvictionParser.parseEvictedToWinner(mutableGraph).keySet();
        rootIds.removeAll(evictedIds);

        // Coursier, the default sbt resolver since sbt 1.3, can leave stranded nodes and phantom callers
        // that look like roots but are not. Stranded nodes are the winner's transitive deps whose incoming
        // edges were dropped; phantom callers are nodes whose only outgoing edge targets an evicted version.
        // Prefer candidates with at least one real dependency edge; fall back to all candidates for
        // dependency-free projects.
        Set<String> nodesWithDependencyEdges = nodesWithDependencyEdges(mutableGraph, evictedIds);
        Set<String> rootIdsWithDependencies = rootIds.stream()
            .filter(nodesWithDependencyEdges::contains)
            .collect(Collectors.toSet());
        return rootIdsWithDependencies.isEmpty() ? rootIds : rootIdsWithDependencies;
    }

    private Set<String> nodesWithDependencyEdges(MutableGraph mutableGraph, Set<String> evictedIds) {
        return mutableGraph.nodes().stream()
            .filter(node -> node.links().stream().anyMatch(link -> isDependencyEdge(link, evictedIds)))
            .map(MutableNode::name)
            .map(Label::value)
            .collect(Collectors.toSet());
    }

    private boolean isDependencyEdge(Link link, Set<String> evictedIds) {
        return !SbtDotEvictionParser.isEvictedByLink(link)
            && !evictedIds.contains(SbtDotEvictionParser.normalizeNodeId(link.asLinkSource().name().toString()));
    }
}
