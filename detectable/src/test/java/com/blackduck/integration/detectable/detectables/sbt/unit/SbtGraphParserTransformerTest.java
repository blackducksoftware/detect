package com.blackduck.integration.detectable.detectables.sbt.unit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtDotGraphNodeParser;
import com.blackduck.integration.detectable.detectables.sbt.dot.SbtGraphParserTransformer;

/**
 * Tests for {@link SbtGraphParserTransformer}, focusing on SBT dependency eviction handling.
 * <p>
 * SBT represents eviction in DOT graphs with an edge like:
 *   "com.google.guava:guava:27.0-jre" -> "com.google.guava:guava:30.1-jre" [label="Evicted By"]
 * meaning guava 27.0 was evicted in favor of 30.1.
 * <p>
 * The evicted node (27.0) should be excluded from the dependency graph entirely,
 * while the replacement (30.1) and all other legitimate edges must be preserved.
 */
public class SbtGraphParserTransformerTest {

    private final ExternalIdFactory externalIdFactory = new ExternalIdFactory();
    private final SbtDotGraphNodeParser nodeParser = new SbtDotGraphNodeParser(externalIdFactory);
    private final SbtGraphParserTransformer transformer = new SbtGraphParserTransformer(nodeParser);

    private MutableGraph parseDot(String dotContent) throws IOException {
        InputStream stream = new ByteArrayInputStream(dotContent.getBytes(StandardCharsets.UTF_8));
        return new Parser().read(stream);
    }

    @Test
    public void evictedDependencyIsExcludedFromEntireGraph() throws IOException {
        // DOT graph with guava 27.0 evicted by 30.1.
        // Edges: eviction marker (27.0 -> 30.1), guice -> 30.1, project -> 30.1, project -> guice.
        // Expected: guava 27.0 excluded, guava 30.1 and guice preserved.
        String dot = "digraph \"dependency-graph\" {\n"
            + "    graph[rankdir=\"LR\"]\n"
            + "    edge [arrowtail=\"none\"]\n"
            + "    \"com.google.guava:guava:27.0-jre\"[shape=box label=<com.google.guava<BR/><B>guava</B><BR/>27.0-jre> style=\"dashed\" penwidth=\"3\"]\n"
            + "    \"com.google.guava:guava:30.1-jre\"[shape=box label=<com.google.guava<BR/><B>guava</B><BR/>30.1-jre> style=\"\"]\n"
            + "    \"com.google.inject:guice:5.1.0\"[shape=box label=<com.google.inject<BR/><B>guice</B><BR/>5.1.0> style=\"\"]\n"
            + "    \"default:myproject:1.0\"[shape=box label=<default<BR/><B>myproject</B><BR/>1.0> style=\"\"]\n"
            + "    \"com.google.guava:guava:27.0-jre\" -> \"com.google.guava:guava:30.1-jre\" [label=\"Evicted By\" style=\"dashed\"]\n"
            + "    \"com.google.inject:guice:5.1.0\" -> \"com.google.guava:guava:30.1-jre\"\n"
            + "    \"default:myproject:1.0\" -> \"com.google.guava:guava:30.1-jre\"\n"
            + "    \"default:myproject:1.0\" -> \"com.google.inject:guice:5.1.0\"\n"
            + "}";

        MutableGraph mutableGraph = parseDot(dot);
        Set<String> projectIds = Collections.singleton("default:myproject:1.0");
        DependencyGraph graph = transformer.transformDotToGraph(projectIds, mutableGraph);

        // The evicted guava 27.0 must NOT appear anywhere in the graph.
        ExternalId evictedId = externalIdFactory.createMavenExternalId("com.google.guava", "guava", "27.0-jre");
        Assertions.assertFalse(graph.hasDependency(evictedId),
            "Evicted guava 27.0-jre should not exist anywhere in the dependency graph");

        // The replacement (guava 30.1) must still be present — it is NOT in evictedIds,
        // so edges like "project -> guava:30.1" and "guice -> guava:30.1" are preserved.
        ExternalId replacementId = externalIdFactory.createMavenExternalId("com.google.guava", "guava", "30.1-jre");
        Assertions.assertTrue(graph.hasDependency(replacementId),
            "Replacement guava 30.1-jre should exist in the dependency graph");

        // guice is unrelated to the eviction and must remain in the graph.
        ExternalId guiceId = externalIdFactory.createMavenExternalId("com.google.inject", "guice", "5.1.0");
        Assertions.assertTrue(graph.hasDependency(guiceId),
            "guice 5.1.0 should exist in the dependency graph");
    }

    @Test
    public void evictedDependencyNotAddedAsDirectDependency() throws IOException {
        // Verifies eviction works even when the evicted node's only edge is the eviction marker.
        // DOT graph: lib 1.0 evicted by lib 2.0, project depends on lib 2.0.
        // Expected: lib 1.0 excluded, lib 2.0 is the sole direct dependency.
        String dot = "digraph \"dependency-graph\" {\n"
            + "    graph[rankdir=\"LR\"]\n"
            + "    edge [arrowtail=\"none\"]\n"
            + "    \"org.example:lib:1.0\"[shape=box label=<org.example<BR/><B>lib</B><BR/>1.0> style=\"dashed\"]\n"
            + "    \"org.example:lib:2.0\"[shape=box label=<org.example<BR/><B>lib</B><BR/>2.0> style=\"\"]\n"
            + "    \"default:myproject:1.0\"[shape=box label=<default<BR/><B>myproject</B><BR/>1.0> style=\"\"]\n"
            + "    \"org.example:lib:1.0\" -> \"org.example:lib:2.0\" [label=\"Evicted By\" style=\"dashed\"]\n"
            + "    \"default:myproject:1.0\" -> \"org.example:lib:2.0\"\n"
            + "}";

        MutableGraph mutableGraph = parseDot(dot);
        Set<String> projectIds = Collections.singleton("default:myproject:1.0");
        DependencyGraph graph = transformer.transformDotToGraph(projectIds, mutableGraph);

        // The evicted lib 1.0 must not appear anywhere (not as root, not as transitive).
        ExternalId evictedId = externalIdFactory.createMavenExternalId("org.example", "lib", "1.0");
        Assertions.assertFalse(graph.hasDependency(evictedId),
            "Evicted lib 1.0 should not exist anywhere in the dependency graph");

        // Only the replacement lib 2.0 should remain as a direct dependency.
        Assertions.assertEquals(1, graph.getRootDependencies().size());
        Dependency rootDep = graph.getRootDependencies().iterator().next();
        Assertions.assertEquals("lib", rootDep.getName());
        Assertions.assertEquals("2.0", rootDep.getVersion());
    }
}
