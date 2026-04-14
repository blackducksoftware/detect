package com.blackduck.integration.detectable.detectables.go.gomodfile.parse;

import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModFileContent;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModuleInfo;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoReplaceDirective;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GoModFileParserTest {
    
    private GoModFileParser parser;
    
    @BeforeEach
    public void setUp() {
        parser = new GoModFileParser();
    }
    
    @Test
    public void testParseGoModFile_EmptyContent() {
        String content = "";
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertNotNull(result, "Result should not be null");
        assertNull(result.getModuleName(), "Module name should be null");
        assertNull(result.getGoVersion(), "Go version should be null");
        assertNull(result.getToolchainVersion(), "Toolchain version should be null");
        assertTrue(result.getDirectDependencies().isEmpty(), "Direct dependencies should be empty");
        assertTrue(result.getIndirectDependencies().isEmpty(), "Indirect dependencies should be empty");
        assertTrue(result.getExcludedModules().isEmpty(), "Excluded modules should be empty");
        assertTrue(result.getReplaceDirectives().isEmpty(), "Replace directives should be empty");
        assertTrue(result.getRetractedVersions().isEmpty(), "Retracted versions should be empty");
    }
    
    @Test
    public void testParseGoModFile_OnlyComments() {
        String content = "// This is a comment\n" +
                        "// Another comment\n" +
                        "\n" +
                        "// More comments\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertNotNull(result, "Result should not be null");
        assertNull(result.getModuleName(), "Module name should be null");
        assertTrue(result.getDirectDependencies().isEmpty(), "Dependencies should be empty");
    }
    
    @Test
    public void testParseGoModFile_BasicModule() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "go 1.18\n" +
                        "\n" +
                        "toolchain go1.18.1\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertNotNull(result, "Result should not be null");
        assertEquals("github.com/example/myapp", result.getModuleName(), "Module name should match");
        assertEquals("1.18", result.getGoVersion(), "Go version should match");
        assertEquals("go1.18.1", result.getToolchainVersion(), "Toolchain version should match");
        assertTrue(result.getDirectDependencies().isEmpty(), "Dependencies should be empty");
    }
    
    @Test
    public void testParseGoModFile_SingleRequire() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "go 1.18\n" +
                        "\n" +
                        "require github.com/gin-gonic/gin v1.8.1\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertNotNull(result, "Result should not be null");
        assertEquals("github.com/example/myapp", result.getModuleName(), "Module name should match");
        assertEquals(1, result.getDirectDependencies().size(), "Should have one direct dependency");
        assertTrue(result.getIndirectDependencies().isEmpty(), "Should have no indirect dependencies");
        
        GoModuleInfo dependency = result.getDirectDependencies().get(0);
        assertEquals("github.com/gin-gonic/gin", dependency.getName(), "Dependency name should match");
        assertEquals("v1.8.1", dependency.getVersion(), "Dependency version should match");
        assertFalse(dependency.isIndirect(), "Dependency should be direct");
    }
    
    @Test
    public void testParseGoModFile_RequireBlock() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "go 1.18\n" +
                        "\n" +
                        "require (\n" +
                        "    github.com/gin-gonic/gin v1.8.1\n" +
                        "    github.com/gorilla/mux v1.8.0\n" +
                        "    github.com/lib/pq v1.10.6 // indirect\n" +
                        "    golang.org/x/crypto v0.0.0-20220622213112-05595931fe9d // indirect\n" +
                        ")\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.getDirectDependencies().size(), "Should have two direct dependencies");
        assertEquals(2, result.getIndirectDependencies().size(), "Should have two indirect dependencies");
        
        // Check direct dependencies
        assertTrue(result.getDirectDependencies().stream()
            .anyMatch(dep -> "github.com/gin-gonic/gin".equals(dep.getName()) && "v1.8.1".equals(dep.getVersion())), 
            "Should contain gin dependency");
        assertTrue(result.getDirectDependencies().stream()
            .anyMatch(dep -> "github.com/gorilla/mux".equals(dep.getName()) && "v1.8.0".equals(dep.getVersion())), 
            "Should contain mux dependency");
        
        // Check indirect dependencies
        assertTrue(result.getIndirectDependencies().stream()
            .anyMatch(dep -> "github.com/lib/pq".equals(dep.getName()) && dep.isIndirect()), 
            "Should contain indirect pq dependency");
        assertTrue(result.getIndirectDependencies().stream()
            .anyMatch(dep -> "golang.org/x/crypto".equals(dep.getName()) && dep.isIndirect()), 
            "Should contain indirect crypto dependency");
    }
    
    @Test
    public void testParseGoModFile_WithIncompatibleVersions() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "require (\n" +
                        "    github.com/old/module v1.0.0+incompatible\n" +
                        "    github.com/another/module v2.0.0%2Bincompatible\n" +
                        ")\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertEquals(2, result.getDirectDependencies().size(), "Should have two dependencies");
        
        GoModuleInfo firstDep = result.getDirectDependencies().stream()
            .filter(dep -> "github.com/old/module".equals(dep.getName()))
            .findFirst()
            .orElse(null);
        assertNotNull(firstDep, "Should find first dependency");
        assertEquals("v1.0.0", firstDep.getVersion(), "Should clean +incompatible suffix");
        
        GoModuleInfo secondDep = result.getDirectDependencies().stream()
            .filter(dep -> "github.com/another/module".equals(dep.getName()))
            .findFirst()
            .orElse(null);
        assertNotNull(secondDep, "Should find second dependency");
        assertEquals("v2.0.0", secondDep.getVersion(), "Should clean %2Bincompatible suffix");
    }
    
    @Test
    public void testParseGoModFile_ExcludeDirective() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "exclude github.com/bad/module v1.0.0\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertEquals(1, result.getExcludedModules().size(), "Should have one excluded module");
        
        GoModuleInfo excluded = result.getExcludedModules().iterator().next();
        assertEquals("github.com/bad/module", excluded.getName(), "Excluded module name should match");
        assertEquals("v1.0.0", excluded.getVersion(), "Excluded module version should match");
    }
    
    @Test
    public void testParseGoModFile_ExcludeBlock() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "exclude (\n" +
                        "    github.com/bad/module v1.0.0\n" +
                        "    github.com/another/bad v2.0.0\n" +
                        ")\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertEquals(2, result.getExcludedModules().size(), "Should have two excluded modules");
        assertTrue(result.getExcludedModules().stream()
            .anyMatch(mod -> "github.com/bad/module".equals(mod.getName())), 
            "Should contain first excluded module");
        assertTrue(result.getExcludedModules().stream()
            .anyMatch(mod -> "github.com/another/bad".equals(mod.getName())), 
            "Should contain second excluded module");
    }
    
    @Test
    public void testParseGoModFile_ReplaceDirective() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "replace github.com/old/module v1.0.0 => github.com/new/module v1.1.0\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertEquals(1, result.getReplaceDirectives().size(), "Should have one replace directive");
        
        GoReplaceDirective replace = result.getReplaceDirectives().get(0);
        assertEquals("github.com/old/module", replace.getOldModule().getName(), "Old module name should match");
        assertEquals("v1.0.0", replace.getOldModule().getVersion(), "Old module version should match");
        assertEquals("github.com/new/module", replace.getNewModule().getName(), "New module name should match");
        assertEquals("v1.1.0", replace.getNewModule().getVersion(), "New module version should match");
    }
    
    @Test
    public void testParseGoModFile_ReplaceDirectiveWithoutVersion() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "replace github.com/old/module => github.com/new/module v1.1.0\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertEquals(1, result.getReplaceDirectives().size(), "Should have one replace directive");
        
        GoReplaceDirective replace = result.getReplaceDirectives().get(0);
        assertEquals("github.com/old/module", replace.getOldModule().getName(), "Old module name should match");
        assertEquals("github.com/new/module", replace.getNewModule().getName(), "New module name should match");
        assertEquals("v1.1.0", replace.getNewModule().getVersion(), "New module version should match");
    }
    
    @Test
    public void testParseGoModFile_ReplaceBlock() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "replace (\n" +
                        "    github.com/old/module v1.0.0 => github.com/new/module v1.1.0\n" +
                        "    github.com/another/old => github.com/another/new v2.0.0\n" +
                        ")\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertEquals(2, result.getReplaceDirectives().size(), "Should have two replace directives");
        
        assertTrue(result.getReplaceDirectives().stream()
            .anyMatch(replace -> "github.com/old/module".equals(replace.getOldModule().getName())), 
            "Should contain first replace directive");
        assertTrue(result.getReplaceDirectives().stream()
            .anyMatch(replace -> "github.com/another/old".equals(replace.getOldModule().getName())), 
            "Should contain second replace directive");
    }
    
    @Test
    public void testParseGoModFile_RetractDirective() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "retract github.com/old/module v1.0.0\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertEquals(1, result.getRetractedVersions().size(), "Should have one retracted version");
        
        GoModuleInfo retracted = result.getRetractedVersions().iterator().next();
        assertEquals("v1.0.0", retracted.getVersion(), "Retracted version should match");
    }
    
    @Test
    public void testParseGoModFile_RetractBlock() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "retract (\n" +
                        "    github.com/old/module v1.0.0\n" +
                        "    github.com/new/module v1.0.1\n" +
                        ")\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertEquals(2, result.getRetractedVersions().size(), "Should have two retracted versions");
        assertTrue(result.getRetractedVersions().stream()
            .anyMatch(retracted -> "v1.0.0".equals(retracted.getVersion())), 
            "Should contain v1.0.0");
        assertTrue(result.getRetractedVersions().stream()
            .anyMatch(retracted -> "v1.0.1".equals(retracted.getVersion())), 
            "Should contain v1.0.1");
    }
    
    @Test
    public void testParseGoModFile_RetractVersionRange() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "retract [v1.0.0, v1.0.5]\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertEquals(2, result.getRetractedVersions().size(), "Should have two retracted versions from range");
        assertTrue(result.getRetractedVersions().stream()
            .anyMatch(retracted -> "v1.0.0".equals(retracted.getVersion())), 
            "Should contain v1.0.0");
        assertTrue(result.getRetractedVersions().stream()
            .anyMatch(retracted -> "v1.0.5".equals(retracted.getVersion())), 
            "Should contain v1.0.5");
    }
    
    @Test
    public void testParseGoModFile_CompleteExample() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "go 1.18\n" +
                        "\n" +
                        "toolchain go1.18.1\n" +
                        "\n" +
                        "require (\n" +
                        "    github.com/gin-gonic/gin v1.8.1\n" +
                        "    github.com/gorilla/mux v1.8.0\n" +
                        "    github.com/lib/pq v1.10.6 // indirect\n" +
                        ")\n" +
                        "\n" +
                        "exclude (\n" +
                        "    github.com/bad/module v1.0.0\n" +
                        "    github.com/another/bad v2.0.0\n" +
                        ")\n" +
                        "\n" +
                        "replace (\n" +
                        "    github.com/old/module v1.0.0 => github.com/new/module v1.1.0\n" +
                        "    github.com/fork => ../local/fork\n" +
                        ")\n" +
                        "\n" +
                        "retract (\n" +
                        "    v1.0.0\n" +
                        "    [v1.1.0, v1.1.5]\n" +
                        ")\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        // Verify all components are parsed
        assertEquals("github.com/example/myapp", result.getModuleName(), "Module name should match");
        assertEquals("1.18", result.getGoVersion(), "Go version should match");
        assertEquals("go1.18.1", result.getToolchainVersion(), "Toolchain should match");
        
        assertEquals(2, result.getDirectDependencies().size(), "Should have two direct dependencies");
        assertEquals(1, result.getIndirectDependencies().size(), "Should have one indirect dependency");
        assertEquals(2, result.getExcludedModules().size(), "Should have two excluded modules");
        assertEquals(2, result.getReplaceDirectives().size(), "Should have two replace directives");
        assertEquals(3, result.getRetractedVersions().size(), "Should have three retracted versions");
    }
    
    @Test
    public void testParseGoModFile_MalformedLines() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "require\n" +
                        "invalidline\n" +
                        "require github.com/valid/module v1.0.0\n" +
                        "replace => invalid\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertEquals("github.com/example/myapp", result.getModuleName(), "Module name should be parsed");
        assertEquals(1, result.getDirectDependencies().size(), "Should parse only valid dependency");
        assertEquals("github.com/valid/module", result.getDirectDependencies().get(0).getName(), 
            "Valid dependency should be parsed");
        assertTrue(result.getReplaceDirectives().isEmpty(), "Invalid replace should be ignored");
    }
    
    @Test
    public void testParseGoModFile_WithListInterface() {
        List<String> lines = Arrays.asList(
            "module github.com/example/myapp",
            "",
            "go 1.18",
            "",
            "require github.com/gin-gonic/gin v1.8.1"
        );
        
        GoModFileContent result = parser.parseGoModFile(lines);
        
        assertEquals("github.com/example/myapp", result.getModuleName(), "Module name should match");
        assertEquals("1.18", result.getGoVersion(), "Go version should match");
        assertEquals(1, result.getDirectDependencies().size(), "Should have one dependency");
    }
    
    @Test
    public void testParseGoModFile_DependencyWithoutVersion() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "require (\n" +
                        "    github.com/module-without-version\n" +
                        "    github.com/module-with-version v1.0.0\n" +
                        ")\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertEquals(2, result.getDirectDependencies().size(), "Should parse both dependencies");
        
        GoModuleInfo withoutVersion = result.getDirectDependencies().stream()
            .filter(dep -> "github.com/module-without-version".equals(dep.getName()))
            .findFirst()
            .orElse(null);
        assertNotNull(withoutVersion, "Should find dependency without version");
        assertEquals("", withoutVersion.getVersion(), "Version should be empty string");
        
        GoModuleInfo withVersion = result.getDirectDependencies().stream()
            .filter(dep -> "github.com/module-with-version".equals(dep.getName()))
            .findFirst()
            .orElse(null);
        assertNotNull(withVersion, "Should find dependency with version");
        assertEquals("v1.0.0", withVersion.getVersion(), "Version should match");
    }
    
    @Test
    public void testParseGoModFile_MixedIndentation() {
        String content = "module github.com/example/myapp\n" +
                        "\n" +
                        "require (\n" +
                        "\tgithub.com/tab-indented v1.0.0\n" +
                        "    github.com/space-indented v2.0.0\n" +
                        "\t    github.com/mixed-indented v3.0.0 // indirect\n" +
                        ")\n";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertEquals(2, result.getDirectDependencies().size(), "Should have two direct dependencies");
        assertEquals(1, result.getIndirectDependencies().size(), "Should have one indirect dependency");
        
        assertTrue(result.getDirectDependencies().stream()
            .anyMatch(dep -> "github.com/tab-indented".equals(dep.getName())), 
            "Should parse tab-indented dependency");
        assertTrue(result.getDirectDependencies().stream()
            .anyMatch(dep -> "github.com/space-indented".equals(dep.getName())), 
            "Should parse space-indented dependency");
        assertTrue(result.getIndirectDependencies().stream()
            .anyMatch(dep -> "github.com/mixed-indented".equals(dep.getName())), 
            "Should parse mixed-indented indirect dependency");
    }
    
    @Test
    public void testParseGoModFile_WindowsLineEndings() {
        String content = "module github.com/example/myapp\r\n\r\ngo 1.18\r\n\r\nrequire github.com/gin-gonic/gin v1.8.1";
        
        GoModFileContent result = parser.parseGoModFile(content);
        
        assertEquals("github.com/example/myapp", result.getModuleName(), "Module name should be parsed");
        assertEquals("1.18", result.getGoVersion(), "Go version should be parsed");
        assertEquals(1, result.getDirectDependencies().size(), "Should have one dependency");
    }
}