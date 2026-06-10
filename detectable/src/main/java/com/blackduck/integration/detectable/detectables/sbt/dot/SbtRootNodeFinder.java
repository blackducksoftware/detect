package com.blackduck.integration.detectable.detectables.sbt.dot;

import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import org.apache.commons.collections4.SetUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SbtRootNodeFinder {
    private final SbtDotGraphNodeParser sbtDotGraphNodeParser;

    public SbtRootNodeFinder(SbtDotGraphNodeParser sbtDotGraphNodeParser) {
        this.sbtDotGraphNodeParser = sbtDotGraphNodeParser;
    }

    public Set<String> determineRootIDs(@NotNull Map<String, String> evictions, @NotNull MutableGraph mutableGraph) throws DetectableException {
        Set<String> nodeIdsUsedInDestination = mutableGraph.nodes().stream()
                .map(MutableNode::links)
                .flatMap(List::stream)
                .map(Link::asLinkSource)
                .map(LinkSource::name)
                .map(Label::value)
                .collect(Collectors.toSet());

        Set<String> allNodeIds = mutableGraph.nodes().stream().map(MutableNode::name).map(Label::value).collect(Collectors.toSet());

        Set<String> candidates = SetUtils.difference(allNodeIds, nodeIdsUsedInDestination);
        // Evicted nodes have an outgoing edge but no incoming edges, so they appear as root candidates.
        // Remove them — they are not real project roots but evicted nodes.
        // e.g. guava:27.0 -> guava:30.1 [label="Evicted By"]
        Set<String> nonEvictedCandidates = SetUtils.difference(candidates, evictions.keySet());

        // Orphan nodes (no outgoing edges) appear as spurious root candidates when SBT's dependencyDot
        // omits edges from the eviction winner to its transitive deps. This only happens in graphs that
        // have eviction edges — in multi-project builds, sibling modules appear as orphan nodes too,
        // but those ARE legitimate root candidates. Applying the orphan filter to graphs without
        // eviction edges would incorrectly collapse multi-project DOT files to single-root mode.
        if (!evictions.isEmpty()) {
            Set<String> nodesWithOutgoingEdges = mutableGraph.nodes().stream()
                    .filter(node -> !node.links().isEmpty())
                    .map(MutableNode::name)
                    .map(Label::value)
                    .collect(Collectors.toSet());
            Set<String> nonOrphanCandidates = SetUtils.intersection(nonEvictedCandidates, nodesWithOutgoingEdges);
            if (!nonOrphanCandidates.isEmpty()) {
                return nonOrphanCandidates;
            }
        }
        return nonEvictedCandidates;
    }

    public Set<String> determineRootIDs(@NotNull MutableGraph mutableGraph) throws DetectableException {
        return determineRootIDs(SbtEvictionNodeUtil.findEvictions(mutableGraph), mutableGraph);
    }
}
