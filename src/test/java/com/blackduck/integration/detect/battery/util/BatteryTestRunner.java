package com.blackduck.integration.detect.battery.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;

import com.blackduck.integration.configuration.property.Property;
import com.blackduck.integration.detect.battery.util.assertions.BatteryBdioAssert;
import com.blackduck.integration.detect.battery.util.executable.ResourceCopyingExecutableCreator;
import com.blackduck.integration.executable.ExecutableRunnerException;

import freemarker.template.TemplateException;

public abstract class BatteryTestRunner {
    protected final BatteryContext batteryContext;
    private boolean shouldAssertBdio = false;
    private boolean manualCleanup = false;

    public BatteryTestRunner(String name) {
        this.batteryContext = new BatteryContext(name);
    }

    public BatteryTestRunner(String testName, String resourcePrefix) {
        this.batteryContext = new BatteryContext(testName, resourcePrefix);
    }

    protected abstract List<String> generateArguments() throws IOException;

    public DetectOutput run() {
        DetectOutput detectOutput = null;
        try {
            List<String> allArguments = new ArrayList<>();
            allArguments.addAll(batteryContext.initialize());
            allArguments.addAll(generateArguments());
            List<String> standardOut = new BatteryDetectRunner(batteryContext.getOutputDirectory(), batteryContext.getScriptDirectory(), "").runDetect(allArguments, false);
            detectOutput = new DetectOutput(
                standardOut,
                batteryContext.getSourceDirectory(),
                batteryContext.getStatusJson(),
                batteryContext.getExtractedDiagnosticZip().orElse(null)
            );
            if (shouldAssertBdio) {
                new BatteryBdioAssert(batteryContext.getTestName(), batteryContext.getResourcePrefix()).assertBdio(
                    batteryContext.getBdioDirectory(),
                    batteryContext.getBdioFileName() + ".bdio",
                    batteryContext.getCompareDirectory()
                );
            }
        } catch (ExecutableRunnerException | JSONException | IOException | TemplateException e) {
            Assertions.assertNull(e, "An exception should not have been thrown!");
        } finally {
            if (!manualCleanup) {
                batteryContext.checkAndCleanupBatteryDirectory();
            }
        }

        Assertions.assertNotNull(detectOutput, "");

        return detectOutput;
    }

    public void expectBdioResources() {
        this.shouldAssertBdio = true;
    }

    //Convenience methods for the context:
    public void executableFromResourceFiles(Property detectProperty, String... resourceFiles) {
        batteryContext.executableFromResourceFiles(detectProperty, resourceFiles);
    }

    public void executableSourceFileFromResourceFiles(String windowsName, String linuxName, String... resourceFiles) {
        batteryContext.executableSourceFileFromResourceFiles(windowsName, linuxName, resourceFiles);
    }

    public ResourceCopyingExecutableCreator executableThatCopiesFiles(Property detectProperty, String... resourceFiles) {
        return batteryContext.executableThatCopiesFiles(detectProperty, resourceFiles);
    }

    public ResourceCopyingExecutableCreator executableSourceFileThatCopiesFiles(String windowsName, String linuxName, String... resourceFiles) {
        return batteryContext.executableSourceFileThatCopiesFiles(windowsName, linuxName, resourceFiles);
    }

    public void executable(Property detectProperty, String... responses) {
        batteryContext.executable(detectProperty, responses);
    }

    public void git(String origin, String branch) {
        // If the hash is not important to the Battery test, use this random Detect commit as a default response.
        batteryContext.git(origin, branch, "97676d12063839aaf1f572bf96aee96da01ab053");
    }

    public void git(String origin, String branch, String commitHash) {
        batteryContext.git(origin, branch, commitHash);
    }

    public void sourceDirectoryNamed(String name) {
        batteryContext.sourceDirectoryNamed(name);
    }

    public void sourceFileNamed(String filename) {
        batteryContext.sourceFileNamed(filename);
    }

    @NotNull
    public void sourceFileNamed(String filename, @NotNull String... lines) {
        batteryContext.sourceFileNamed(filename, lines);
    }

    public void addDirectlyToSourceFolderFromExpandedResource(String filename) {
        batteryContext.addDirectlyToSourceFolderFromExpandedResource(filename);
    }

    public void sourceFolderFromExpandedResource(String filename) {
        batteryContext.sourceFolderFromExpandedResource(filename);
    }

    public void sourceFileFromResource(String filename) {
        batteryContext.sourceFileFromResource(filename);
    }

    public void setManualCleanup(boolean value) {
        manualCleanup = value;
    }

    public void cleanup() {
        batteryContext.checkAndCleanupBatteryDirectory();
    }

    public void executableWithExitCode(Property pathProperty, String exitCode) {
        batteryContext.executableWithExitCode(pathProperty, exitCode);
    }
}
