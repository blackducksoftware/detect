package com.blackduck.integration.detectable.detectables.go.gomodfile.parse;

import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.go.gomodfile.GoModFileDetectableOptions;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoDependencyNode;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModFileContent;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModuleInfo;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoReplaceDirective;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class GoModDependencyResolverTest {
    
    private GoModDependencyResolver resolver;
    private ExternalIdFactory externalIdFactory;
    private GoModFileDetectableOptions options;
    
    @BeforeEach
    public void setUp() {
        options = new GoModFileDetectableOptions("https://proxy.golang.org", 30, 30);
        resolver = new GoModDependencyResolver(options);
        externalIdFactory = new ExternalIdFactory();
    }
    
    @Test
    public void testResolveDependencies_EmptyContent() {
        // Create empty go.mod content
        GoModFileContent content = new GoModFileContent(
            "test/module",
            "1.18",
            "go1.18.1",
            new ArrayList<>(),
            new ArrayList<>(),
            new HashSet<>(),
            new ArrayList<>(),
            new HashSet<>()
        );
        
        GoModDependencyResolver.ResolvedDependencies result = resolver.resolveDependencies(content, externalIdFactory);
        
        assertTrue(result.getDirectDependencies().isEmpty(), "Direct dependencies should be empty");
        assertTrue(result.getIndirectDependencies().isEmpty(), "Indirect dependencies should be empty");
        assertTrue(result.getAllDependencies().isEmpty(), "All dependencies should be empty");
        assertNotNull(result.getDependencyGraph(), "Dependency graph should not be null");
        assertTrue(result.getDependencyGraph().isRootNode(), "Root node should be marked as root");
    }
    
    @Test
    public void testResolveDependencies_DirectAndIndirectDependencies() {
        // Create test dependencies
        List<GoModuleInfo> directDeps = Arrays.asList(
            new GoModuleInfo("github.com/direct1", "v1.0.0", false),
            new GoModuleInfo("github.com/direct2", "v2.1.0", false)
        );
        
        List<GoModuleInfo> indirectDeps = Arrays.asList(
            new GoModuleInfo("github.com/indirect1", "v1.2.0", true),
            new GoModuleInfo("github.com/indirect2", "v0.5.0", true)
        );
        
        GoModFileContent content = new GoModFileContent(
            "test/module",
            "1.18",
            "go1.18.1",
            directDeps,
            indirectDeps,
            new HashSet<>(),
            new ArrayList<>(),
            new HashSet<>()
        );
        
        GoModDependencyResolver.ResolvedDependencies result = resolver.resolveDependencies(content, externalIdFactory);
        
        assertEquals(2, result.getDirectDependencies().size(), "Should have 2 direct dependencies");
        assertEquals(2, result.getIndirectDependencies().size(), "Should have 2 indirect dependencies");
        assertEquals(4, result.getAllDependencies().size(), "Should have 4 total dependencies");
        
        // Verify direct dependencies
        assertTrue(result.getDirectDependencies().stream()
            .anyMatch(dep -> "github.com/direct1".equals(dep.getName())), 
            "Should contain direct1 dependency");
        assertTrue(result.getDirectDependencies().stream()
            .anyMatch(dep -> "github.com/direct2".equals(dep.getName())), 
            "Should contain direct2 dependency");
        
        // Verify indirect dependencies
        assertTrue(result.getIndirectDependencies().stream()
            .anyMatch(dep -> "github.com/indirect1".equals(dep.getName())), 
            "Should contain indirect1 dependency");
        assertTrue(result.getIndirectDependencies().stream()
            .anyMatch(dep -> "github.com/indirect2".equals(dep.getName())), 
            "Should contain indirect2 dependency");
    }
    
    @Test
    public void testResolveDependencies_WithReplaceDirectives() {
        // Create test dependencies
        List<GoModuleInfo> directDeps = Arrays.asList(
            new GoModuleInfo("github.com/old-module", "v1.0.0", false),
            new GoModuleInfo("github.com/unchanged", "v2.0.0", false)
        );
        
        // Create replace directive
        GoReplaceDirective replaceDirective = new GoReplaceDirective(
            new GoModuleInfo("github.com/old-module", "v1.0.0"),
            new GoModuleInfo("github.com/new-module", "v1.5.0")
        );
        
        GoModFileContent content = new GoModFileContent(
            "test/module",
            "1.18",
            "go1.18.1",
            directDeps,
            new ArrayList<>(),
            new HashSet<>(),
            Arrays.asList(replaceDirective),
            new HashSet<>()
        );
        
        GoModDependencyResolver.ResolvedDependencies result = resolver.resolveDependencies(content, externalIdFactory);
        
        assertEquals(2, result.getDirectDependencies().size(), "Should have 2 direct dependencies");
        
        // Verify replacement occurred
        assertTrue(result.getDirectDependencies().stream()
            .anyMatch(dep -> "github.com/new-module".equals(dep.getName()) && "v1.5.0".equals(dep.getVersion())), 
            "Should contain replaced module");
        assertTrue(result.getDirectDependencies().stream()
            .anyMatch(dep -> "github.com/unchanged".equals(dep.getName())), 
            "Should contain unchanged module");
        assertTrue(result.getDirectDependencies().stream()
            .noneMatch(dep -> "github.com/old-module".equals(dep.getName())), 
            "Should not contain old module");
    }
    
    @Test
    public void testResolveDependencies_WithModuleOnlyReplacement() {
        // Create test dependencies
        List<GoModuleInfo> directDeps = Arrays.asList(
            new GoModuleInfo("github.com/old-module", "v1.0.0", false),
            new GoModuleInfo("github.com/old-module", "v2.0.0", false)
        );
        
        // Create module-only replace directive (no specific version in old module)
        GoReplaceDirective replaceDirective = new GoReplaceDirective(
            new GoModuleInfo("github.com/old-module", ""),
            new GoModuleInfo("github.com/new-module", "v3.0.0")
        );
        
        GoModFileContent content = new GoModFileContent(
            "test/module",
            "1.18",
            "go1.18.1",
            directDeps,
            new ArrayList<>(),
            new HashSet<>(),
            Arrays.asList(replaceDirective),
            new HashSet<>()
        );
        
        GoModDependencyResolver.ResolvedDependencies result = resolver.resolveDependencies(content, externalIdFactory);
        
        assertEquals(2, result.getDirectDependencies().size(), "Should have 2 direct dependencies");
        
        // Both instances should be replaced
        assertTrue(result.getDirectDependencies().stream()
            .allMatch(dep -> "github.com/new-module".equals(dep.getName()) && "v3.0.0".equals(dep.getVersion())), 
            "All instances should be replaced with new module");
    }
    
    @Test
    public void testResolveDependencies_WithExcludeDirectives() {
        // Create test dependencies
        List<GoModuleInfo> directDeps = Arrays.asList(
            new GoModuleInfo("github.com/keep", "v1.0.0", false),
            new GoModuleInfo("github.com/exclude-exact", "v1.0.0", false),
            new GoModuleInfo("github.com/exclude-module", "v2.0.0", false)
        );
        
        // Create exclude directives
        Set<GoModuleInfo> excludeDirectives = new HashSet<>(Arrays.asList(
            new GoModuleInfo("github.com/exclude-exact", "v1.0.0") // Exact version match
        ));
        
        GoModFileContent content = new GoModFileContent(
            "test/module",
            "1.18",
            "go1.18.1",
            directDeps,
            new ArrayList<>(),
            excludeDirectives,
            new ArrayList<>(),
            new HashSet<>()
        );
        
        GoModDependencyResolver.ResolvedDependencies result = resolver.resolveDependencies(content, externalIdFactory);

        assertEquals(2, result.getDirectDependencies().size(), "Should have 2 remaining dependencies");
        assertTrue(result.getDirectDependencies().stream()
            .anyMatch(dep -> "github.com/keep".equals(dep.getName())), 
            "Should keep non-excluded dependencies");
        assertTrue(result.getDirectDependencies().stream()
            .noneMatch(dep -> dep.getName().contains("exclude-exact")), 
            "Should exclude github.com/exclude-exact dependency");
    }
    
    @Test
    public void testResolveDependencies_WithRetractedVersions() {
        // Create test dependencies
        List<GoModuleInfo> directDeps = Arrays.asList(
            new GoModuleInfo("github.com/module1", "v1.0.0", false),
            new GoModuleInfo("github.com/module2", "v2.0.0", false),
            new GoModuleInfo("github.com/module3", "v1.5.0", false)
        );
        
        // Create retracted versions
        Set<GoModuleInfo> retractedVersions = new HashSet<>(Arrays.asList(
            new GoModuleInfo("github.com/module2", "v2.0.0") // This version should be filtered
        ));
        
        GoModFileContent content = new GoModFileContent(
            "test/module",
            "1.18",
            "go1.18.1",
            directDeps,
            new ArrayList<>(),
            new HashSet<>(),
            new ArrayList<>(),
            retractedVersions
        );
        
        GoModDependencyResolver.ResolvedDependencies result = resolver.resolveDependencies(content, externalIdFactory);
        
        assertEquals(2, result.getDirectDependencies().size(), "Should have 2 remaining dependencies");
        assertTrue(result.getDirectDependencies().stream()
            .anyMatch(dep -> "github.com/module1".equals(dep.getName())), 
            "Should keep module1");
        assertTrue(result.getDirectDependencies().stream()
            .anyMatch(dep -> "github.com/module3".equals(dep.getName())), 
            "Should keep module3");
        assertTrue(result.getDirectDependencies().stream()
            .noneMatch(dep -> "github.com/module2".equals(dep.getName())), 
            "Should filter retracted version");
    }
    
    @Test
    public void testResolveDependencies_ComplexScenario() {
        // Create test dependencies
        List<GoModuleInfo> directDeps = Arrays.asList(
            new GoModuleInfo("github.com/replace-me", "v1.0.0", false),
            new GoModuleInfo("github.com/exclude-me", "v2.0.0", false),
            new GoModuleInfo("github.com/retract-me", "v3.0.0", false),
            new GoModuleInfo("github.com/keep-me", "v4.0.0", false)
        );
        
        List<GoModuleInfo> indirectDeps = Arrays.asList(
            new GoModuleInfo("github.com/indirect-exclude", "v1.0.0", true)
        );
        
        // Create replace directive
        GoReplaceDirective replaceDirective = new GoReplaceDirective(
            new GoModuleInfo("github.com/replace-me", "v1.0.0"),
            new GoModuleInfo("github.com/replaced", "v1.1.0")
        );
        
        // Create exclude directive
        Set<GoModuleInfo> excludeDirectives = new HashSet<>(Arrays.asList(
            new GoModuleInfo("github.com/exclude-me", "v2.0.0"),
            new GoModuleInfo("github.com/indirect-exclude", "v1.0.0")
        ));
        
        // Create retracted versions
        Set<GoModuleInfo> retractedVersions = new HashSet<>(Arrays.asList(
            new GoModuleInfo("github.com/retract-me", "v3.0.0")
        ));
        
        GoModFileContent content = new GoModFileContent(
            "test/module",
            "1.18",
            "go1.18.1",
            directDeps,
            indirectDeps,
            excludeDirectives,
            Arrays.asList(replaceDirective),
            retractedVersions
        );
        
        GoModDependencyResolver.ResolvedDependencies result = resolver.resolveDependencies(content, externalIdFactory);
        
        // Should have replaced module and keep-me module
        assertEquals(2, result.getDirectDependencies().size(), "Should have 2 remaining direct dependencies");
        assertTrue(result.getDirectDependencies().stream()
            .anyMatch(dep -> "github.com/replaced".equals(dep.getName())), 
            "Should contain replaced module");
        assertTrue(result.getDirectDependencies().stream()
            .anyMatch(dep -> "github.com/keep-me".equals(dep.getName())), 
            "Should contain kept module");
        
        // Indirect dependencies should be empty due to exclusion
        assertTrue(result.getIndirectDependencies().isEmpty(), "Indirect dependencies should be empty");
        assertEquals(2, result.getAllDependencies().size(), "Should have 2 total dependencies");
    }
    
    @Test
    public void testResolveDependencies_PreservesIndirectFlags() {
        // Create test dependencies with mixed indirect flags
        List<GoModuleInfo> directDeps = Arrays.asList(
            new GoModuleInfo("github.com/direct1", "v1.0.0", false)
        );
        
        List<GoModuleInfo> indirectDeps = Arrays.asList(
            new GoModuleInfo("github.com/indirect1", "v1.0.0", true)
        );
        
        // Create replace directive that should preserve indirect flag
        GoReplaceDirective replaceDirective = new GoReplaceDirective(
            new GoModuleInfo("github.com/indirect1", "v1.0.0"),
            new GoModuleInfo("github.com/new-indirect", "v2.0.0")
        );
        
        GoModFileContent content = new GoModFileContent(
            "test/module",
            "1.18",
            "go1.18.1",
            directDeps,
            indirectDeps,
            new HashSet<>(),
            Arrays.asList(replaceDirective),
            new HashSet<>()
        );
        
        GoModDependencyResolver.ResolvedDependencies result = resolver.resolveDependencies(content, externalIdFactory);
        
        assertEquals(1, result.getDirectDependencies().size(), "Should have 1 direct dependency");
        assertEquals(1, result.getIndirectDependencies().size(), "Should have 1 indirect dependency");
        
        // Verify indirect flag is preserved after replacement
        GoModuleInfo replacedIndirect = result.getIndirectDependencies().get(0);
        assertEquals("github.com/new-indirect", replacedIndirect.getName(), "Should have replaced name");
        assertTrue(replacedIndirect.isIndirect(), "Should preserve indirect flag");
    }
    
    @Test
    public void testResolvedDependencies_Constructor() {
        List<GoModuleInfo> directDeps = Arrays.asList(
            new GoModuleInfo("github.com/direct", "v1.0.0", false)
        );
        List<GoModuleInfo> indirectDeps = Arrays.asList(
            new GoModuleInfo("github.com/indirect", "v1.0.0", true)
        );
        
        // Test constructor without dependency graph
        GoModDependencyResolver.ResolvedDependencies result1 = 
            new GoModDependencyResolver.ResolvedDependencies(directDeps, indirectDeps);
        
        assertEquals(directDeps, result1.getDirectDependencies(), "Direct dependencies should match");
        assertEquals(indirectDeps, result1.getIndirectDependencies(), "Indirect dependencies should match");
        assertNull(result1.getDependencyGraph(), "Dependency graph should be null");
        assertEquals(2, result1.getAllDependencies().size(), "Should have 2 total dependencies");
        
        // Test constructor with dependency graph
        GoDependencyNode mockGraph = new GoDependencyNode(true, null, new ArrayList<>());
        GoModDependencyResolver.ResolvedDependencies result2 = 
            new GoModDependencyResolver.ResolvedDependencies(directDeps, indirectDeps, mockGraph);
        
        assertEquals(directDeps, result2.getDirectDependencies(), "Direct dependencies should match");
        assertEquals(indirectDeps, result2.getIndirectDependencies(), "Indirect dependencies should match");
        assertEquals(mockGraph, result2.getDependencyGraph(), "Dependency graph should match");
        assertEquals(2, result2.getAllDependencies().size(), "Should have 2 total dependencies");
    }
    
    @Test
    public void testResolvedDependencies_GetAllDependencies() {
        List<GoModuleInfo> directDeps = Arrays.asList(
            new GoModuleInfo("github.com/direct1", "v1.0.0", false),
            new GoModuleInfo("github.com/direct2", "v2.0.0", false)
        );
        List<GoModuleInfo> indirectDeps = Arrays.asList(
            new GoModuleInfo("github.com/indirect1", "v1.0.0", true)
        );
        
        GoModDependencyResolver.ResolvedDependencies result = 
            new GoModDependencyResolver.ResolvedDependencies(directDeps, indirectDeps);
        
        List<GoModuleInfo> allDeps = result.getAllDependencies();
        assertEquals(3, allDeps.size(), "Should have 3 total dependencies");
        
        // Verify all dependencies are included
        assertTrue(allDeps.stream()
            .anyMatch(dep -> "github.com/direct1".equals(dep.getName())), 
            "Should contain direct1");
        assertTrue(allDeps.stream()
            .anyMatch(dep -> "github.com/direct2".equals(dep.getName())), 
            "Should contain direct2");
        assertTrue(allDeps.stream()
            .anyMatch(dep -> "github.com/indirect1".equals(dep.getName())), 
            "Should contain indirect1");
    }
    
    @Test
    public void testResolvedDependencies_ToString() {
        List<GoModuleInfo> directDeps = Arrays.asList(
            new GoModuleInfo("github.com/direct", "v1.0.0", false)
        );
        List<GoModuleInfo> indirectDeps = Arrays.asList(
            new GoModuleInfo("github.com/indirect", "v1.0.0", true)
        );
        
        // Test without dependency graph
        GoModDependencyResolver.ResolvedDependencies result1 = 
            new GoModDependencyResolver.ResolvedDependencies(directDeps, indirectDeps);
        
        String toString1 = result1.toString();
        assertTrue(toString1.contains("directDependencies=1"), "Should contain direct count");
        assertTrue(toString1.contains("indirectDependencies=1"), "Should contain indirect count");
        assertTrue(toString1.contains("hasRecursiveGraph=false"), "Should indicate no graph");
        
        // Test with dependency graph
        GoDependencyNode mockGraph = new GoDependencyNode(true, null, new ArrayList<>());
        GoModDependencyResolver.ResolvedDependencies result2 = 
            new GoModDependencyResolver.ResolvedDependencies(directDeps, indirectDeps, mockGraph);
        
        String toString2 = result2.toString();
        assertTrue(toString2.contains("directDependencies=1"), "Should contain direct count");
        assertTrue(toString2.contains("indirectDependencies=1"), "Should contain indirect count");
        assertTrue(toString2.contains("hasRecursiveGraph=true"), "Should indicate graph exists");
    }
    
    @Test
    public void testResolveDependencies_WithModuleName() {
        GoModFileContent content = new GoModFileContent(
            "github.com/test/module",
            "1.18",
            "go1.18.1",
            new ArrayList<>(),
            new ArrayList<>(),
            new HashSet<>(),
            new ArrayList<>(),
            new HashSet<>()
        );
        
        GoModDependencyResolver.ResolvedDependencies result = resolver.resolveDependencies(content, externalIdFactory);
        
        assertNotNull(result.getDependencyGraph(), "Dependency graph should not be null");
        assertTrue(result.getDependencyGraph().isRootNode(), "Root node should be marked as root");
        assertNotNull(result.getDependencyGraph().getDependency(), "Root should have parent dependency");
        assertEquals("github.com/test/module", result.getDependencyGraph().getDependency().getName(), 
            "Root dependency should have module name");
    }
}
