package com.blackduck.integration.detect.tool.signaturescanner.operation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatch;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatchBuilder;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.ScanTarget;
import com.blackduck.integration.blackduck.configuration.BlackDuckServerConfig;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.DockerTargetData;
import com.blackduck.integration.detect.tool.signaturescanner.BlackDuckSignatureScannerOptions;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScanPath;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerVersion;
import com.blackduck.integration.detect.workflow.codelocation.CodeLocationNameManager;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;
import com.blackduck.integration.util.NameVersion;

public class CreateScanBatchOperation {
    private static final SignatureScannerVersion MIN_CSV_ARCHIVE_VERSION = new SignatureScannerVersion(2025, 1, 0);
    private static final SignatureScannerVersion MIN_SCASS_VERSION = new SignatureScannerVersion(1, 0, 1);
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final BlackDuckSignatureScannerOptions signatureScannerOptions;
    private final DirectoryManager directoryManager;
    private final CodeLocationNameManager codeLocationNameManager;

    public CreateScanBatchOperation(BlackDuckSignatureScannerOptions signatureScannerOptions, DirectoryManager directoryManager, CodeLocationNameManager codeLocationNameManager) {
        this.signatureScannerOptions = signatureScannerOptions;
        this.directoryManager = directoryManager;
        this.codeLocationNameManager = codeLocationNameManager;
    }

    public ScanBatch createScanBatchWithBlackDuck(
        String detectRunUuid,
        NameVersion projectNameVersion,
        List<SignatureScanPath> signatureScanPaths,
        BlackDuckRunData blackDuckRunData,
        @Nullable DockerTargetData dockerTargetData, 
        boolean isScassFallback
    )
        throws DetectUserFriendlyException {
        return createScanBatch(detectRunUuid, projectNameVersion, signatureScanPaths, blackDuckRunData, dockerTargetData, isScassFallback);
    }

    public ScanBatch createScanBatchWithoutBlackDuck(
        String detectRunUuid,
        NameVersion projectNameVersion,
        List<SignatureScanPath> signatureScanPaths,
        @Nullable DockerTargetData dockerTargetData
    )
        throws DetectUserFriendlyException {
        //when offline, we must still call this with 'null' as a workaround for library issues, so offline scanner must be created with this set to null.
        return createScanBatch(detectRunUuid, projectNameVersion, signatureScanPaths, null, dockerTargetData, false);
    }

    private ScanBatch createScanBatch(
        String detectRunUuid,
        NameVersion projectNameVersion,
        List<SignatureScanPath> signatureScanPaths,
        @Nullable BlackDuckRunData blackDuckRunData,
        @Nullable DockerTargetData dockerTargetData, 
        boolean isScassFallback
    )
        throws DetectUserFriendlyException {
        ScanBatchBuilder scanJobBuilder = new ScanBatchBuilder();
        scanJobBuilder.scanMemoryInMegabytes(signatureScannerOptions.getScanMemory());
        scanJobBuilder.outputDirectory(directoryManager.getScanOutputDirectory());

        scanJobBuilder.dryRun(signatureScannerOptions.getDryRun());
        scanJobBuilder.cleanupOutput(false);
        if (conditionalCorrelationFilter(signatureScannerOptions.getSnippetMatching().isPresent(), "Snippet matching")) {
            scanJobBuilder.snippetMatching(signatureScannerOptions.getSnippetMatching().get());
        }
        scanJobBuilder.uploadSource(signatureScannerOptions.getUploadSource());
        if (conditionalCorrelationFilter(signatureScannerOptions.getLicenseSearch(), "License search")) {
            scanJobBuilder.licenseSearch(signatureScannerOptions.getLicenseSearch());
        }
        if (conditionalCorrelationFilter(signatureScannerOptions.getCopyrightSearch(), "Copyright search")) {
            scanJobBuilder.copyrightSearch(signatureScannerOptions.getCopyrightSearch());
        }
        signatureScannerOptions.getAdditionalArguments().ifPresent(scanJobBuilder::additionalScanArguments);
        
        scanJobBuilder.rapid(signatureScannerOptions.getIsStateless());
        
        scanJobBuilder.bomCompareMode(signatureScannerOptions.getBomCompareMode().toString());
        
        attemptToSetCsvArchive(scanJobBuilder, blackDuckRunData);
        
        attemptToSetScassScan(scanJobBuilder, blackDuckRunData, isScassFallback);

        String projectName = projectNameVersion.getName();
        String projectVersionName = projectNameVersion.getVersion();
        scanJobBuilder.projectAndVersionNames(projectName, projectVersionName);

        signatureScannerOptions.getIndividualFileMatching()
            .ifPresent(scanJobBuilder::individualFileMatching);
        
        signatureScannerOptions.getReducedPersistence()
            .ifPresent(scanJobBuilder::reducedPersistence);

        // Someday the integrated matching enabled option will (we think) go away, and we'll always provide
        // detectRunUuid as correlationId, but for now it's optional.
        if (signatureScannerOptions.isCorrelatedScanningEnabled()) {
            scanJobBuilder.correlationId(detectRunUuid);
        }

        File sourcePath = directoryManager.getSourceDirectory();

        for (SignatureScanPath scanPath : signatureScanPaths) {
            File dockerTarget = null;
            if (dockerTargetData != null) {
                dockerTarget = dockerTargetData.getSquashedImage().orElse(dockerTargetData.getProvidedImageTar().orElse(null));
            }
            
            String codeLocationName = codeLocationNameManager.createScanCodeLocationName(
                sourcePath,
                scanPath.getTargetPath(),
                dockerTarget,
                projectName,
                projectVersionName,
                isScassFallback
            );
            scanJobBuilder.addTarget(ScanTarget.createBasicTarget(scanPath.getTargetCanonicalPath(), scanPath.getExclusions(), codeLocationName));
        }

        BlackDuckServerConfig blackDuckServerConfig = blackDuckRunData != null ? blackDuckRunData.getBlackDuckServerConfig() : null;
        scanJobBuilder.fromBlackDuckServerConfig(blackDuckServerConfig);//when offline, we must still call this with 'null' as a workaround for library issues, so offline scanner must be created with this set to null.
        try {
            return scanJobBuilder.build();
        } catch (IllegalArgumentException e) {
            throw new DetectUserFriendlyException(e.getMessage(), e, ExitCodeType.FAILURE_CONFIGURATION);
        }
    }
    
    /**
     * If we are online and if a user has specified they want csvArchives, warn if the 
     * BlackDuck server we are connected to can't handle it.
     */
    private void attemptToSetCsvArchive(ScanBatchBuilder scanJobBuilder, BlackDuckRunData blackDuckRunData) {
        if (signatureScannerOptions.getCsvArchive()) {
            if (blackDuckRunData != null 
                    && blackDuckRunData.getBlackDuckServerVersion().isPresent()
                    && !blackDuckRunData.getBlackDuckServerVersion().get().isAtLeast(MIN_CSV_ARCHIVE_VERSION)) {
                logger.error("The associated Black Duck server version is not compatible with the CSV archive feature.");
            }
            scanJobBuilder.csvArchive(signatureScannerOptions.getCsvArchive());        
        }
    }

    private void attemptToSetScassScan(ScanBatchBuilder scanJobBuilder, BlackDuckRunData blackDuckRunData, boolean isScassFallback) {
        try {
            SignatureScannerVersion signatureScannerVersion = 
                    SignatureScanVersionChecker.getSignatureScannerVersion(logger, signatureScannerOptions.getLocalScannerInstallPath(), directoryManager.getPermanentDirectory());

            if (signatureScannerVersion != null 
                    && signatureScannerVersion.isAtLeast(MIN_SCASS_VERSION)
                    && !isScassFallback) {
                scanJobBuilder.scassScan(true);
            }
        } catch (IOException e) {
            // Be cautious and do a non-SCASS scan if we can't obtain the signature scanner version.
            logger.debug("Unable to determine the signature scanner version. {}. SCASS will not be performed.", e.getMessage());
            scanJobBuilder.scassScan(false);
        }
    }

    private boolean conditionalCorrelationFilter(boolean toCheck, String toWarn) {
        if (toCheck) {
            if (signatureScannerOptions.isCorrelatedScanningEnabled()) {
                logger.warn("{} is not compatible with Integrated Matching feature and will be skipped. Please re-run {} with integrated matching disabled.", toWarn, toWarn.toLowerCase());
            } else {
                return true;
            }
        }
        return false;
    }

}
