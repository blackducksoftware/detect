package com.blackduck.integration.detectable.detectables.sbt.dot;

import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;

import java.util.HashMap;
import java.util.Map;

/**
 * sbt's dependencyDot output (sbt-dependency-graph plugin, built into sbt 1.4+) marks an evicted module with a
 * synthetic edge from the evicted version to the version that replaced it:
 * "org:name:oldVersion" -> "org:name:winningVersion" [label="Evicted By"].
 * That edge is the only reliable eviction marker in the dot output; node labels never describe evictions, and
 * ordinary dependency edges point at the version a parent requested, which may be the evicted one.
 */
public final class SbtDotEvictionParser {
    private static final String EVICTED_BY_EDGE_LABEL = "evicted by";

    private SbtDotEvictionParser() {
    }

    public static boolean isEvictedByLink(Link link) {
        for (Map.Entry<String, Object> attr : link.attrs()) {
            if ("label".equals(attr.getKey()) && EVICTED_BY_EDGE_LABEL.equalsIgnoreCase(String.valueOf(attr.getValue()).trim())) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, String> parseEvictedToWinner(MutableGraph mutableGraph) {
        Map<String, String> evictedToWinner = new HashMap<>();
        mutableGraph.nodes().forEach(node -> node.links().forEach(link -> {
            if (isEvictedByLink(link)) {
                // graphviz-java exposes an edge "from" -> "to" with asLinkTarget() as the from endpoint and asLinkSource() as the to endpoint
                String evicted = normalizeNodeId(link.asLinkTarget().name().toString());
                String winner = normalizeNodeId(link.asLinkSource().name().toString());
                if (!evicted.equals(winner)) {
                    evictedToWinner.put(evicted, winner);
                }
            }
        }));
        return evictedToWinner;
    }

    public static String normalizeNodeId(String nodeId) {
        if (nodeId.startsWith("--")) {
            return nodeId.substring(2);
        }
        return nodeId;
    }
}
