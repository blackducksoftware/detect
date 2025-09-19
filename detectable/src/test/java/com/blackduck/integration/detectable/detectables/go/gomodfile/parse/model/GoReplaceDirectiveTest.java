package com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GoReplaceDirectiveTest {

    @Test
    void getOldModule_shouldReturnOldModule() {
        // Given
        GoModuleInfo oldModule = new GoModuleInfo("old-module", "v1.0.0");
        GoModuleInfo newModule = new GoModuleInfo("new-module", "v2.0.0");
        GoReplaceDirective directive = new GoReplaceDirective(oldModule, newModule);

        // When
        GoModuleInfo result = directive.getOldModule();

        // Then
        assertEquals(oldModule, result);
    }

    @Test
    void getOldModule_shouldReturnNullWhenOldModuleIsNull() {
        // Given
        GoModuleInfo newModule = new GoModuleInfo("new-module", "v2.0.0");
        GoReplaceDirective directive = new GoReplaceDirective(null, newModule);

        // When
        GoModuleInfo result = directive.getOldModule();

        // Then
        assertNull(result);
    }
}
