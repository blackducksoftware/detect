package com.blackduck.integration.detectable.detectables.pip.inspector.functional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.annotations.FunctionalTest;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.pip.inspector.PipInspectorExtractor;
import com.blackduck.integration.detectable.detectables.pip.inspector.parser.PipInspectorTreeParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.util.ToolVersionLogger;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.executable.ExecutableOutput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FunctionalTest
public class PipInspectorExtractorFunctionalTest {

    private Path tempRequirements;

    @AfterEach
    void cleanup() throws Exception {
        if (tempRequirements != null) {
            Files.deleteIfExists(tempRequirements);
        }
    }

    @Test
    void extractProducesCodeLocationWithDependencyTree() throws Exception {
        // Arrange: create a requirements file (content not read by extractor, but path is passed to inspector)
        tempRequirements = Files.createTempFile("requirements", ".txt");
        Files.write(tempRequirements, Arrays.asList(
            "requests==2.31.0",
            "charset.normalizer==3.3.1"
        ));

        DetectableExecutableRunner mockRunner = mock(DetectableExecutableRunner.class);
        ToolVersionLogger mockLogger = mock(ToolVersionLogger.class);
        PipInspectorTreeParser parser = new PipInspectorTreeParser(new ExternalIdFactory());
        PipInspectorExtractor extractor = new PipInspectorExtractor(mockRunner, parser, mockLogger);

        // Mock inspector output: unknown project root plus resolved dependency tree under requests
        List<String> lines = Arrays.asList(
            "p?" + tempRequirements.toAbsolutePath(),  // unparseable/parse error line is ignored by parser
            "n?==v?",                                  // unknown project root (name/version empty)
            "    requests==2.31.0",
            "        charset-normalizer==3.3.1",
            "        idna==3.11",
            "        urllib3==2.5.0",
            "        certifi==2025.11.12"
        );
        when(mockRunner.execute(any())).thenReturn(new ExecutableOutput(String.join(System.lineSeparator(), lines), ""));

        Extraction extraction = extractor.extract(
            new File("."),
            ExecutableTarget.forCommand("python"),
            ExecutableTarget.forCommand("pip"),
            new File("pip-inspector.py"),
            null,
            Collections.singletonList(tempRequirements),
            "",
            null
        );

        List<CodeLocation> codeLocations = extraction.getCodeLocations();
        assertEquals(1, codeLocations.size(), "Expected a single CodeLocation");

        CodeLocation codeLocation = codeLocations.get(0);
        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.PYPI, codeLocation.getDependencyGraph());
        graphAssert.hasRootSize(1);
        graphAssert.hasRootDependency("requests", "2.31.0");
        graphAssert.hasParentChildRelationship("requests", "2.31.0", "charset-normalizer", "3.3.1");
        graphAssert.hasParentChildRelationship("requests", "2.31.0", "idna", "3.11");
        graphAssert.hasParentChildRelationship("requests", "2.31.0", "urllib3", "2.5.0");
        graphAssert.hasParentChildRelationship("requests", "2.31.0", "certifi", "2025.11.12");
    }
}
