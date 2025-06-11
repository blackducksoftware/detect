package com.blackduck.integration.detectable.detectables.npm.packagejson.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.annotations.UnitTest;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.npm.NpmDependencyType;
import com.blackduck.integration.detectable.detectables.npm.packagejson.CombinedPackageJson;
import com.blackduck.integration.detectable.detectables.npm.packagejson.PackageJsonExtractor;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.util.graph.GraphAssert;

@UnitTest
class PackageJsonExtractorTest {
    private ExternalId testDep1;
    private ExternalId testDep2;
    private ExternalId testDevDep1;
    private ExternalId testDevDep2;

    @BeforeEach
    void setUp() {
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        testDep1 = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "name1", "version1");
        testDep2 = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "name2", "version2");
        testDevDep1 = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "nameDev1", "versionDev1");
        testDevDep2 = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "nameDev2", "versionDev2");
    }

    private PackageJsonExtractor createExtractor(NpmDependencyType... excludedTypes) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        EnumListFilter<NpmDependencyType> npmDependencyTypeFilter = EnumListFilter.fromExcluded(excludedTypes);
        return new PackageJsonExtractor(gson, new ExternalIdFactory(), npmDependencyTypeFilter);
    }

    @Test
    void extractWithNoDevOrPeerDependencies() {
        CombinedPackageJson packageJson = createPackageJson();
        Extraction extraction = createExtractor(NpmDependencyType.DEV, NpmDependencyType.PEER).extract(packageJson);
        assertEquals(1, extraction.getCodeLocations().size());
        CodeLocation codeLocation = extraction.getCodeLocations().get(0);
        DependencyGraph dependencyGraph = codeLocation.getDependencyGraph();

        GraphAssert graphAssert = new GraphAssert(Forge.NPMJS, dependencyGraph);
        graphAssert.hasRootDependency(testDep1);
        graphAssert.hasRootDependency(testDep2);
        graphAssert.hasNoDependency(testDevDep1);
        graphAssert.hasNoDependency(testDevDep2);
        graphAssert.hasRootSize(2);
    }

    @Test
    void extractWithDevNoPeerDependencies() {
        CombinedPackageJson packageJson = createPackageJson();
        Extraction extraction = createExtractor(NpmDependencyType.PEER).extract(packageJson);
        assertEquals(1, extraction.getCodeLocations().size());
        CodeLocation codeLocation = extraction.getCodeLocations().get(0);
        DependencyGraph dependencyGraph = codeLocation.getDependencyGraph();

        GraphAssert graphAssert = new GraphAssert(Forge.NPMJS, dependencyGraph);
        graphAssert.hasRootDependency(testDep1);
        graphAssert.hasRootDependency(testDep2);
        graphAssert.hasRootDependency(testDevDep1);
        graphAssert.hasRootDependency(testDevDep2);
        graphAssert.hasRootSize(4);
    }
    
    @Test
    void extractWithNonNumericalCharacters() {
        CombinedPackageJson packageJson = new CombinedPackageJson();
        packageJson.getDependencies().put("foo", "1.0.0 - 2.9999.9999");
        packageJson.getDependencies().put("bar", ">=1.0.2 <2.1.2");
        packageJson.getDependencies().put("qux", "<1.0.0 || >=2.3.1 <2.4.5 || >=2.5.2 <3.0.0");
        packageJson.getDependencies().put("asd", "http://asdf.com/asdf.tar.gz");
        packageJson.getDependencies().put("til", "~1.2");
        packageJson.getDependencies().put("thr", "3.3.x");
        packageJson.getDependencies().put("myproject", "1.0.0-SNAPSHOT");
        
        Extraction extraction = createExtractor(NpmDependencyType.PEER).extract(packageJson);
        CodeLocation codeLocation = extraction.getCodeLocations().get(0);
        DependencyGraph dependencyGraph = codeLocation.getDependencyGraph();
        
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        ExternalId testCharDep1 = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "foo", "1.0.0");
        ExternalId testCharDep2 = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "bar", "1.0.2");
        ExternalId testCharDep3 = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "qux", "1.0.0");
        ExternalId testCharDep4 = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "asd", "http://asdf.com/asdf.tar.gz");
        ExternalId testCharDep5 = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "til", "1.2");
        ExternalId testCharDep6 = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "thr", "3.3.0");
        ExternalId testCharDep7 = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "myproject", "1.0.0");        
        
        GraphAssert graphAssert = new GraphAssert(Forge.NPMJS, dependencyGraph);
        graphAssert.hasRootDependency(testCharDep1);
        graphAssert.hasRootDependency(testCharDep2);
        graphAssert.hasRootDependency(testCharDep3);
        graphAssert.hasRootDependency(testCharDep4);
        graphAssert.hasRootDependency(testCharDep5);
        graphAssert.hasRootDependency(testCharDep6);
        graphAssert.hasRootDependency(testCharDep7);
        graphAssert.hasRootSize(7);
    }

    @Test
    void shouldExtractLowestSemVerCorrectly() {
        PackageJsonExtractor extractor = createExtractor();

        assertEquals("1.0.0", extractor.extractLowestVersion("1.0.0 - 2.9999.9999"));
        assertEquals("1.0.2", extractor.extractLowestVersion(">=1.0.2 <2.1.2"));
        assertEquals("1.0.0", extractor.extractLowestVersion("<1.0.0 || >=2.3.1 <2.4.5 || >=2.5.2 <3.0.0"));
        assertEquals("1.2.0", extractor.extractLowestVersion("~1.2.0"));
        assertEquals("3.3.0", extractor.extractLowestVersion("3.3.x"));
        assertEquals("1.0.0", extractor.extractLowestVersion("1.0.0-SNAPSHOT"));
        assertEquals("1.2.3", extractor.extractLowestVersion("^1.2.3"));
        assertEquals("1.2.3", extractor.extractLowestVersion("1.2.3-alpha"));
        assertEquals("17.0.0", extractor.extractLowestVersion("17.0.0-1551262265873"));
        assertEquals("3.2.0", extractor.extractLowestVersion("^3.2.0 || 4.0.1"));
        assertEquals("1.2.0", extractor.extractLowestVersion("~1.2.x"));
        assertEquals("1.2.3", extractor.extractLowestVersion("1.2.3-beta.2"));
        assertEquals("5.0.0", extractor.extractLowestVersion("5.0.0-alpha+build.1"));
        assertEquals("3.0.0", extractor.extractLowestVersion("3.0.0-abc123"));
        assertEquals("3.0.0", extractor.extractLowestVersion("3.0.0-1"));
        assertEquals("10.0.0", extractor.extractLowestVersion("10.0.0-0000000000123"));
        assertEquals("2.1.0", extractor.extractLowestVersion("2.1.0-beta.1+exp.sha.5114f85"));
        assertEquals("4.0.1", extractor.extractLowestVersion("4.0.1+build.meta"));
        assertEquals("1.0.0", extractor.extractLowestVersion("1.0.0 - 1.5.0"));
        assertEquals("3.0.0", extractor.extractLowestVersion("3.0.0-1.5.0"));
        assertEquals("1.0.0", extractor.extractLowestVersion("^1.0.x"));
        assertEquals("2.0", extractor.extractLowestVersion("2.*"));
        assertEquals("0.9.1", extractor.extractLowestVersion(">=0.9.1 || 1.0.0"));
        assertEquals("3.4.5", extractor.extractLowestVersion("3.4.5"));
        assertEquals("1.0.0", extractor.extractLowestVersion("1.0.0 || 2.0.0"));
    }


    private CombinedPackageJson createPackageJson() {
        CombinedPackageJson combinedPackageJson = new CombinedPackageJson();

        combinedPackageJson.setName("test");
        combinedPackageJson.setVersion("test-version");

        combinedPackageJson.getDependencies().put(testDep1.getName(), testDep1.getVersion());
        combinedPackageJson.getDependencies().put(testDep2.getName(), testDep2.getVersion());
        combinedPackageJson.getDevDependencies().put(testDevDep1.getName(), testDevDep1.getVersion());
        combinedPackageJson.getDevDependencies().put(testDevDep2.getName(), testDevDep2.getVersion());

        return combinedPackageJson;
    }
}
