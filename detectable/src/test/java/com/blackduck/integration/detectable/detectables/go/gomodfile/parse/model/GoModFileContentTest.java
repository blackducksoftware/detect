package com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;



class GoModFileContentTest {

    @Test
    void constructorAndGettersTest() {
        // Arrange
        String moduleName = "example.com/mymodule";
        String goVersion = "1.21";
        String toolchainVersion = "go1.21.0";
        
        GoModuleInfo directDep = new GoModuleInfo("direct.dep", "v1.0.0");
        GoModuleInfo indirectDep = new GoModuleInfo("indirect.dep", "v2.0.0");
        GoModuleInfo excludedModule = new GoModuleInfo("excluded.module", "v3.0.0");
        GoModuleInfo oldModule = new GoModuleInfo("old.module", "v1.0.0");
        GoModuleInfo newModule = new GoModuleInfo("new.module", "v1.0.0");
        GoReplaceDirective replaceDirective = new GoReplaceDirective(oldModule, newModule);
        GoModuleInfo retractedVersion = new GoModuleInfo("retracted.module", "v4.0.0");
        
        List<GoModuleInfo> directDependencies = Arrays.asList(directDep);
        List<GoModuleInfo> indirectDependencies = Arrays.asList(indirectDep);
        Set<GoModuleInfo> excludedModules = new HashSet<>(Arrays.asList(excludedModule));
        List<GoReplaceDirective> replaceDirectives = Arrays.asList(replaceDirective);
        Set<GoModuleInfo> retractedVersions = new HashSet<>(Arrays.asList(retractedVersion));

        // Act
        GoModFileContent content = new GoModFileContent(
            moduleName,
            goVersion,
            toolchainVersion,
            directDependencies,
            indirectDependencies,
            excludedModules,
            replaceDirectives,
            retractedVersions
        );

        // Assert
        assertEquals(moduleName, content.getModuleName());
        assertEquals(goVersion, content.getGoVersion());
        assertEquals(toolchainVersion, content.getToolchainVersion());
        assertEquals(directDependencies, content.getDirectDependencies());
        assertEquals(indirectDependencies, content.getIndirectDependencies());
        assertEquals(excludedModules, content.getExcludedModules());
        assertEquals(replaceDirectives, content.getReplaceDirectives());
        assertTrue(content.getRetractedVersions().contains(retractedVersion));
    }
}