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
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmDependencyInfo;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmDependencyType;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmLockYaml;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmPackageInfo;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLinkedPackageResolver;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmYamlTransformer;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonReader;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;

public class PnpmYamlTransformerTestv6 {
    NameVersion projectNameVersion = new NameVersion("name", "version");
    PnpmLinkedPackageResolver linkedPackageResolver = new PnpmLinkedPackageResolver(new File(""), new PackageJsonFiles(new PackageJsonReader(new Gson())));
    File pnpmLockYamlFile = new File("something");

    private PnpmYamlTransformer createTransformer(PnpmDependencyType... excludedDependencyTypes) {
        EnumListFilter<PnpmDependencyType> dependencyTypeFilter = EnumListFilter.fromExcluded(excludedDependencyTypes);
        return new PnpmYamlTransformer(dependencyTypeFilter, "6");
    }

    @Test
    public void testGenerateCodeLocation() throws IntegrationException {
        PnpmLockYaml pnpmLockYaml = createPnpmLockYaml();
        PnpmYamlTransformer transformer = createTransformer();
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
        PnpmYamlTransformer transformer = createTransformer(PnpmDependencyType.DEV);
        PnpmLockYaml pnpmLockYaml = createPnpmLockYaml();
        DependencyGraph dependencyGraph = transformer.generateCodeLocation(pnpmLockYamlFile, pnpmLockYaml, projectNameVersion, linkedPackageResolver).getDependencyGraph();
        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, dependencyGraph);
        graphAssert.hasRootSize(3);
        graphAssert.hasNoDependency("devDep", "2.0.0");
    }

    @Test
    public void testExcludeOptionalDependencies() throws IntegrationException {
        PnpmLockYaml pnpmLockYaml = createPnpmLockYaml();
        PnpmYamlTransformer transformer = createTransformer(PnpmDependencyType.OPTIONAL);
        DependencyGraph dependencyGraph = transformer.generateCodeLocation(pnpmLockYamlFile, pnpmLockYaml, projectNameVersion, linkedPackageResolver).getDependencyGraph();
        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, dependencyGraph);
        graphAssert.hasRootSize(3);
        graphAssert.hasNoDependency("optDep", "3.0.0");
    }

    @Test
    public void testThrowExceptionOnNullPackagesSection() {
        PnpmLockYaml pnpmLockYaml = createPnpmLockYaml();
        PnpmYamlTransformer transformer = createTransformer();
        pnpmLockYaml.packages = null;
        assertThrows(
            IntegrationException.class,
            () -> transformer.generateCodeLocation(pnpmLockYamlFile, pnpmLockYaml, projectNameVersion, linkedPackageResolver)
        );
    }

    @Test
    public void testNoFailureOnNullNameVersion() throws IntegrationException {
        PnpmLockYaml pnpmLockYaml = createPnpmLockYaml();
        PnpmYamlTransformer transformer = createTransformer();
        transformer.generateCodeLocation(pnpmLockYamlFile, pnpmLockYaml, null, linkedPackageResolver);
    }

    @Test
    public void testParseFileDependencies() throws IntegrationException {
        PnpmLockYaml pnpmLockYaml = createPnpmLockYaml();
        PnpmYamlTransformer transformer = createTransformer(PnpmDependencyType.DEV, PnpmDependencyType.OPTIONAL);
        DependencyGraph dependencyGraph = transformer.generateCodeLocation(pnpmLockYamlFile, pnpmLockYaml, projectNameVersion, linkedPackageResolver).getDependencyGraph();
        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, dependencyGraph);
        graphAssert.hasRootSize(2);
        graphAssert.hasDependency("fileDep", "1.0.0");
    }

    private PnpmLockYaml createPnpmLockYaml() {
        PnpmLockYaml pnpmLockYaml = new PnpmLockYaml();

        Map<String, PnpmDependencyInfo> dependencies = new HashMap<>();
        PnpmDependencyInfo dependency1 = new PnpmDependencyInfo();
        dependency1.version = "1.0.0";
        dependencies.put("dep", dependency1);
        PnpmDependencyInfo dependency2 = new PnpmDependencyInfo();
        dependency2.version = "file:fileDep-1.0.0.tgz";
        dependencies.put("fileDep", dependency2);
        pnpmLockYaml.dependencies = dependencies;

        Map<String, PnpmDependencyInfo> devDependencies = new HashMap<>();
        PnpmDependencyInfo devDependency1 = new PnpmDependencyInfo();
        devDependency1.version = "2.0.0";
        devDependencies.put("devDep", devDependency1);
        pnpmLockYaml.devDependencies = devDependencies;

        Map<String, PnpmDependencyInfo> optionalDependencies = new HashMap<>();
        PnpmDependencyInfo optDependency1 = new PnpmDependencyInfo();
        optDependency1.version = "3.0.0";
        optionalDependencies.put("optDep", optDependency1);
        pnpmLockYaml.optionalDependencies = optionalDependencies;

        Map<String, PnpmPackageInfo> packages = new HashMap<>();

        PnpmPackageInfo dep = new PnpmPackageInfo();
        Map<String, String> depDependencies = new HashMap<>();
        depDependencies.put("transitive", "1.1.0");
        dep.dependencies = depDependencies;
        packages.put("/dep@1.0.0", dep);

        PnpmPackageInfo devDep = new PnpmPackageInfo();
        devDep.dev = true;
        packages.put("/devDep@2.0.0", devDep);

        PnpmPackageInfo optDep = new PnpmPackageInfo();
        optDep.optional = true;
        packages.put("/optDep@3.0.0", optDep);

        PnpmPackageInfo transitive = new PnpmPackageInfo();
        packages.put("/transitive@1.1.0", transitive);

        PnpmPackageInfo fileDep = new PnpmPackageInfo();
        fileDep.name = "fileDep";
        fileDep.version = "1.0.0";
        packages.put("file:fileDep-1.0.0.tgz", fileDep);

        pnpmLockYaml.packages = packages;

        return pnpmLockYaml;
    }
}
