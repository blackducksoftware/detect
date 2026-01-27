package com.blackduck.integration.detectable.detectables.bazel.v2.functional;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.detectable.detectables.bazel.BazelDetectableOptions;
import com.blackduck.integration.detectable.detectables.bazel.DependencySource;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelV2Detectable;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.util.graph.GraphAssert;

public class BazelV2DetectableFunctionalTest extends DetectableFunctionalTest {

    public BazelV2DetectableFunctionalTest() throws IOException {
        super("BazelV2DetectableFunctionalTest");
    }

    @Override
    protected void setup() throws IOException {
        // Add a WORKSPACE file that contains multiple rules (the resource has examples)
        addFileFromResources(Path.of("WORKSPACE"), "/bazel/WORKSPACE_multipleRules");

        // Add the Haskell JSON proto output resource as the output of the bazel cquery or related command
        ExecutableOutput haskellProto = createStandardOutputFromResource("/bazel/jsonProtoForHaskellCabalLibraries.txt");
        // The Haskell pipeline expects a specific cquery invocation; register that exact command so the functional runner returns our resource
        addExecutableOutput(haskellProto, "bazel", "cquery", "--noimplicit_deps", "kind(haskell_cabal_library, deps(//:test))", "--output", "jsonproto");
    }

    @Override
    public BazelV2Detectable create(DetectableEnvironment detectableEnvironment) {
        // Set dependency sources override so the detectable does not attempt probing (we provide outputs directly)
        Set<DependencySource> rules = Collections.singleton(DependencySource.HASKELL_CABAL_LIBRARY);
        BazelDetectableOptions options = new BazelDetectableOptions("//:test", rules, null, null, 30);
        // Use the factory to create the detectable so we get correct configured helpers
        return detectableFactory.createBazelV2Detectable(detectableEnvironment, options, () -> com.blackduck.integration.detectable.ExecutableTarget.forCommand("bazel"));
    }

    @Override
    public void assertExtraction(Extraction extraction) {
        assertEquals(Extraction.ExtractionResultType.SUCCESS, extraction.getResult());
        // Compute expected project name using the real generator so the test remains stable
        String expected = new com.blackduck.integration.detectable.detectables.bazel.BazelProjectNameGenerator().generateFromBazelTarget("//:test");
        assertEquals(expected, extraction.getProjectName());
        assertEquals(1, extraction.getCodeLocations().size(), "Expected exactly one code location");
        com.blackduck.integration.bdio.graph.DependencyGraph graph = extraction.getCodeLocations().get(0).getDependencyGraph();
        Forge hackageForge = new Forge("/", "hackage");
        GraphAssert graphAssert = new GraphAssert(hackageForge, graph);
        graphAssert.hasRootSize(1);
        com.blackduck.integration.bdio.model.externalid.ExternalId expectedExternalId = new com.blackduck.integration.bdio.model.externalid.ExternalIdFactory().createNameVersionExternalId(hackageForge, "colour", "2.3.5");
        graphAssert.hasRootDependency(expectedExternalId);
    }
}
