package com.blackduck.integration.detectable.detectables.go.functional;

import java.io.File;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.annotations.FunctionalTest;
import com.blackduck.integration.detectable.detectables.go.gomodfile.GoModFileDetectableOptions;
import com.blackduck.integration.detectable.detectables.go.gomodfile.GoModFileExtractor;
import com.blackduck.integration.detectable.extraction.Extraction;


@FunctionalTest
public class GoModFileDetectableTest {

    private static GoModFileDetectableOptions options = new GoModFileDetectableOptions("https://proxy.golang.org");
    private static File goModFile;
    private static GoModFileExtractor goModFileExtractor;

    @BeforeAll
    protected static void setUp() {
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        goModFile = Paths.get("src", "test", "resources", "detectables", "functional", "go", "gomodfile", "go.mod").toFile();
        goModFileExtractor = new GoModFileExtractor(externalIdFactory);
    }

    @Test
    public void testDependencyExtractionFromGoModFile() {
        Extraction testGoModExtraction = goModFileExtractor.extract(goModFile, options);
        Assertions.assertNotNull(testGoModExtraction);
        Assertions.assertTrue(testGoModExtraction.getCodeLocations().size() == 1);
        DependencyGraph dependencyGraph = testGoModExtraction.getCodeLocations().get(0).getDependencyGraph();
        Assertions.assertNotNull(dependencyGraph);
        Assertions.assertEquals(dependencyGraph.getDirectDependencies().size(), 2);
        // TODO: Add more assertions to verify the graph structure
    }
    

}
