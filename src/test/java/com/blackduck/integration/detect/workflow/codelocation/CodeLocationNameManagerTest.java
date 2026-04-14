package com.blackduck.integration.detect.workflow.codelocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CodeLocationNameManagerTest {
    
    private CodeLocationNameGenerator codeLocationNameGenerator;
    private CodeLocationNameManager codeLocationNameManager;
    
    @BeforeEach
    public void setUp() {
        // Use real CodeLocationNameGenerator with override to test actual behavior
        codeLocationNameGenerator = CodeLocationNameGenerator.withOverride("overridden-name");
        codeLocationNameManager = new CodeLocationNameManager(codeLocationNameGenerator);
    }
    
    @Test
    public void testCreateScanCodeLocationNameWithOverrideCalledTwiceWithoutScassFallback() {
        // Setup
        File sourcePath = mock(File.class);
        File scanTargetPath = mock(File.class);
        File dockerTar = null;
        String projectName = "testProject";
        String projectVersionName = "1.0.0";
        boolean isScassFallback = false;
        
        // Execute - First call
        String result1 = codeLocationNameManager.createScanCodeLocationName(
            sourcePath, scanTargetPath, dockerTar, projectName, projectVersionName, isScassFallback
        );
        
        // Execute - Second call
        String result2 = codeLocationNameManager.createScanCodeLocationName(
            sourcePath, scanTargetPath, dockerTar, projectName, projectVersionName, isScassFallback
        );
        
        // Verify - Test actual behavior of counter incrementing
        assertEquals("overridden-name signature", result1);
        assertEquals("overridden-name signature 2", result2);
    }
    
    @Test
    public void testCreateScanCodeLocationNameWithOverrideCalledTwiceWithScassFallbackSecondTime() {
        // Setup
        File sourcePath = mock(File.class);
        File scanTargetPath = mock(File.class);
        File dockerTar = null;
        String projectName = "testProject";
        String projectVersionName = "1.0.0";
        
        // Execute - First call without SCASS fallback
        String result1 = codeLocationNameManager.createScanCodeLocationName(
            sourcePath, scanTargetPath, dockerTar, projectName, projectVersionName, false
        );
        
        // Execute - Second call with SCASS fallback (should reset counter)
        String result2 = codeLocationNameManager.createScanCodeLocationName(
            sourcePath, scanTargetPath, dockerTar, projectName, projectVersionName, true
        );
        
        // Verify - Test that counter resets when isScassFallback is true
        assertEquals("overridden-name signature", result1);
        assertEquals("overridden-name signature", result2); // Should not have "2" due to counter reset
    }
}