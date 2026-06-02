package com.blackduck.integration.detectable.detectables.sbt.dot;

import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import org.apache.commons.collections4.SetUtils;

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

        Set<String> candidates = SetUtils.difference(allNodeIds, nodeIdsUsedInDestination);
        // Evicted nodes have an outgoing edge but no incoming edges, so they appear as root candidates.
        // Remove them — they are not real project roots but evicted nodes.
        // e.g. guava:27.0 -> guava:30.1 [label="Evicted By"]
        Set<String> evictedIds = SbtEvictionNodeUtil.findEvictedNodeIds(mutableGraph);
        return SetUtils.difference(candidates, evictedIds);
    }
}
