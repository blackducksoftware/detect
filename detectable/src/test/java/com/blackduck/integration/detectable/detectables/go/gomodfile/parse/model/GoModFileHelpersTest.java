package com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;

class GoModFileHelpersTest {

    private ExternalIdFactory externalIdFactory;
    private GoModFileHelpers goModFileHelpers;

    @BeforeEach
    void setUp() {
        externalIdFactory = new ExternalIdFactory();
        goModFileHelpers = new GoModFileHelpers(externalIdFactory);
    }

    @Test
    void testCreateDependencyWithValidModuleInfo() {
        GoModuleInfo moduleInfo = new GoModuleInfo("github.com/example/module", "v1.2.3");

        Dependency result = goModFileHelpers.createDependency(moduleInfo);

        assertEquals("github.com/example/module", result.getName());
        assertEquals("v1.2.3", result.getVersion());
        assertTrue(result.getExternalId() != null);
        assertNull(result.getScope());
    }

    @Test
    void testCreateDependencyWithIncompatibleVersion() {
        GoModuleInfo moduleInfo = new GoModuleInfo("github.com/example/module", "v1.2.3+incompatible");

        Dependency result = goModFileHelpers.createDependency(moduleInfo);

        assertEquals("github.com/example/module", result.getName());
        assertEquals("v1.2.3", result.getVersion());
        assertTrue(result.getExternalId() != null);
    }

    @Test
    void testCreateDependencyWithEncodedIncompatibleVersion() {
        GoModuleInfo moduleInfo = new GoModuleInfo("github.com/example/module", "v1.2.3%2Bincompatible");

        Dependency result = goModFileHelpers.createDependency(moduleInfo);

        assertEquals("github.com/example/module", result.getName());
        assertEquals("v1.2.3", result.getVersion());
        assertTrue(result.getExternalId() != null);
    }

    @Test
    void testCreateDependencyWithNullVersion() {
        GoModuleInfo moduleInfo = new GoModuleInfo("github.com/example/module", null);

        Dependency result = goModFileHelpers.createDependency(moduleInfo);

        assertEquals("github.com/example/module", result.getName());
        assertNull(result.getVersion());
        assertTrue(result.getExternalId() != null);
    }

    @Test
    void testCreateDependencyWithEmptyVersion() {
        GoModuleInfo moduleInfo = new GoModuleInfo("github.com/example/module", "");

        Dependency result = goModFileHelpers.createDependency(moduleInfo);

        assertEquals("github.com/example/module", result.getName());
        assertNull(result.getVersion());
        assertTrue(result.getExternalId() != null);
    }
}
