package com.blackduck.integration.detectable.detectables.sbt.dot;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.model.MutableNode;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.model.Link;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectable.exception.DetectableException;

public class SbtRootNodeFinder {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
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
        return SetUtils.difference(allNodeIds, nodeIdsUsedInDestination);
    }
}
