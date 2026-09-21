package com.blackduck.integration.detectable.detectables.npm.lockfile.unit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.npm.lockfile.NpmDependencyConverter;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.NpmDependency;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.PackageLock;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.PackageLockPackage;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfilePackager;
import com.blackduck.integration.detectable.util.FunctionalTestFiles;
import com.google.gson.Gson;

public class NpmDependencyConverterTest {
    
    private Gson gson;
    private ExternalIdFactory externalIdFactory;
    private NpmDependencyConverter converter;
    private NpmLockfilePackager packager;
    
    @BeforeEach
    public void setup() {
        gson = new Gson();
        externalIdFactory = new ExternalIdFactory();
        converter = new NpmDependencyConverter(externalIdFactory);
        packager = new NpmLockfilePackager(gson, externalIdFactory, null, null);        
    }
    
    @Test
    public void testLinkPackagesDependenciesWithWildcards() {        
        String lockFileText = FunctionalTestFiles.asString("/npm/packages-linkage-test/package-lock-wildcards.json");
        validatePackageLinkage(lockFileText);
    }
    
    @Test
    public void testLinkPackagesDependenciesWithRelativePaths() {
        String lockFileText = FunctionalTestFiles.asString("/npm/packages-linkage-test/package-lock-relative.json");
        validatePackageLinkage(lockFileText);
    }
    
    @Test
    public void testLinkPackagesDependenciesWithWildcardsAndRelativePaths() {
        String lockFileText = FunctionalTestFiles.asString("/npm/packages-linkage-test/package-lock-wildcards-and-relative.json");
        validatePackageLinkage(lockFileText);
    }

    @Test
    public void testLinkPackagesDependenciesExtraneousDependencies() {
        String lockFileText = FunctionalTestFiles.asString("/npm/packages-linkage-test/package-lock-extraneous.json");
        lockFileText = packager.removePathInfoFromPackageName(lockFileText);
        PackageLock packageLock = gson.fromJson(lockFileText, PackageLock.class);
        converter.linkPackagesDependencies(packageLock);
        
        
        Assertions.assertNull(packageLock.packages.get("testpackage"));;
        Assertions.assertNull(packageLock.packages.get("extraneouspackage"));;
        Assertions.assertNull(packageLock.packages.get("testpackage*extraneouspackage"));
        Assertions.assertNull(packageLock.packages.get("node_modules/testpackage/node_modules/extraneouspackage"));
    }
    
    @Test
    public void testAllDependenciesAddedToDependencies() {
        String lockFileText = FunctionalTestFiles.asString("/npm/packages-linkage-test/package-lock-multiple-deps.json");
        lockFileText = packager.removePathInfoFromPackageName(lockFileText);
        PackageLock packageLock = gson.fromJson(lockFileText, PackageLock.class);
        converter.linkPackagesDependencies(packageLock);
        
        PackageLockPackage testPackage = packageLock.packages.get("testpackage");
        
        Assertions.assertNotNull(testPackage);
        Assertions.assertTrue(testPackage.dependencies.containsKey("dep1"));
        Assertions.assertTrue(testPackage.dependencies.containsKey("dev1"));
        Assertions.assertTrue(testPackage.dependencies.containsKey("peer1"));
        Assertions.assertTrue(testPackage.dependencies.containsKey("optional1"));
    }
    
    @Test
    void aliasPackagePreservesKeyAsLookupNameAndReportsActualNameInExternalId() {
        // node_modules/react-is-18 -> { "name": "react-is", "version": "18.3.1" }
        // After removePathInfoFromPackageName the key becomes "react-is-18"
        PackageLockPackage aliasPackage = new PackageLockPackage();
        aliasPackage.name = "react-is";
        aliasPackage.version = "18.3.1";
        Map<String, PackageLockPackage> packages = new HashMap<>();
        packages.put("react-is-18", aliasPackage);

        List<NpmDependency> dependencies = converter.convertLockPackagesToNpmDependencies(null, packages);

        Assertions.assertEquals(1, dependencies.size());
        NpmDependency dep = dependencies.get(0);
        // Lookup name must be the alias key so other packages' "requires" can find it
        Assertions.assertEquals("react-is-18", dep.getName());
        // ExternalId must use the actual npm package name so the BOM identifies the real component
        ExternalId expected = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "react-is", "18.3.1");
        Assertions.assertEquals(expected, dep.getExternalId());
    }

    private void validatePackageLinkage(String lockFileText) {
        lockFileText = packager.removePathInfoFromPackageName(lockFileText);   
        PackageLock packageLock = gson.fromJson(lockFileText, PackageLock.class);  
        
        // In the supplied JSON there is an open source project connect that has a dependency
        // on the open source project finalhandler. Ensure that before linkage we are missing
        // this relationship and that after linkage connect shows finalhandler as a dependency.   
        String parentProject = "connect";
        String childProject = "finalhandler";
        
        PackageLockPackage connectLinkageState = packageLock.packages.get(parentProject);
        Assertions.assertTrue(connectLinkageState.packages == null ||
                !connectLinkageState.packages.containsKey(childProject));
        
        converter.linkPackagesDependencies(packageLock);
        connectLinkageState = packageLock.packages.get(parentProject);
        Assertions.assertTrue(connectLinkageState.packages.containsKey(childProject));
    }
}
