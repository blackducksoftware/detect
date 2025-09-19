package com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.detectable.detectables.go.gomodfile.GoModFileDetectableOptions;

class GoProxyModuleResolverTest {

    private GoModFileDetectableOptions options;
    private GoProxyModuleResolver resolver;

    @BeforeEach
    void setUp() {
        options = new GoModFileDetectableOptions("https://proxy.golang.org");
        resolver = new GoProxyModuleResolver(options);
    }

    @Test
    void checkConnectivity_success() throws Exception {
        Boolean result = resolver.checkConnectivity();
        assertTrue(result);
    }

    @Test
    void getGoModFileOfTheDependency_success() throws Exception {
        Dependency dependency = new Dependency("github.com/fsnotify/fsnotify", "v1.8.0", null, null);
        
        String result = resolver.getGoModFileOfTheDependency(dependency);
        
        assertTrue(result != null && !result.isEmpty());
        assertTrue(result.contains("module github.com/fsnotify/fsnotify"));
    }

    @Test
    void getGoModFileOfTheDependency_withQuotedModulePath() throws Exception {
        Dependency dependency = new Dependency("\"github.com/fsnotify/fsnotify\"", "v1.8.0", null, null);
        
        String result = resolver.getGoModFileOfTheDependency(dependency);
        
        assertTrue(result != null && !result.isEmpty());
        assertTrue(result.contains("module github.com/fsnotify/fsnotify"));
    }

    @Test
    void getGoModFileOfTheDependency_nonExistentModule() throws Exception {
        Dependency dependency = new Dependency("github.com/nonexistent/module", "v1.0.0", null, null);
        
        String result = resolver.getGoModFileOfTheDependency(dependency);

        assertTrue(result == null || result.isEmpty());
    }
}
