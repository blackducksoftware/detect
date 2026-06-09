package com.blackduck.integration.detectable.detectables.sbt.unit;

import com.blackduck.integration.detectable.detectables.sbt.dot.SbtEvictionNodeUtil;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class SbtEvictionNodeUtilTest {

    private MutableGraph parseDot(String body) throws IOException {
        String dot = "digraph \"dependency-graph\" {\n"
            + "    graph[rankdir=\"LR\"]\n"
            + "    edge [arrowtail=\"none\"]\n"
            + body
            + "}";
        InputStream stream = new ByteArrayInputStream(dot.getBytes(StandardCharsets.UTF_8));
        return new Parser().read(stream);
    }

    @Test
    public void noEvictionEdgesReturnsEmptySet() throws IOException {
        String body =
            "    \"com.example:lib:1.0\"[shape=box style=\"\"]\n"
            + "    \"com.example:dep:2.0\"[shape=box style=\"\"]\n"
            + "    \"com.example:lib:1.0\" -> \"com.example:dep:2.0\"\n";

        Set<String> evicted = SbtEvictionNodeUtil.findEvictedNodeIds(parseDot(body));

        Assertions.assertTrue(evicted.isEmpty(), "No eviction edges should yield an empty set");
    }

    @Test
    public void evictionEdgeIdentifiesSourceAsEvicted() throws IOException {
        // guava:27.0 -> guava:30.1 [label="Evicted By"] — only 27.0 is evicted, not 30.1
        String body =
            "    \"com.google.guava:guava:27.0-jre\"[shape=box style=\"dashed\"]\n"
            + "    \"com.google.guava:guava:30.1-jre\"[shape=box style=\"\"]\n"
            + "    \"com.google.guava:guava:27.0-jre\" -> \"com.google.guava:guava:30.1-jre\" [label=\"Evicted By\" style=\"dashed\"]\n";

        Set<String> evicted = SbtEvictionNodeUtil.findEvictedNodeIds(parseDot(body));

        Assertions.assertEquals(1, evicted.size());
        Assertions.assertTrue(evicted.contains("com.google.guava:guava:27.0-jre"),
            "Source of the eviction edge should be evicted");
        Assertions.assertFalse(evicted.contains("com.google.guava:guava:30.1-jre"),
            "Replacement node should not be marked as evicted");
    }

    @Test
    public void multipleEvictedNodesAllDetected() throws IOException {
        String body =
            "    \"com.example:foo:1.0\"[shape=box style=\"dashed\"]\n"
            + "    \"com.example:foo:2.0\"[shape=box style=\"\"]\n"
            + "    \"com.example:bar:1.0\"[shape=box style=\"dashed\"]\n"
            + "    \"com.example:bar:3.0\"[shape=box style=\"\"]\n"
            + "    \"com.example:foo:1.0\" -> \"com.example:foo:2.0\" [label=\"Evicted By\"]\n"
            + "    \"com.example:bar:1.0\" -> \"com.example:bar:3.0\" [label=\"Evicted By\"]\n";

        Set<String> evicted = SbtEvictionNodeUtil.findEvictedNodeIds(parseDot(body));

        Assertions.assertEquals(2, evicted.size());
        Assertions.assertTrue(evicted.contains("com.example:foo:1.0"));
        Assertions.assertTrue(evicted.contains("com.example:bar:1.0"));
    }

    @Test
    public void evictionLabelIsCaseInsensitive() throws IOException {
        String body =
            "    \"com.example:lib:1.0\"[shape=box style=\"dashed\"]\n"
            + "    \"com.example:lib:2.0\"[shape=box style=\"\"]\n"
            + "    \"com.example:lib:1.0\" -> \"com.example:lib:2.0\" [label=\"EVICTED BY\"]\n";

        Set<String> evicted = SbtEvictionNodeUtil.findEvictedNodeIds(parseDot(body));

        Assertions.assertTrue(evicted.contains("com.example:lib:1.0"),
            "Eviction label check should be case-insensitive");
    }
}
