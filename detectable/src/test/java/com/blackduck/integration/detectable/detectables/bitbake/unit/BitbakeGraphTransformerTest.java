package com.blackduck.integration.detectable.detectables.bitbake.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.annotations.UnitTest;
import com.blackduck.integration.detectable.detectables.bitbake.model.BitbakeGraph;
import com.blackduck.integration.detectable.detectables.bitbake.parse.GraphNodeLabelParser;
import com.blackduck.integration.detectable.detectables.bitbake.transform.BitbakeGraphTransformer;

@UnitTest
public class BitbakeGraphTransformerTest {

    private MutableGraph buildGraph(String content) throws IOException {
        String dot = "digraph depends {\n" + content + "\n}";
        InputStream stream = new ByteArrayInputStream(dot.getBytes(StandardCharsets.UTF_8));
        return new Parser().read(stream);
    }

    @Test
    public void parsedVersionFromLabel() throws IOException {
        String content = "\"name.do_build\" [label = \"name\\n:version\\n/some/meta/path/to.bb\"]\n";
        Set<String> knownLayers = new HashSet<>(Arrays.asList("aaa", "meta", "bbb"));
        BitbakeGraph bitbakeGraph = new BitbakeGraphTransformer(new GraphNodeLabelParser()).transform(buildGraph(content), knownLayers);

        assertEquals(1, bitbakeGraph.getNodes().size());
        assertTrue(bitbakeGraph.getNodes().get(0).getVersion().isPresent());
        assertEquals("version", bitbakeGraph.getNodes().get(0).getVersion().get());
        assertTrue(bitbakeGraph.getNodes().get(0).getLayer().isPresent());
        assertEquals("meta", bitbakeGraph.getNodes().get(0).getLayer().get());
    }

    @Test
    public void parsedRelationship() throws IOException {
        String content =
            "\"parent.do_build\" [label = \"name\\n:parent.version\\n/some/meta/path/to.bb\"]\n" +
            "\"child.do_build\" [label = \"name\\n:child.version\\n/some/meta/path/to.bb\"]\n" +
            "\"parent.do_build\" -> \"child.do_build\"\n";
        Set<String> knownLayers = new HashSet<>(Arrays.asList("aaa", "meta", "bbb"));
        BitbakeGraph bitbakeGraph = new BitbakeGraphTransformer(new GraphNodeLabelParser()).transform(buildGraph(content), knownLayers);

        assertEquals(2, bitbakeGraph.getNodes().size());
        assertEquals(1, bitbakeGraph.getNodes().get(0).getChildren().size());
        assertTrue(bitbakeGraph.getNodes().get(0).getChildren().contains("child"), "Parent node children must contain child");
    }

    @Test
    public void removedQuotesFromName() throws IOException {
        String content = "\"quotes\\\"removed.do_build\" [label = \"example\\n:example\\n/example/meta/some.bb\"]\n";
        Set<String> knownLayers = new HashSet<>(Arrays.asList("aaa", "meta", "bbb"));
        BitbakeGraph bitbakeGraph = new BitbakeGraphTransformer(new GraphNodeLabelParser()).transform(buildGraph(content), knownLayers);

        assertEquals(1, bitbakeGraph.getNodes().size());
        assertEquals("quotesremoved", bitbakeGraph.getNodes().get(0).getName());
    }

}
