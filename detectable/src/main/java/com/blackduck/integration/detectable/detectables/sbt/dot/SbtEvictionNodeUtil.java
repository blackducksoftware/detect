package com.blackduck.integration.detectable.detectables.sbt.dot;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

public class SbtEvictionNodeUtil {

    private SbtEvictionNodeUtil() {}

    public static Set<String> findEvictedNodeIds(MutableGraph mutableGraph) {
        Set<String> evictedIds = new HashSet<>();
        mutableGraph.nodes().forEach(node -> {
            node.attrs().forEach(attr -> addEvictedEntry(attr, node, evictedIds));
            node.links().forEach(link -> link.attrs().forEach(attr -> addEvictedEntry(attr, node, evictedIds)));
        });
        return evictedIds;
    }

    private static void addEvictedEntry(Map.Entry<String, Object> attr, MutableNode node, Set<String> evictedIds) {
        if (attr.getKey().equals("label") && attr.getValue().toString().toLowerCase().contains("evicted")) {
            evictedIds.add(node.name().toString());
        }
    }
}
