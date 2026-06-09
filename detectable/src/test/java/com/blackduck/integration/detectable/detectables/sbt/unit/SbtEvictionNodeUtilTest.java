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
import java.util.Map;

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
    public void noEvictionEdgesReturnsEmptyMap() throws IOException {
        String body =
            "    \"com.example:lib:1.0\"[shape=box style=\"\"]\n"
            + "    \"com.example:dep:2.0\"[shape=box style=\"\"]\n"
            + "    \"com.example:lib:1.0\" -> \"com.example:dep:2.0\"\n";

        Map<String, String> evictions = SbtEvictionNodeUtil.findEvictions(parseDot(body));

        Assertions.assertTrue(evictions.isEmpty(), "No eviction edges should yield an empty map");
    }

    @Test
    public void evictionEdgeMapsEvictedToWinner() throws IOException {
        // guava:27.0 -> guava:30.1 [label="Evicted By"] — 27.0 is evicted, 30.1 is the winner
        String body =
            "    \"com.google.guava:guava:27.0-jre\"[shape=box style=\"dashed\"]\n"
            + "    \"com.google.guava:guava:30.1-jre\"[shape=box style=\"\"]\n"
            + "    \"com.google.guava:guava:27.0-jre\" -> \"com.google.guava:guava:30.1-jre\" [label=\"Evicted By\" style=\"dashed\"]\n";

        Map<String, String> evictions = SbtEvictionNodeUtil.findEvictions(parseDot(body));

        Assertions.assertEquals(1, evictions.size());
        Assertions.assertEquals("com.google.guava:guava:30.1-jre", evictions.get("com.google.guava:guava:27.0-jre"),
            "Evicted node should map to its winning replacement");
        Assertions.assertFalse(evictions.containsKey("com.google.guava:guava:30.1-jre"),
            "Replacement node should not be a key in the evictions map");
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

        Map<String, String> evictions = SbtEvictionNodeUtil.findEvictions(parseDot(body));

        Assertions.assertEquals(2, evictions.size());
        Assertions.assertEquals("com.example:foo:2.0", evictions.get("com.example:foo:1.0"));
        Assertions.assertEquals("com.example:bar:3.0", evictions.get("com.example:bar:1.0"));
    }

    @Test
    public void evictionLabelIsCaseInsensitive() throws IOException {
        String body =
            "    \"com.example:lib:1.0\"[shape=box style=\"dashed\"]\n"
            + "    \"com.example:lib:2.0\"[shape=box style=\"\"]\n"
            + "    \"com.example:lib:1.0\" -> \"com.example:lib:2.0\" [label=\"EVICTED BY\"]\n";

        Map<String, String> evictions = SbtEvictionNodeUtil.findEvictions(parseDot(body));

        Assertions.assertTrue(evictions.containsKey("com.example:lib:1.0"),
            "Eviction label check should be case-insensitive");
    }
}
