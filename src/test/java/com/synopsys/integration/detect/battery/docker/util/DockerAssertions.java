package com.synopsys.integration.detect.battery.docker.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.Set;

import com.synopsys.integration.common.util.Bds;
import com.synopsys.integration.detect.lifecycle.autonomous.model.PackageManagerType;
import com.synopsys.integration.detect.lifecycle.autonomous.model.ScanSettings;
import com.synopsys.integration.detect.lifecycle.autonomous.model.ScanType;
import org.junit.jupiter.api.Assertions;

import com.google.gson.Gson;
import com.synopsys.integration.detect.workflow.report.output.FormattedDetectorOutput;
import com.synopsys.integration.detect.workflow.report.output.FormattedOperationOutput;
import com.synopsys.integration.detect.workflow.report.output.FormattedOutput;
import com.synopsys.integration.detect.workflow.report.output.FormattedStatusOutput;
import com.synopsys.integration.util.NameVersion;

public class DockerAssertions {
    private final DockerDetectResult dockerDetectResult;
    private final File outputDirectory;
    private final File bdioDirectory;
    private FormattedOutput statusJson = null;
    private ScanSettings scanSettingsJson = null;

    public DockerAssertions(DockerTestDirectories testDirectories, DockerDetectResult dockerDetectResult) {
        this.dockerDetectResult = dockerDetectResult;
        this.outputDirectory = testDirectories.getResultOutputDirectory();
        this.bdioDirectory = testDirectories.getResultBdioDirectory();
    }

    public void successfulDetectorType(String detectorType) {
        successfulThingLogged(detectorType);
        successfulDetectorTypeStatusJson(detectorType);
    }

    public void successfulTool(String tool) {
        successfulThingLogged(tool);
        successfulToolStatusJson(tool);
    }

    public void successfulThingLogged(String detectorType) {
        Assertions.assertTrue(dockerDetectResult.getDetectLogs().contains(detectorType + ": SUCCESS"));
    }

    public void successfulDetectorTypeStatusJson(String detectorType) {
        FormattedOutput statusJson = locateStatusJson();
        Optional<FormattedDetectorOutput> detector = statusJson.detectors.stream().filter(it -> it.detectorType.equals(detectorType))
            .findFirst();

        Assertions.assertTrue(detector.isPresent(), "Could not find required detector in status json detector list.");

        Assertions.assertEquals("SUCCESS", detector.get().status);
    }


    public void successfulOperationStatusJson(String operationKey) {
        FormattedOutput statusJson = locateStatusJson();
        Optional<FormattedOperationOutput> operation = statusJson.operations.stream().filter(it -> it.descriptionKey.equals(operationKey))
            .findFirst();

        Assertions.assertTrue(operation.isPresent(), "Could not find required operation '" + operationKey + "' in status json detector list.");

        Assertions.assertEquals("SUCCESS", operation.get().status);
    }

    public void successfulToolStatusJson(String detectorType) {
        FormattedOutput statusJson = locateStatusJson();
        Optional<FormattedStatusOutput> tool = statusJson.status.stream().filter(it -> it.key.equals(detectorType))
            .findFirst();

        Assertions.assertTrue(tool.isPresent(), "Could not find required detector in status json detector list.");

        Assertions.assertEquals("SUCCESS", tool.get().status);
    }

    private FormattedOutput locateStatusJson() {
        if (statusJson != null)
            return statusJson;

        File runs = new File(outputDirectory, "runs");
        File[] runDirectories = runs.listFiles();
        Assertions.assertNotNull(runDirectories, "Could not find any run directories, looked in: " + runs);
        Assertions.assertEquals(1, runDirectories.length, "There should be exactly one run directory (from this latest run).");

        File run = runDirectories[0];
        File status = new File(run, "status");
        Assertions.assertTrue(status.exists(), "Could not find status directory in the run directory!");
        File statusJsonFile = new File(status, "status.json");
        Assertions.assertTrue(statusJsonFile.exists(), "Could not find status json in the status directory!");

        try {
            statusJson = new Gson().fromJson(new FileReader(statusJsonFile), FormattedOutput.class);
        } catch (FileNotFoundException e) {
            Assertions.fail("Unable to parse status json with gson.", e);
        }
        return statusJson;
    }

    public void locateScanSettingsFile() {
        File scanSettingsDirectory = new File(outputDirectory,"scan-settings");
        Assertions.assertNotNull(scanSettingsDirectory, "Could not find any scanSettings directories, looked in: " + outputDirectory);
        File[] scanSettingsFiles = scanSettingsDirectory.listFiles();
        Assertions.assertNotNull(scanSettingsFiles, "There are no scan-settings file inside scan-settings directory");
        Assertions.assertEquals(1, scanSettingsFiles.length, "There should be exactly one scan settings file (from this latest run).");

        File scanSettings = scanSettingsFiles[0];
        Assertions.assertTrue(scanSettings.exists(), "Could not find scan-settings json in the directory!");

        try {
            scanSettingsJson = new Gson().fromJson(new FileReader(scanSettings), ScanSettings.class);
        } catch (FileNotFoundException e) {
            Assertions.fail("Unable to parse scans-settings json file", e);
        }
    }

    public void autonomousScanModeAssertions(String scanMode) {
        String scanModeInFile = scanSettingsJson.getGlobalDetectProperties().get("detect.blackduck.scan.mode");
        Assertions.assertEquals(scanModeInFile, scanMode, "Expected Blackduck scan mode to be " + scanMode + " but it is actually " + scanModeInFile);
    }

    public void autonomousDetectorAssertions(String detectorType, String... propertiesPresent) {
        Optional<PackageManagerType> detectorTypeInFile = scanSettingsJson.getDetectorTypes().stream().filter(detector -> detector.getDetectorTypeName().equals(detectorType)).findFirst();
        Assertions.assertTrue(detectorTypeInFile.isPresent(), "Expected Scan Settings File to contain Detector Type: " + detectorType);

        Set<String> propertiesToCheck = Bds.setOf(propertiesPresent);

        PackageManagerType packageManager = detectorTypeInFile.get();

        if(!propertiesToCheck.isEmpty()) {
            propertiesToCheck.forEach(property -> {
                Assertions.assertTrue(packageManager.getDetectorProperties().containsKey(property),"Expected property " + property + " to be present in the scan settings file.");
            });
        }
    }

    public void autonomousScanTypeAssertions(String scanType, String... propertiesPresent) {
        Optional<ScanType> scanTypeOptional = scanSettingsJson.getScanTypes().stream().filter(scanTool -> scanTool.getScanTypeName().equals(scanType)).findFirst();
        Assertions.assertTrue(scanTypeOptional.isPresent(), "Expected Scan Settings File to contain Detector Type: " + scanType);

        Set<String> propertiesToCheck = Bds.setOf(propertiesPresent);

        ScanType scanType1 = scanTypeOptional.get();

        if(!propertiesToCheck.isEmpty()) {
            propertiesToCheck.forEach(property -> {
                Assertions.assertTrue(scanType1.getScanProperties().containsKey(property),"Expected property " + property + " to be present in the scan settings file.");
            });
        }
    }

    public void atLeastOneBdioFile() {
        Assertions.assertNotNull(bdioDirectory, "Expected at least one bdio file!");
        Assertions.assertNotNull(bdioDirectory.listFiles(), "Expected at least one bdio file!");
        Assertions.assertTrue(Objects.requireNonNull(bdioDirectory.listFiles()).length > 0, "Expected at least one bdio file!");
    }

    public void logContainsPattern(String pattern) {
        Assertions.assertNotNull(pattern);
        Pattern regex = Pattern.compile("(?s).*" + pattern + ".*", Pattern.MULTILINE);
        Assertions.assertTrue(regex.matcher(dockerDetectResult.getDetectLogs()).matches(), "Expected logs to contain '" + regex + "' but they did not.");
    }

    // TODO convenience methods on dockerAssertions for testing whether a detector was ATTEMPTED, SUCCESSFUL, ...

    public void logContains(String thing) {
        Assertions.assertTrue(dockerDetectResult.getDetectLogs().contains(thing), "Expected logs to contain '" + thing + "' but they did not.");
    }

    public void logDoesNotContain(String thing) {
        Assertions.assertFalse(dockerDetectResult.getDetectLogs().contains(thing), "Expected logs to NOT contain '" + thing + "' but they did.");
    }

    public void successfulOperation(String operationName) {
        successfulOperationStatusJson(operationName);
        successfulThingLogged(operationName);
    }

    public void projectVersion(NameVersion nameVersion) {
        projectVersion(nameVersion.getName(), nameVersion.getVersion());
    }

    public void projectVersion(String project, String version) {
        FormattedOutput statusJson = locateStatusJson();
        Assertions.assertEquals(project, statusJson.projectName);
        Assertions.assertEquals(version, statusJson.projectVersion);
        logContains("Project name: " + project); //Should we rely solely on the status json?
        logContains("Project version: " + version);
    }

    public void bdioFiles(int bdioCount) {
        checkBdioDirectory();
        Assertions.assertEquals(bdioCount, Objects.requireNonNull(bdioDirectory.listFiles()).length);
    }

    public void bdioFileCreated(String requiredBdioFilename) {
        checkBdioDirectory();
        Assertions.assertTrue(
            Arrays.asList(bdioDirectory.listFiles()).stream()
                .map(File::getName)
                .filter(requiredBdioFilename::equals)
                .findAny().isPresent(),
            String.format("Expected BDIO file %s, but it was not created", requiredBdioFilename)
        );
    }

    public void exitCodeIs(int expected) {
        Assertions.assertEquals(expected, dockerDetectResult.getExitCode());
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void resultProducedAtLocation(String location) {
        Assertions.assertTrue(locateStatusJson().results.stream().anyMatch(result -> result.location.equals(location)), "Unable to find result: " + location);
    }

    private void checkBdioDirectory() {
        Assertions.assertNotNull(bdioDirectory, "Bdio directory did not exist!");
        Assertions.assertNotNull(bdioDirectory.listFiles(), "Bdio directory list files was null.");
    }
}