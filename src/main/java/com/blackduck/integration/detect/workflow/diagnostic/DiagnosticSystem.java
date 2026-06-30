package com.blackduck.integration.detect.workflow.diagnostic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.configuration.config.PropertyConfiguration;
import com.blackduck.integration.detect.configuration.DetectInfo;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.workflow.DetectRunId;
import static com.blackduck.integration.detect.workflow.componentlocationanalysis.GenerateComponentLocationAnalysisOperation.QUACKPATCH_SUBDIRECTORY_NAME;
import com.blackduck.integration.detect.workflow.event.EventSystem;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;

public class DiagnosticSystem {
    private static final String FAILED_TO_FINISH = "Failed to finish.";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final PropertyConfiguration propertyConfiguration;
    private DiagnosticReportHandler diagnosticReportHandler;
    private DiagnosticLogSystem diagnosticLogSystem;
    private DiagnosticExecutableCapture diagnosticExecutableCapture;
    private DiagnosticFileCapture diagnosticFileCapture;
    private final DetectRunId detectRunId;
    private final DetectInfo detectInfo;
    private final DirectoryManager directoryManager;
    private final EventSystem eventSystem;

    public DiagnosticSystem(
        PropertyConfiguration propertyConfiguration,
        DetectRunId detectRunId,
        DetectInfo detectInfo,
        DirectoryManager directoryManager,
        EventSystem eventSystem,
        SortedMap<String, String> maskedRawPropertyValues
    ) {
        this.propertyConfiguration = propertyConfiguration;
        this.detectRunId = detectRunId;
        this.detectInfo = detectInfo;
        this.directoryManager = directoryManager;
        this.eventSystem = eventSystem;

        init(maskedRawPropertyValues);
    }

    private void init(SortedMap<String, String> maskedRawPropertyValues) {
        System.out.println();
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("Diagnostic mode on.");
        System.out.println("A zip file will be created with logs and relevant Detect output files.");
        System.out.println("It is generally not recommended to leave diagnostic mode on as you must manually clean up the zip.");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println();

        logger.info("Initializing diagnostic components.");
        try {
            diagnosticReportHandler = new DiagnosticReportHandler(directoryManager.getReportOutputDirectory(), detectRunId.getRunId(), eventSystem);
            diagnosticLogSystem = new DiagnosticLogSystem(directoryManager.getLogOutputDirectory(), eventSystem);
            diagnosticExecutableCapture = new DiagnosticExecutableCapture(directoryManager.getExecutableOutputDirectory(), eventSystem);
            diagnosticFileCapture = new DiagnosticFileCapture(directoryManager.getRelevantOutputDirectory(), eventSystem);
        } catch (Exception e) {
            logger.error("Failed to process.", e);
        }

        logger.info("Creating configuration diagnostics reports.");

        diagnosticReportHandler.configurationsReport(detectInfo, propertyConfiguration, maskedRawPropertyValues);

        logger.info("Diagnostics system is ready.");
    }

    public Map<String, String> getAdditionalDockerProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("logging.level.com.blackduck", "TRACE");
        return properties;
    }

    public void finish() {
        logger.info("Finishing diagnostic mode.");

        try {
            logger.info("Finishing reports.");
            diagnosticReportHandler.finish();
        } catch (Exception e) {
            logger.error(FAILED_TO_FINISH, e);
        }

        try {
            logger.info("Finishing logging.");
            diagnosticLogSystem.finish();
        } catch (Exception e) {
            logger.error(FAILED_TO_FINISH, e);
        }

        try {
            logger.info("Finishing executable capture.");
            diagnosticExecutableCapture.finish();
        } catch (Exception e) {
            logger.error(FAILED_TO_FINISH, e);
        }

        if (diagnosticFileCapture != null) {
            try {
                logger.info("Finishing file capture.");
                diagnosticFileCapture.finish();
            } catch (Exception e) {
                logger.error(FAILED_TO_FINISH, e);
            }
        }

        logger.info("Creating diagnostics zip.");
        boolean zipCreated = false;
        try {
            zipCreated = createZip();
        } catch (Exception e) {
            logger.error("Failed to create diagnostic zip. Cleanup will not occur.", e);
        }

        if (!zipCreated) {
            logger.error("Diagnostic mode failed to create zip. Cleanup will not occur.");
        }
        // If detect.diagnostic.archive.path is specified and is not empty, then copy diagnostic zip to that path.
        String detectDiagnosticOutputPath = propertyConfiguration.getValueOrDefault(DetectProperties.DETECT_DIAGNOSTIC_ARCHIVE_PATH);
        if (!detectDiagnosticOutputPath.isEmpty()) {
            File diagnosticZipFile = new File(directoryManager.getRunsOutputDirectory(), detectRunId.getRunId() + ".zip");
            File destinationFile = new File(detectDiagnosticOutputPath + "detect-run-" + detectRunId.getRunId() + ".zip");
            try {
                FileUtils.copyFile(diagnosticZipFile, destinationFile);
                logger.info("Diagnostic zip file copied to {}.", destinationFile.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Failed to copy diagnostic zip file to {}. Error: {}", destinationFile.getAbsolutePath(), e.getMessage());
            }
        }

        logger.info("Diagnostic mode has completed.");
    }

    public void appendBlackDuckServerProperties(Map<String, String> serverProperties) {
        if (diagnosticReportHandler != null) {
            diagnosticReportHandler.appendBlackDuckServerProperties(serverProperties);
        }
    }

    private boolean createZip() {
        // If quack patch is enabled, then add quack patch output directory to the zip
        if (propertyConfiguration.getValueOrDefault(DetectProperties.DETECT_QUACK_PATCH_ENABLED)) {
            String quackPatchOutputDirPath = propertyConfiguration.getValueOrDefault(DetectProperties.DETECT_QUACK_PATCH_OUTPUT);
            // If quack patch output path is customized, then explicitly include it in the diagnostic zip. Otherwise, default behaviour will automatically pack it.
            if (!quackPatchOutputDirPath.isEmpty()) {
                quackPatchOutputDirPath = quackPatchOutputDirPath.trim() + File.separator + QUACKPATCH_SUBDIRECTORY_NAME;
                logger.info("Adding quack patch output dir {} to the diagnostic zip.", quackPatchOutputDirPath);
                // Copy directory quackPatchOutputDirPath to directoryManager.getRunsOutputDirectory()
                try {
                    FileUtils.copyDirectory(new File(quackPatchOutputDirPath), new File(directoryManager.getScanOutputDirectory().getAbsolutePath() + File.separator + QUACKPATCH_SUBDIRECTORY_NAME));
                } catch (IOException e) {
                    logger.warn("Failed to copy the quack patch output directory to the runs directory. Skipping the addition of quack patch output directory to diagnostics zip file. Error: {}.", e.getMessage());
                }
            }
        }
        List<File> directoriesToCompress = new ArrayList<>();
        directoriesToCompress.add(directoryManager.getRunHomeDirectory());
        DiagnosticZipCreator zipper = new DiagnosticZipCreator();
        return zipper.createDiagnosticZip(detectRunId.getRunId(), directoryManager.getRunsOutputDirectory(), directoriesToCompress);
    }
}
