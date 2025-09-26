package com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoModuleInfoTest {

    @Test
    void testConstructorWithNameVersionAndIndirect() {
        GoModuleInfo moduleInfo = new GoModuleInfo("example.com/module", "v1.2.3", true);
        
        assertEquals("example.com/module", moduleInfo.getName());
        assertEquals("v1.2.3", moduleInfo.getVersion());
        assertTrue(moduleInfo.isIndirect());
    }

    @Test
    void testConstructorWithNameAndVersion() {
        GoModuleInfo moduleInfo = new GoModuleInfo("example.com/module", "v1.2.3");
        
        assertEquals("example.com/module", moduleInfo.getName());
        assertEquals("v1.2.3", moduleInfo.getVersion());
        assertFalse(moduleInfo.isIndirect());
    }

    @Test
    void testEqualsAndHashCode() {
        GoModuleInfo module1 = new GoModuleInfo("example.com/module", "v1.2.3", true);
        GoModuleInfo module2 = new GoModuleInfo("example.com/module", "v1.2.3", false);
        GoModuleInfo module3 = new GoModuleInfo("example.com/other", "v1.2.3", true);

        assertFalse(module1.equals(module2));
        assertFalse(module1.equals(module3));
    }
}