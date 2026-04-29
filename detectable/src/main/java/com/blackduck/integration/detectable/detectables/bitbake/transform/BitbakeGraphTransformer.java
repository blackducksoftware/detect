package com.blackduck.integration.detectable.detectables.bitbake.transform;

import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import com.blackduck.integration.detectable.detectables.bitbake.model.BitbakeGraph;
import com.blackduck.integration.detectable.detectables.bitbake.parse.GraphNodeLabelParser;

public class BitbakeGraphTransformer {
    private final GraphNodeLabelParser graphNodeLabelParser;

    public BitbakeGraphTransformer(GraphNodeLabelParser graphNodeLabelParser) {
        this.graphNodeLabelParser = graphNodeLabelParser;
    }

    public BitbakeGraph transform(MutableGraph mutableGraph, Set<String> layerNames) {
        BitbakeGraph bitbakeGraph = new BitbakeGraph();

        for (MutableNode node : mutableGraph.nodes()) {
            String name = parseNameFromId(node.name().value());
            Optional<String> layer = parseLayerFromNode(node, layerNames);
            parseVersionFromNode(node).ifPresent(ver -> bitbakeGraph.addNode(name, ver, layer.orElse(null)));
        }

        for (MutableNode node : mutableGraph.nodes()) {
            for (Link link : node.links()) {
                String parent = parseNameFromId(link.asLinkTarget().name().value());
                String child = parseNameFromId(link.asLinkSource().name().value());
                if (!parent.equals(child)) {
                    bitbakeGraph.addChild(parent, child);
                }
            }
        }

        return bitbakeGraph;
    }

    private String parseNameFromId(String id) {
        String[] nodeIdPieces = id.split(".do_");
        return normalizeDependency(nodeIdPieces[0].replace("\"", ""));
    }

    private Optional<String> parseVersionFromNode(MutableNode node) {
        Optional<String> labelValue = getLabelAttribute(node);
        if (labelValue.isPresent()) {
            return graphNodeLabelParser.parseVersionFromLabel(labelValue.get());
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> parseLayerFromNode(MutableNode node, Set<String> knownLayerNames) {
        Optional<String> labelAttribute = getLabelAttribute(node);
        if (labelAttribute.isPresent()) {
            return graphNodeLabelParser.parseLayerFromLabel(labelAttribute.get(), knownLayerNames);
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> getLabelAttribute(MutableNode node) {
        for (Map.Entry<String, Object> attr : node.attrs()) {
            if ("label".equals(attr.getKey()) && attr.getValue() != null) {
                String value = attr.getValue().toString();
                if (StringUtils.isNotBlank(value)) {
                    return Optional.of(value);
                }
            }
        }
        return Optional.empty();
    }

    private String normalizeDependency(String dependency) {
        if (dependency.startsWith("--")) {
            return dependency.substring(2);
        }
        return dependency;
    }

}
