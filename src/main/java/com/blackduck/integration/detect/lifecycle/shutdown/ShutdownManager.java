package com.blackduck.integration.detect.lifecycle.shutdown;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detect.lifecycle.boot.DetectBootResult;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;

public class ShutdownManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final CleanupUtility cleanupUtility;

    public ShutdownManager(CleanupUtility cleanupUtility) {
        this.cleanupUtility = cleanupUtility;
    }

    public void shutdown(DetectBootResult detectBootResult, ShutdownDecision shutdownDecision) {
        if (shutdownDecision.getDiagnosticSystem() != null) {
            shutdownDecision.getDiagnosticSystem().finish();
        }

        if (detectBootResult.getDirectoryManager().isPresent() && shutdownDecision.getCleanupDecision().shouldCleanup()) {
            DirectoryManager directoryManager = detectBootResult.getDirectoryManager().get();
            try {
                List<File> cleanupToSkip = determineSkippedCleanupFiles(shutdownDecision.getCleanupDecision(), directoryManager);
                cleanupUtility.cleanup(directoryManager.getRunHomeDirectory(), cleanupToSkip);
            } catch (Exception e) {
                logger.debug("Error trying cleanup: ", e);
            }
        } else {
            logger.info("Skipping cleanup, it is disabled.");
        }

        if (shutdownDecision.getPhoneHomeManager() != null) {
            try {
                logger.debug("Ending phone home.");
                shutdownDecision.getPhoneHomeManager().endPhoneHome();
            } catch (Exception e) {
                logger.debug(String.format("Error trying to end the phone home task: %s", e.getMessage()));
            }
        }
    }

    private List<File> determineSkippedCleanupFiles(CleanupDecision cleanupDecision, DirectoryManager directoryManager) {
        logger.debug("Detect will cleanup.");
        List<File> cleanupToSkip = new ArrayList<>();
        if (cleanupDecision.shouldPreserveScan()) {
            logger.debug("Will not cleanup scan folder.");
            cleanupToSkip.add(directoryManager.getScanOutputDirectory());
        }
        if (cleanupDecision.shouldPreserveBdio()) {
            logger.debug("Will not cleanup bdio folder.");
            cleanupToSkip.add(directoryManager.getBdioOutputDirectory());
            logger.debug("Will not cleanup impact analysis folder.");
            cleanupToSkip.add(directoryManager.getImpactAnalysisOutputDirectory());
        }
        if (cleanupDecision.shouldPreserveCsv()) {
            logger.debug("Will not cleanup csv folder.");
            cleanupToSkip.add(directoryManager.getCsvOutputDirectory()); 
        }       
        if (cleanupDecision.shouldPreserveAirGap()) {
            logger.debug("Will not cleanup Air Gap file.");
            cleanupToSkip.add(cleanupDecision.getAirGapZip());
        }
        if (cleanupDecision.shouldPreserveIac()) {
            logger.debug("Will not cleanup iac folder.");
            cleanupToSkip.add(directoryManager.getIacScanOutputDirectory());
        }
        return cleanupToSkip;
    }
}
