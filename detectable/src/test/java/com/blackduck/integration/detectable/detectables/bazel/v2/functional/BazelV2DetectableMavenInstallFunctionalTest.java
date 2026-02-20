package com.blackduck.integration.detectable.detectables.bazel.v2.functional;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectables.bazel.v2.unit.BazelDetectableOptionsTestBuilder;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.detectable.detectables.bazel.BazelDetectableOptions;
import com.blackduck.integration.detectable.detectables.bazel.DependencySource;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelV2Detectable;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.util.graph.GraphAssert;

public class BazelV2DetectableMavenInstallFunctionalTest extends DetectableFunctionalTest {

    public BazelV2DetectableMavenInstallFunctionalTest() throws IOException {
        super("BazelV2DetectableMavenInstallFunctionalTest");
    }

    @Override
    protected void setup() throws IOException {
        // Provide a minimal WORKSPACE (not strictly needed) but keep parity with other tests
        addFileFromResources(Path.of("WORKSPACE"), "/bazel/WORKSPACE_multipleRules");

        // Create a small synthetic output that contains maven_coordinates entries so the maven_install pipeline will pick them up
        String outputLine = "some output line tags = [\"maven_coordinates=com.google.guava:guava:27.0-jre\"]";
        ExecutableOutput mavenInstallOutput = createStandardOutput(outputLine);
        // Register the exact cquery command the maven_install pipeline uses
        addExecutableOutput(mavenInstallOutput, "bazel", "cquery", "--noimplicit_deps", "kind(j.*import, deps(//:test))", "--output", "build");

        // Mock Bazel mode detection to return BZLMOD (non-empty stdout, return code 0)
        ExecutableOutput bazelModGraphOutput = createStandardOutput("mocked bazel mod graph output");
        addExecutableOutput(bazelModGraphOutput, "bazel", "mod", "graph");
    }

    @Override
    public BazelV2Detectable create(DetectableEnvironment detectableEnvironment) {
        Set<DependencySource> rules = Collections.singleton(DependencySource.MAVEN_INSTALL);
        BazelDetectableOptions options = BazelDetectableOptionsTestBuilder.builder()
            .target("//:test")
            .dependencySources(rules)
            .build();
        return detectableFactory.createBazelV2Detectable(detectableEnvironment, options, () -> com.blackduck.integration.detectable.ExecutableTarget.forCommand("bazel"));
    }

    @Override
    public void assertExtraction(Extraction extraction) {
        assertEquals(Extraction.ExtractionResultType.SUCCESS, extraction.getResult());
        // Validate dependency graph contains the maven GAV we injected
        com.blackduck.integration.bdio.graph.DependencyGraph graph = extraction.getCodeLocations().get(0).getDependencyGraph();
        // Use GraphAssert + ExternalIdFactory to assert the maven GAV is present in the root of the graph
        Forge mavenForge = new Forge("/", "maven");
        GraphAssert graphAssert = new GraphAssert(mavenForge, graph);
        com.blackduck.integration.bdio.model.externalid.ExternalId expectedMaven = new ExternalIdFactory().createMavenExternalId("com.google.guava", "guava", "27.0-jre");
        graphAssert.hasRootDependency(expectedMaven);
    }
}
