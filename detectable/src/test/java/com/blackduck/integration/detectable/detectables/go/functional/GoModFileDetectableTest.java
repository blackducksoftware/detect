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

    private static GoModFileDetectableOptions options = new GoModFileDetectableOptions("https://proxy.golang.org", 30, 30);
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
        // Check if graph contains github.com/fsnotify/fsnotify v1.90 as a direct dependency
        Assertions.assertTrue(dependencyGraph.getDirectDependencies().stream()
                .anyMatch(dependency -> "github.com/fsnotify/fsnotify".equals(dependency.getName())
                        && "v1.9.0".equals(dependency.getVersion())));
        // Check if graph contains github.com/gin-gonic/gin v1.10.1 as a direct dependency
        Assertions.assertTrue(dependencyGraph.getDirectDependencies().stream()
                .anyMatch(dependency -> "github.com/gin-gonic/gin".equals(dependency.getName())
                        && "v1.10.1".equals(dependency.getVersion())));
        // google.golang.org/protobuf v1.34.1 should not in the children of github.com/gin-gonic/gin
        Assertions.assertFalse(dependencyGraph.getChildrenForParent(dependencyGraph.getDirectDependencies().stream()
                .filter(dependency -> "github.com/gin-gonic/gin".equals(dependency.getName())
                        && "v1.10.1".equals(dependency.getVersion()))
                .findFirst()
                .orElse(null)).stream()
                .anyMatch(dependency -> "google.golang.org/protobuf".equals(dependency.getName())
                        && "v1.34.1".equals(dependency.getVersion())));
        // gopkg.in/yaml.v3 v3.0.0-20200313102051-9f266ea9e77c should not be in the children of github.com/gin-gonic/gin
        Assertions.assertFalse(dependencyGraph.getChildrenForParent(dependencyGraph.getDirectDependencies().stream()
                .filter(dependency -> "github.com/gin-gonic/gin".equals(dependency.getName())
                        && "v1.10.1".equals(dependency.getVersion()))
                .findFirst()
                .orElse(null)).stream()
                .anyMatch(dependency -> "gopkg.in/yaml.v3".equals(dependency.getName())
                        && "v3.0.0-20200313102051-9f266ea9e77c".equals(dependency.getVersion())));
    }

    @Test
    public void testDependencyExtractionFromGoModFileWithInvalidForge() {
        GoModFileDetectableOptions options = new GoModFileDetectableOptions("https://proxy.invalid.go.forge", 30, 30);
        Extraction testGoModExtraction = goModFileExtractor.extract(goModFile, options);
        Assertions.assertNotNull(testGoModExtraction);
        Assertions.assertTrue(testGoModExtraction.getCodeLocations().size() == 1);
        DependencyGraph dependencyGraph = testGoModExtraction.getCodeLocations().get(0).getDependencyGraph();
        Assertions.assertNotNull(dependencyGraph);
        Assertions.assertEquals(dependencyGraph.getDirectDependencies().size(), 3);
        // Check if graph contains github.com/fsnotify/fsnotify v1.90 as a direct dependency
        Assertions.assertTrue(dependencyGraph.getDirectDependencies().stream()
                .anyMatch(dependency -> "github.com/fsnotify/fsnotify".equals(dependency.getName())
                        && "v1.9.0".equals(dependency.getVersion())));
        // Check if graph contains github.com/gin-gonic/gin v1.10.1 as a direct dependency
        Assertions.assertTrue(dependencyGraph.getDirectDependencies().stream()
                .anyMatch(dependency -> "github.com/gin-gonic/gin".equals(dependency.getName())
                        && "v1.10.1".equals(dependency.getVersion())));
        // Check if graph contains "Additional_Components" as a direct dependency
        Assertions.assertTrue(dependencyGraph.getDirectDependencies().stream()
                .anyMatch(dependency -> "Additional_Components".equals(dependency.getName())));
        // google.golang.org/protobuf v1.34.1 should not in the children of github.com/gin-gonic/gin
        Assertions.assertFalse(dependencyGraph.getChildrenForParent(dependencyGraph.getDirectDependencies().stream()
                .filter(dependency -> "github.com/gin-gonic/gin".equals(dependency.getName())
                        && "v1.10.1".equals(dependency.getVersion()))
                .findFirst()
                .orElse(null)).stream()
                .anyMatch(dependency -> "google.golang.org/protobuf".equals(dependency.getName())
                        && "v1.34.1".equals(dependency.getVersion())));
        // gopkg.in/yaml.v3 v3.0.0-20200313102051-9f266ea9e77c should not be in the children of github.com/gin-gonic/gin
        Assertions.assertFalse(dependencyGraph.getChildrenForParent(dependencyGraph.getDirectDependencies().stream()
                .filter(dependency -> "github.com/gin-gonic/gin".equals(dependency.getName())
                        && "v1.10.1".equals(dependency.getVersion()))
                .findFirst()
                .orElse(null)).stream()
                .anyMatch(dependency -> "gopkg.in/yaml.v3".equals(dependency.getName())
                        && "v3.0.0-20200313102051-9f266ea9e77c".equals(dependency.getVersion())));
        // Check Additional_Components has 24 children
        Assertions.assertEquals(24, dependencyGraph.getChildrenForParent(dependencyGraph.getDirectDependencies().stream()
                .filter(dependency -> "Additional_Components".equals(dependency.getName()))
                .findFirst()
                .orElse(null)).size());
    }
    

}
