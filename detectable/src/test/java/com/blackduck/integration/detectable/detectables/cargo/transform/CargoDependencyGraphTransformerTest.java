package com.blackduck.integration.detectable.detectables.cargo.transform;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CargoDependencyGraphTransformerTest {

    private CargoDependencyGraphTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new CargoDependencyGraphTransformer(new ExternalIdFactory());
    }

    @Test
    void testSingleRootWithChildren() {
        List<String> input = Arrays.asList(
            "0test-project v1.0.0",
            "1tempfile v3.15.0",
            "2cfg-if v1.0.0",
            "2fastrand v2.3.0"
        );

        List<CodeLocation> codeLocations = transformer.transform(input);

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();

        Dependency root = assertDependencyExists(graph.getRootDependencies(), "tempfile", "3.15.0");
        Set<Dependency> children = graph.getChildrenForParent(root);

        assertEquals(2, children.size());
        assertDependencyExists(children, "cfg-if", "1.0.0");
        assertDependencyExists(children, "fastrand", "2.3.0");
    }

    @Test
    void testMultipleRootDependencies() {
        List<String> input = Arrays.asList(
            "0test-project v1.0.0",
            "1tempfile v3.15.0",
            "1tokio v1.0.0",
            "1serde v1.0.0"
        );

        List<CodeLocation> codeLocations = transformer.transform(input);

        assertEquals(1, codeLocations.size());
        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();

        assertEquals(3, graph.getRootDependencies().size());
        assertDependencyExists(graph.getRootDependencies(), "tempfile", "3.15.0");
        assertDependencyExists(graph.getRootDependencies(), "tokio", "1.0.0");
        assertDependencyExists(graph.getRootDependencies(), "serde", "1.0.0");
    }

    @Test
    void testTransformWithInvalidInput() {
        List<String> cargoTreeOutput = Arrays.asList(
            "0test-project v1.0.0",
            "1tempfile v3.15.0",
            "^2fastrand v2.3.0"
        );

        assertThrows(NumberFormatException.class, () -> {
            transformer.transform(cargoTreeOutput);
        });
    }

    private Dependency assertDependencyExists(Set<Dependency> dependencies, String name, String version) {
        return dependencies.stream()
            .filter(d -> d.getName().equals(name) && d.getVersion().equals(version))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing dependency: " + name + " " + version));
    }
}