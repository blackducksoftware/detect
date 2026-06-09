package com.blackduck.integration.detect.battery.detector;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.battery.util.DetectorBatteryTestRunner;
import com.blackduck.integration.detect.configuration.DetectProperties;

/**
 * Battery tests for the UV (Python package manager) detector.
 * 
 * Battery tests validate the full detection pipeline using real project structures.
 * They simulate running Detect against actual project files and verify the detector
 * is triggered and completes successfully.
 * 
 * These tests ensure:
 * - The UV detector is properly triggered when pyproject.toml with [tool.uv] is present
 * - The `uv tree` command output is correctly parsed
 * - Dependencies are extracted into code locations
 */
@Tag("battery")
public class UVBattery {

    /**
     * Tests a simple single-package UV project with basic dependencies.
     * Validates that the UV CLI detector can parse pyproject.toml and
     * process `uv tree` output correctly.
     */
    @Test
    void simpleProject() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("uv-simple");
        test.sourceDirectoryNamed("linux-uv-simple");
        test.sourceFileFromResource("pyproject.toml");
        test.executableFromResourceFiles(DetectProperties.DETECT_UV_PATH, "uv-tree.xout");
        test.expectBdioResources();
        test.run();
    }

    /**
     * Tests a UV project with dev dependencies included.
     * Validates that dev dependencies are properly captured in the BOM
     * when not excluded via --no-group flags.
     */
    @Test
    void projectWithDevDependencies() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("uv-devdeps");
        test.sourceDirectoryNamed("linux-uv-devdeps");
        test.sourceFileFromResource("pyproject.toml");
        test.executableFromResourceFiles(DetectProperties.DETECT_UV_PATH, "uv-tree.xout");
        test.expectBdioResources();
        test.run();
    }

    /**
     * Tests a UV workspace project with multiple members.
     * Validates that workspace members are detected and each produces
     * its own code location in the resulting BOM.
     */
    @Test
    void workspaceProject() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("uv-workspace");
        test.sourceDirectoryNamed("linux-uv-workspace");
        test.sourceFileFromResource("pyproject.toml");
        test.executableFromResourceFiles(DetectProperties.DETECT_UV_PATH, "uv-tree.xout");
        test.expectBdioResources();
        test.run();
    }
}

