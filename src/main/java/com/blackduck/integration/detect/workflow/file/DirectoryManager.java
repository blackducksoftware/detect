package com.blackduck.integration.detect.workflow.file;

import java.io.File;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.tool.detector.extraction.ExtractionId;
import com.blackduck.integration.detect.workflow.DetectRunId;

public class DirectoryManager {
    private final Logger logger = LoggerFactory.getLogger(DirectoryManager.class);

    private enum OutputDirectory {
        RUNS("runs"),
        TOOLS("tools"),
        SCAN_SETTINGS("scan-settings");

        private final String directoryName;

        OutputDirectory(String directoryName) {
            this.directoryName = directoryName;
        }

        public String getDirectoryName() {
            return directoryName;
        }
    }

    private enum RunDirectory {
        BDIO("bdio"),
        BINARY("binary"),
        CONTAINER("container"),
        CSV("csv"),
        EXTRACTION("extractions"),
        IMPACT_ANALYSIS("impact-analysis"),
        LOG("logs"),
        EXECUTABLES("executables"),
        RAPID("rapid"),
        RELEVANT("relevant"),
        REPORT("reports"),
        SCAN("scan"),
        SHARED("shared"),
        STATUS("status"),
        STATUS_COPY("status-copy"),
        IAC("iac");

        private final String directoryName;

        RunDirectory(String directoryName) {
            this.directoryName = directoryName;
        }

        public String getDirectoryName() {
            return directoryName;
        }
    }

    private final File userHome;
    private final File runDirectory;
    private final File sourceDirectory;

    private final Map<OutputDirectory, File> outputDirectories = new HashMap<>();
    private final Map<RunDirectory, File> runDirectories = new HashMap<>();

    private final Map<ExtractionId, File> extractionDirectories = new HashMap<>();

    public DirectoryManager(DirectoryOptions directoryOptions, DetectRunId detectRunId) {
        sourceDirectory = directoryOptions.getSourcePathOverride()
            .map(Path::toFile)
            .orElse(new File(System.getProperty("user.dir")));
        logger.info("Source directory: " + sourceDirectory.getAbsolutePath());

        userHome = new File(System.getProperty("user.home"));

        File outputDirectory = directoryOptions.getOutputPathOverride()
            .map(Path::toFile)
            .orElse(new File(userHome, "blackduck"));
        if (outputDirectory.getAbsolutePath().contains("systemprofile")) {
            logger.warn("You appear to be running in 'systemprofile' which can happen when detect is invoked by a system account or as a service.");
            logger.warn("If detect has full access to the output directory, no further action is necessary.");
            logger.warn("However, this folder typically has restricted access and may cause exceptions in detect.");
            logger.warn("To ensure continued operation, supply an output directory using " + DetectProperties.DETECT_OUTPUT_PATH.getName() + " in the future.");
        }
        logger.info("Output directory: " + outputDirectory.getAbsolutePath());

        directoryOptions.getToolsOutputPath()
            .map(Path::toFile)
            .ifPresent(toolPath -> {
                outputDirectories.put(OutputDirectory.TOOLS, toolPath);
                logger.info("Tool directory: " + toolPath.getAbsolutePath());
            });

        EnumSet.allOf(OutputDirectory.class).stream()
            .filter(it -> !outputDirectories.containsKey(it))
            .forEach(it -> outputDirectories.put(it, new File(outputDirectory, it.getDirectoryName())));

        File possibleRunDirectory = new File(getOutputDirectory(OutputDirectory.RUNS), detectRunId.getRunId());
        if (possibleRunDirectory.exists()) {
            logger.warn("A run directory already exists with this detect run id. Will attempt to use a UUID for the run folder in addition.");
            possibleRunDirectory = new File(getOutputDirectory(OutputDirectory.RUNS), detectRunId.getRunId() + "-" + java.util.UUID.randomUUID());
        }
        runDirectory = possibleRunDirectory;

        logger.info("Run directory: " + runDirectory.getAbsolutePath());

        EnumSet.allOf(RunDirectory.class).stream()
            // Do not initialize the directory to copy the status.json file into, we'll set 
            // it later if the user specified the detect.status.json.output.path property.
            .filter(it -> !it.equals(RunDirectory.STATUS_COPY))
            .forEach(it -> runDirectories.put(it, new File(runDirectory, it.getDirectoryName())));

        //overrides
        directoryOptions.getBdioOutputPathOverride()
            .map(Path::toFile)
            .ifPresent(bdioOutputPath -> runDirectories.put(RunDirectory.BDIO, bdioOutputPath));

        directoryOptions.getScanOutputPathOverride()
            .map(Path::toFile)
            .ifPresent(scanOutputPath -> runDirectories.put(RunDirectory.SCAN, scanOutputPath));

        directoryOptions.getImpactOutputPathOverride()
            .map(Path::toFile)
            .ifPresent(scanOutputPath -> runDirectories.put(RunDirectory.IMPACT_ANALYSIS, scanOutputPath));
        
        directoryOptions.getStatusJsonOutputPathOverride()
            .map(Path::toFile)
            .ifPresent(scanOutputPath -> runDirectories.put(RunDirectory.STATUS_COPY, scanOutputPath));
    }

    public File getUserHome() {
        return userHome;
    }

    public File getExtractionOutputDirectory(ExtractionId extractionId) {
        if (extractionDirectories.containsKey(extractionId)) {
            return extractionDirectories.get(extractionId);
        } else {
            String directoryName = extractionId.toUniqueString();
            File newDirectory = new File(getRunDirectory(RunDirectory.EXTRACTION), directoryName);
            newDirectory.mkdir();
            extractionDirectories.put(extractionId, newDirectory);
            return newDirectory;
        }
    }

    public File getSourceDirectory() {
        return sourceDirectory;
    }

    public File getBdioOutputDirectory() {
        return getRunDirectory(RunDirectory.BDIO);
    }

    public File getRunsOutputDirectory() {
        return getOutputDirectory(OutputDirectory.RUNS);
    }

    public File getScanSettingsOutputDirectory() {
        return getOutputDirectory(OutputDirectory.SCAN_SETTINGS);
    }

    public File getScanOutputDirectory() {
        return getRunDirectory(RunDirectory.SCAN);
    }

    public File getRapidOutputDirectory() {
        return getRunDirectory(RunDirectory.RAPID);
    }

    public File getBinaryOutputDirectory() {
        return getRunDirectory(RunDirectory.BINARY);
    }

    public File getContainerOutputDirectory() {
        return getRunDirectory(RunDirectory.CONTAINER);
    }

    public File getImpactAnalysisOutputDirectory() {
        return getRunDirectory(RunDirectory.IMPACT_ANALYSIS);
    }

    public File getRelevantOutputDirectory() {
        return getRunDirectory(RunDirectory.RELEVANT);
    }

    public File getReportOutputDirectory() {
        return getRunDirectory(RunDirectory.REPORT);
    }

    public File getLogOutputDirectory() {
        return getRunDirectory(RunDirectory.LOG);
    }

    public File getStatusOutputDirectory() {
        return getRunDirectory(RunDirectory.STATUS);
    }
    
    public File getJsonStatusOutputDirectory() {
        File actualDirectory = runDirectories.get(RunDirectory.STATUS_COPY);
        if (actualDirectory != null && !actualDirectory.exists()) {
            actualDirectory.mkdirs();
        }
        return actualDirectory;
    }

    public File getExecutableOutputDirectory() {
        return getRunDirectory(RunDirectory.EXECUTABLES);
    }

    public File getIacScanOutputDirectory() {
        return getRunDirectory(RunDirectory.IAC);
    }
    
    public File getCsvOutputDirectory() {
        return getRunDirectory(RunDirectory.CSV);
    }

    public File getRunHomeDirectory() {
        return runDirectory;
    }

    private File getOutputDirectory(OutputDirectory directory) {
        File actualDirectory = outputDirectories.get(directory);
        if (!actualDirectory.exists()) {
            actualDirectory.mkdirs();
        }
        return actualDirectory;
    }

    private File getRunDirectory(RunDirectory directory) {
        File actualDirectory = runDirectories.get(directory);
        if (!actualDirectory.exists()) {
            actualDirectory.mkdirs();
        }
        return actualDirectory;
    }


    public File getSharedDirectory(String name) { // shared across this invocation of detect (inspectors), returns 'shared/name'
        File newSharedFile = new File(getRunDirectory(RunDirectory.SHARED), name);
        newSharedFile.mkdirs();
        return newSharedFile;
    }

    public File getSharedFile(String sharedDirectory, String fileName) { // helper method for shared files, returns 'shared/name/file'
        return new File(getSharedDirectory(sharedDirectory), fileName);
    }

    public File getPermanentDirectory() { // shared across all invocations of detect (scan cli)
        return getOutputDirectory(OutputDirectory.TOOLS);
    }

    public File getPermanentDirectory(String name) { // shared across all invocations of detect (scan cli)
        return new File(getOutputDirectory(OutputDirectory.TOOLS), name);
    }

    public static String getScanDirectoryName() {
        return RunDirectory.SCAN.getDirectoryName();
    }
}