package com.blackduck.integration.detectable.detectables.npm.lockfile.unit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.npm.lockfile.NpmDependencyConverter;
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
