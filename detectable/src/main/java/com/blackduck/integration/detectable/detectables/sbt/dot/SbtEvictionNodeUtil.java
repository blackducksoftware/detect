package com.blackduck.integration.detectable.detectables.sbt.dot;

import guru.nidi.graphviz.model.MutableGraph;

import java.util.HashMap;
import java.util.Map;

public class SbtEvictionNodeUtil {

    private SbtEvictionNodeUtil() {}

    /**
     * Returns a map from evicted node ID to its winning replacement node ID.
     * Keys are evicted nodes; values are the winning (replacement) versions.
     * e.g. "kotlin-stdlib-jdk8:1.8.21" -> "kotlin-stdlib-jdk8:1.9.10"
     */
    public static Map<String, String> findEvictions(MutableGraph mutableGraph) {
        Map<String, String> evictions = new HashMap<>();
        mutableGraph.nodes().forEach(node ->
            node.links().forEach(link ->
                link.attrs().forEach(attr -> {
                    if (attr.getKey().equals("label") && attr.getValue().toString().toLowerCase().contains("evicted")) {
                        evictions.put(node.name().toString(), link.asLinkSource().name().toString());
                    }
                })));
        return evictions;
    }
}
