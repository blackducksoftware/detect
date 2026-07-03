package com.blackduck.integration.detectable.detectables.pnpm.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmDependencyType;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmLockYamlv5;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmPackageInfov5;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLinkedPackageResolver;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmYamlTransformerv5;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonReader;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;

public class PnpmYamlTransformerTest {
    NameVersion projectNameVersion = new NameVersion("name", "version");
    PnpmLinkedPackageResolver linkedPackageResolver = new PnpmLinkedPackageResolver(new File(""), new PackageJsonFiles(new PackageJsonReader(new Gson())));
    File pnpmLockYamlFile = new File("something");

    private PnpmYamlTransformerv5 createTransformer(PnpmDependencyType... excludedDependencyTypes) {
        EnumListFilter<PnpmDependencyType> dependencyTypeFilter = EnumListFilter.fromExcluded(excludedDependencyTypes);
        return new PnpmYamlTransformerv5(dependencyTypeFilter);
    }

    @Test
    public void testGenerateCodeLocation() throws IntegrationException {
        PnpmLockYamlv5 pnpmLockYaml = createPnpmLockYaml();
        PnpmYamlTransformerv5 transformer = createTransformer();
        CodeLocation codeLocation = transformer.generateCodeLocation(pnpmLockYamlFile, pnpmLockYaml, projectNameVersion, linkedPackageResolver);

        assertTrue(codeLocation.getExternalId().isPresent(), "Expected the codelocation to produce an ExternalId.");
        assertEquals("name", codeLocation.getExternalId().get().getName());
        assertEquals("version", codeLocation.getExternalId().get().getVersion());

        DependencyGraph dependencyGraph = codeLocation.getDependencyGraph();
        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, dependencyGraph);
        graphAssert.hasRootSize(4);
        graphAssert.hasRootDependency("dep", "1.0.0");
        graphAssert.hasRootDependency("devDep", "2.0.0");
        graphAssert.hasRootDependency("optDep", "3.0.0");
        graphAssert.hasParentChildRelationship("dep", "1.0.0", "transitive", "1.1.0");
    }

    @Test
    public void testExcludeDevDependencies() throws IntegrationException {
        PnpmYamlTransformerv5 transformer = createTransformer(PnpmDependencyType.DEV);
        PnpmLockYamlv5 pnpmLockYaml = createPnpmLockYaml();
        DependencyGraph dependencyGraph = transformer.generateCodeLocation(pnpmLockYamlFile, pnpmLockYaml, projectNameVersion, linkedPackageResolver).getDependencyGraph();
        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, dependencyGraph);
        graphAssert.hasRootSize(3);
        graphAssert.hasNoDependency("devDep", "2.0.0");
    }

    @Test
    public void testExcludeOptionalDependencies() throws IntegrationException {
        PnpmLockYamlv5 pnpmLockYaml = createPnpmLockYaml();
        PnpmYamlTransformerv5 transformer = createTransformer(PnpmDependencyType.OPTIONAL);
        DependencyGraph dependencyGraph = transformer.generateCodeLocation(pnpmLockYamlFile, pnpmLockYaml, projectNameVersion, linkedPackageResolver).getDependencyGraph();
        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, dependencyGraph);
        graphAssert.hasRootSize(3);
        graphAssert.hasNoDependency("optDep", "3.0.0");
    }

    @Test
    public void testThrowExceptionOnNullPackagesSection() {
        PnpmLockYamlv5 pnpmLockYaml = createPnpmLockYaml();
        PnpmYamlTransformerv5 transformer = createTransformer();
        pnpmLockYaml.packages = null;
        assertThrows(
            IntegrationException.class,
            () -> transformer.generateCodeLocation(pnpmLockYamlFile, pnpmLockYaml, projectNameVersion, linkedPackageResolver)
        );
    }

    @Test
    public void testNoFailureOnNullNameVersion() throws IntegrationException {
        PnpmLockYamlv5 pnpmLockYaml = createPnpmLockYaml();
        PnpmYamlTransformerv5 transformer = createTransformer();
        transformer.generateCodeLocation(pnpmLockYamlFile, pnpmLockYaml, null, linkedPackageResolver);
    }

    @Test
    public void testParseFileDependencies() throws IntegrationException {
        PnpmLockYamlv5 pnpmLockYaml = createPnpmLockYaml();
        PnpmYamlTransformerv5 transformer = createTransformer(PnpmDependencyType.DEV, PnpmDependencyType.OPTIONAL);
        DependencyGraph dependencyGraph = transformer.generateCodeLocation(pnpmLockYamlFile, pnpmLockYaml, projectNameVersion, linkedPackageResolver).getDependencyGraph();
        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, dependencyGraph);
        graphAssert.hasRootSize(2);
        graphAssert.hasDependency("fileDep", "1.0.0");
    }

    private PnpmLockYamlv5 createPnpmLockYaml() {
        PnpmLockYamlv5 pnpmLockYaml = new PnpmLockYamlv5();

        Map<String, String> dependencies = new HashMap<>();
        dependencies.put("dep", "1.0.0");
        dependencies.put("fileDep", "file:fileDep-1.0.0.tgz");
        pnpmLockYaml.dependencies = dependencies;

        Map<String, String> devDependencies = new HashMap<>();
        devDependencies.put("devDep", "2.0.0");
        pnpmLockYaml.devDependencies = devDependencies;

        Map<String, String> optionalDependencies = new HashMap<>();
        optionalDependencies.put("optDep", "3.0.0");
        pnpmLockYaml.optionalDependencies = optionalDependencies;

        Map<String, PnpmPackageInfov5> packages = new HashMap<>();

        PnpmPackageInfov5 dep = new PnpmPackageInfov5();
        Map<String, String> depDependencies = new HashMap<>();
        depDependencies.put("transitive", "1.1.0");
        dep.dependencies = depDependencies;
        packages.put("/dep/1.0.0", dep);

        PnpmPackageInfov5 devDep = new PnpmPackageInfov5();
        devDep.dev = true;
        packages.put("/devDep/2.0.0", devDep);

        PnpmPackageInfov5 optDep = new PnpmPackageInfov5();
        optDep.optional = true;
        packages.put("/optDep/3.0.0", optDep);

        PnpmPackageInfov5 transitive = new PnpmPackageInfov5();
        packages.put("/transitive/1.1.0", transitive);

        PnpmPackageInfov5 fileDep = new PnpmPackageInfov5();
        fileDep.name = "fileDep";
        fileDep.version = "1.0.0";
        packages.put("file:fileDep-1.0.0.tgz", fileDep);

        pnpmLockYaml.packages = packages;

        return pnpmLockYaml;
    }
}
