package com.blackduck.integration.detectable.detectables.cargo.transform;

import static org.junit.jupiter.api.Assertions.assertThrows;


import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.graph.builder.MissingExternalIdException;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.cargo.model.CargoLockPackage;
import com.blackduck.integration.detectable.detectables.cargo.transform.CargoLockPackageTransformer;
import com.blackduck.integration.detectable.util.CycleDetectedException;
import com.blackduck.integration.detectable.util.NameOptionalVersion;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import com.blackduck.integration.util.NameVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CargoLockPackageTransformerTest {

    @Test
    public void testParsesNamesAndVersionsSimple() throws DetectableException, MissingExternalIdException, CycleDetectedException {
        List<CargoLockPackage> input = new ArrayList<>();
        input.add(createPackage("test1", "1.0.0"));
        input.add(createPackage("test2", "2.0.0"));
        CargoLockPackageTransformer cargoLockPackageTransformer = new CargoLockPackageTransformer();

        Set<NameVersion> rootDependencies = new HashSet<>();
        rootDependencies.add(new NameVersion("test1", "1.0.0"));
        rootDependencies.add(new NameVersion("test2", "2.0.0"));

        DependencyGraph graph = cargoLockPackageTransformer.transformToGraph(input, null, rootDependencies);

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.CRATES, graph);
        graphAssert.hasRootSize(2);
        graphAssert.hasRootDependency("test1", "1.0.0");
        graphAssert.hasRootDependency("test2", "2.0.0");
    }

    @Test
    public void multiplePackageDefinitionsTest() {
        List<CargoLockPackage> input = new ArrayList<>();
        input.add(createPackage("child1", "version1"));
        input.add(createPackage("child1", "version2"));
        input.add(createPackage("parent1", "anything", new NameOptionalVersion("child1")));

        Set<NameVersion> rootDependencies = new HashSet<>();
        rootDependencies.add(new NameVersion("parent1", "anything"));

        CargoLockPackageTransformer cargoLockPackageTransformer = new CargoLockPackageTransformer();

        assertThrows(DetectableException.class, () -> cargoLockPackageTransformer.transformToGraph(input,null, rootDependencies));
    }

    @Test
    public void testCorrectNumberOfRootDependencies() throws DetectableException, MissingExternalIdException {
        List<CargoLockPackage> input = new ArrayList<>();
        input.add(createPackage("test1", "1.0.0",
            new NameOptionalVersion("dep1"),
            new NameOptionalVersion("dep2")
        ));
        input.add(createPackage("dep1", "0.5.0"));
        input.add(createPackage("dep2", "0.6.0"));
        CargoLockPackageTransformer cargoLockPackageTransformer = new CargoLockPackageTransformer();

        Set<NameVersion> rootDependencies = new HashSet<>();
        rootDependencies.add(new NameVersion("test1", "1.0.0"));
        rootDependencies.add(new NameVersion("dep1", "0.5.0"));
        rootDependencies.add(new NameVersion("dep2", "0.6.0"));

        DependencyGraph graph = cargoLockPackageTransformer.transformToGraph(input,null, rootDependencies);

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.CRATES, graph);
        graphAssert.hasRootDependency("test1", "1.0.0");
        graphAssert.hasRootDependency("dep1", "0.5.0");
        graphAssert.hasRootDependency("dep2", "0.6.0");
        graphAssert.hasRootSize(3);
    }

    private CargoLockPackage createPackage(String name, String version, NameOptionalVersion... dependencies) {
        return new CargoLockPackage(new NameVersion(name, version), Arrays.asList(dependencies));
    }
}
