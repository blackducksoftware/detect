package com.blackduck.integration.detectable.detectables.go.gomod.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.go.gomod.model.GoListAllData;
import com.blackduck.integration.detectable.detectables.go.gomod.model.ReplaceData;
import com.blackduck.integration.detectable.detectables.go.gomod.process.GoModDependencyManager;

class GoModDependencyManagerTest {
    @Test
    void happyPathTest() {
        String moduleName = "example.io/module/a";
        String moduleVersion = "1.0.0";
        GoModDependencyManager dependencyManager = createManagerWithModuleLoadedWith(moduleName, moduleVersion);
        Dependency dependency = dependencyManager.getDependencyForModule(moduleName);
        assertEquals(moduleName, dependency.getName());
        assertEquals(moduleVersion, dependency.getVersion());
    }

    @Test
    void versionTruncatedHashTest() {
        String moduleName = "example.io/module/a";
        String moduleVersion = "v0.0.0-20180917221912-90fa682c2a6e";
        GoModDependencyManager dependencyManager = createManagerWithModuleLoadedWith(moduleName, moduleVersion);
        Dependency dependency = dependencyManager.getDependencyForModule(moduleName);
        assertEquals(moduleName, dependency.getName());
        assertEquals("90fa682c2a6e", dependency.getVersion());
    }

    @Test
    void versionWithIncompatible() {
        String moduleName = "example.io/incompatible";
        String moduleVersion = "2.0.0+incompatible";
        GoModDependencyManager dependencyManager = createManagerWithModuleLoadedWith(moduleName, moduleVersion);
        Dependency dependency = dependencyManager.getDependencyForModule(moduleName);
        assertEquals(moduleName, dependency.getName());
        assertEquals("2.0.0", dependency.getVersion(), "The '+incompatible' should have been stripped from the version.");
    }

    @Test
    void versionWithGitHash() {
        String moduleName = "example.io/hash";
        String moduleVersion = "version_with_sha1_hash-62ecb2f7638dbe3bcb6b4b92540333a8b61fcd1c";
        GoModDependencyManager dependencyManager = createManagerWithModuleLoadedWith(moduleName, moduleVersion);
        Dependency dependency = dependencyManager.getDependencyForModule(moduleName);
        assertEquals(moduleName, dependency.getName());
        assertEquals("62ecb2f7638dbe3bcb6b4b92540333a8b61fcd1c", dependency.getVersion(), "When a SHA1 is present, other version text should be stripped.");
    }

    @Test
    void versionWithDash() {
        String moduleName = "example.io/dash";
        String moduleVersion = "1.2.3-preview4";
        GoModDependencyManager dependencyManager = createManagerWithModuleLoadedWith(moduleName, moduleVersion);
        Dependency dependency = dependencyManager.getDependencyForModule(moduleName);
        assertEquals(moduleName, dependency.getName());
        assertEquals(moduleVersion, dependency.getVersion(), "The version does not contain a commit and should be unmodified.");
    }

    @Test
    void nonExistentModuleTest() {
        String moduleName = "non-existent.io/module";
        GoModDependencyManager dependencyManager = createManagerWithModuleLoadedWith("something-else", "1.2.3");
        Dependency dependency = dependencyManager.getDependencyForModule(moduleName);
        assertEquals(moduleName, dependency.getName(), "A dependency should have been created from the query.");
        assertNull(dependency.getVersion(), "There should not be a version to match the given query.");
    }

    @Test
    void nonExistentVersionTest() {
        String moduleName = "non-existent.io/module";
        GoModDependencyManager dependencyManager = createManagerWithModuleLoadedWith(moduleName, null);
        Dependency dependency = dependencyManager.getDependencyForModule(moduleName);
        assertEquals(moduleName, dependency.getName(), "A dependency should have been created from the query.");
        assertNull(dependency.getVersion(), "There should not be a version to match the given query.");
    }

    @Test
    void moduleReplacementTest() {
        String resolvedModulePath = "example.io/module/resolved";
        String replacedModulePath = "example.io/module/replaced";

        GoListAllData resolvedModule = new GoListAllData();
        resolvedModule.setPath(resolvedModulePath);
        resolvedModule.setVersion("1.0.0");

        GoListAllData replacedModule = new GoListAllData();
        replacedModule.setPath(replacedModulePath);
        replacedModule.setVersion("2.0.0");
        ReplaceData replaceData = new ReplaceData();
        replaceData.setPath(resolvedModulePath);
        replaceData.setVersion("2.3.4");
        replacedModule.setReplace(replaceData);

        GoModDependencyManager dependencyManager = new GoModDependencyManager(Arrays.asList(resolvedModule, replacedModule), new ExternalIdFactory());

        Dependency resolvedDependency = dependencyManager.getDependencyForModule(resolvedModule.getPath());
        assertEquals(resolvedModule.getPath(), resolvedDependency.getName());
        assertEquals(resolvedModule.getVersion(), resolvedDependency.getVersion());

        Dependency replacedDependency = dependencyManager.getDependencyForModule(replacedModulePath);
        assertEquals(replaceData.getPath(), replacedDependency.getName(), "The dependency name (module path) should have been replaced.");
        assertEquals(replaceData.getVersion(), replacedDependency.getVersion(), "The version should have been replaced.");
    }

    private GoModDependencyManager createManagerWithModuleLoadedWith(String modulePath, @Nullable String moduleVersion) {
        GoListAllData module = new GoListAllData();
        module.setPath(modulePath);
        module.setVersion(moduleVersion);
        return new GoModDependencyManager(Collections.singletonList(module), new ExternalIdFactory());
    }
}
