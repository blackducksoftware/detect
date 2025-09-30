package com.blackduck.integration.detectable.detectables.go.gomodfile.parse;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.go.gomodfile.GoModFileDetectableOptions;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModFileContent;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModuleInfo;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoReplaceDirective;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class demonstrating the functionality of the enhanced Go mod parser.
 */
@Tag("integration")
public class GoModParserTest {
    
    private GoModParser parser;
    private ExternalIdFactory externalIdFactory;
    private GoModFileDetectableOptions options = new GoModFileDetectableOptions("https://proxy.golang.org", 30, 30);
    
    @BeforeEach
    void setUp() {
        externalIdFactory = new ExternalIdFactory();
        parser = new GoModParser(externalIdFactory, options);
    }
    
    @Test
    void testParseCompleteGoModFile() {
        List<String> goModLines = Arrays.asList(
            "module gitlab.tools.duckutil.net/sat/test-go-directives",
            "",
            "go 1.24.0",
            "",
            "toolchain go1.25.0",
            "",
            "require (",
            "\tgithub.com/fsnotify/fsnotify v1.8.0",
            "\tgithub.com/gin-gonic/gin v1.10.0",
            ")",
            "",
            "require (",
            "\tgithub.com/bytedance/sonic v1.11.6 // indirect",
            "\tgithub.com/gin-contrib/sse v0.1.0 // indirect",
            "\tgoogle.golang.org/protobuf v1.36.8 // indirect",
            "\tgopkg.in/yaml.v3 v3.0.1 // indirect",
            ")",
            "",
            "exclude (",
            "\tgoogle.golang.org/protobuf v1.34.1",
            "\tgopkg.in/yaml.v3 v3.0.0-20200313102051-9f266ea9e77c",
            ")",
            "",
            "replace (",
            "\tgithub.com/fsnotify/fsnotify v1.8.0 => github.com/fsnotify/fsnotify v1.9.0",
            "\tgithub.com/gin-gonic/gin v1.10.0 => github.com/gin-gonic/gin v1.10.1",
            ")"
        );
        
        // Test detailed parsing
        GoModFileContent content = parser.getDetailedParseResult(goModLines);
        
        // Verify module information
        assertEquals("gitlab.tools.duckutil.net/sat/test-go-directives", content.getModuleName());
        assertEquals("1.24.0", content.getGoVersion());
        assertEquals("go1.25.0", content.getToolchainVersion());
        
        // Verify direct dependencies
        assertEquals(2, content.getDirectDependencies().size());
        assertTrue(content.getDirectDependencies().stream()
            .anyMatch(dep -> dep.getName().equals("github.com/fsnotify/fsnotify")));
        assertTrue(content.getDirectDependencies().stream()
            .anyMatch(dep -> dep.getName().equals("github.com/gin-gonic/gin")));
        
        // Verify indirect dependencies
        assertEquals(4, content.getIndirectDependencies().size());
        assertTrue(content.getIndirectDependencies().stream()
            .allMatch(GoModuleInfo::isIndirect));
        
        // Verify exclude directives
        assertEquals(2, content.getExcludedModules().size());
        
        // Verify replace directives
        assertEquals(2, content.getReplaceDirectives().size());
        
        // Test dependency graph creation
        DependencyGraph graph = parser.parseGoModFile(goModLines);
        assertNotNull(graph);
        assertFalse(graph.getDirectDependencies().isEmpty());
    }
    
    @Test
    void testReplaceDirectiveApplication() {
        List<String> goModLines = Arrays.asList(
            "module test-module",
            "go 1.21",
            "",
            "require (",
            "\tgithub.com/old/module v1.0.0",
            ")",
            "",
            "replace github.com/old/module v1.0.0 => github.com/new/module v2.0.0"
        );
        
        GoModFileContent content = parser.getDetailedParseResult(goModLines);
        
        // Verify original dependency exists
        assertEquals(1, content.getDirectDependencies().size());
        assertEquals("github.com/old/module", content.getDirectDependencies().get(0).getName());
        
        // Verify replace directive exists
        assertEquals(1, content.getReplaceDirectives().size());
        GoReplaceDirective replace = content.getReplaceDirectives().get(0);
        assertEquals("github.com/old/module", replace.getOldModule().getName());
        assertEquals("github.com/new/module", replace.getNewModule().getName());
        
        // Test that dependency graph reflects the replacement
        DependencyGraph graph = parser.parseGoModFile(goModLines);
        // The graph should contain the replaced dependency
        assertTrue(graph.getDirectDependencies().stream()
            .anyMatch(dep -> dep.getName().equals("github.com/new/module")));
    }
    
    @Test
    void testExcludeDirectiveApplication() {
        List<String> goModLines = Arrays.asList(
            "module test-module",
            "go 1.21",
            "",
            "require (",
            "\tgithub.com/good/module v1.0.0",
            "\tgithub.com/bad/module v1.0.0",
            ")",
            "",
            "exclude github.com/bad/module v1.0.0"
        );
        
        GoModFileContent content = parser.getDetailedParseResult(goModLines);
        
        // Verify both dependencies are parsed
        assertEquals(2, content.getDirectDependencies().size());
        
        // Verify exclude directive exists
        assertEquals(1, content.getExcludedModules().size());
        
        // Test that dependency graph excludes the bad module
        DependencyGraph graph = parser.parseGoModFile(goModLines);
        assertTrue(graph.getDirectDependencies().stream()
            .anyMatch(dep -> dep.getName().equals("github.com/good/module")));
        assertFalse(graph.getDirectDependencies().stream()
            .anyMatch(dep -> dep.getName().equals("github.com/bad/module")));
    }
    
    @Test
    void testVersionCleaning() {
        List<String> goModLines = Arrays.asList(
            "module test-module",
            "go 1.21",
            "",
            "require (",
            "\tgithub.com/test/module v1.0.0+incompatible",
            "\tgithub.com/test/module2 v0.0.0-20180917221912-90fa682c2a6e",
            ")"
        );
        
        DependencyGraph graph = parser.parseGoModFile(goModLines);
        
        // Verify that versions are properly cleaned
        assertNotNull(graph);
        assertFalse(graph.getDirectDependencies().isEmpty());
        
        // Check that incompatible suffix is removed
        assertTrue(graph.getDirectDependencies().stream()
            .anyMatch(dep -> dep.getName().equals("github.com/test/module") && 
                            !dep.getVersion().contains("+incompatible")));
    }
    
    @Test
    void testSingleLineDirectives() {
        List<String> goModLines = Arrays.asList(
            "module test-module",
            "go 1.21",
            "",
            "require github.com/single/module v1.0.0",
            "exclude github.com/bad/module v1.0.0",
            "replace github.com/old/module => github.com/new/module v2.0.0"
        );
        
        GoModFileContent content = parser.getDetailedParseResult(goModLines);
        
        assertEquals(1, content.getDirectDependencies().size());
        assertEquals(1, content.getExcludedModules().size());
        assertEquals(1, content.getReplaceDirectives().size());
    }
    
    @Test
    void testEmptyGoModFile() {
        List<String> goModLines = Arrays.asList(
            "module test-module",
            "go 1.21"
        );
        
        GoModFileContent content = parser.getDetailedParseResult(goModLines);
        
        assertEquals("test-module", content.getModuleName());
        assertEquals("1.21", content.getGoVersion());
        assertTrue(content.getDirectDependencies().isEmpty());
        assertTrue(content.getIndirectDependencies().isEmpty());
        assertTrue(content.getExcludedModules().isEmpty());
        assertTrue(content.getReplaceDirectives().isEmpty());
    }
}
