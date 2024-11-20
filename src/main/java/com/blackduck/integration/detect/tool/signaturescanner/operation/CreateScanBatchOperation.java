package com.blackduck.integration.detect.tool.signaturescanner.operation;

import java.io.File;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatch;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatchBuilder;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.ScanTarget;
import com.blackduck.integration.blackduck.configuration.BlackDuckServerConfig;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.run.data.DockerTargetData;
import com.blackduck.integration.detect.tool.signaturescanner.BlackDuckSignatureScannerOptions;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScanPath;
import com.blackduck.integration.detect.workflow.codelocation.CodeLocationNameManager;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;
import com.blackduck.integration.util.NameVersion;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateScanBatchOperation {
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
        BlackDuckServerConfig blackDuckServerConfig,
        @Nullable DockerTargetData dockerTargetData
    )
        throws DetectUserFriendlyException {
        return createScanBatch(detectRunUuid, projectNameVersion, signatureScanPaths, blackDuckServerConfig, dockerTargetData);
    }

    public ScanBatch createScanBatchWithoutBlackDuck(
        String detectRunUuid,
        NameVersion projectNameVersion,
        List<SignatureScanPath> signatureScanPaths,
        @Nullable DockerTargetData dockerTargetData
    )
        throws DetectUserFriendlyException {
        //when offline, we must still call this with 'null' as a workaround for library issues, so offline scanner must be created with this set to null.
        return createScanBatch(detectRunUuid, projectNameVersion, signatureScanPaths, null, dockerTargetData);
    }

    private ScanBatch createScanBatch(
        String detectRunUuid,
        NameVersion projectNameVersion,
        List<SignatureScanPath> signatureScanPaths,
        @Nullable BlackDuckServerConfig blackDuckServerConfig,
        @Nullable DockerTargetData dockerTargetData
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
                projectVersionName
            );
            scanJobBuilder.addTarget(ScanTarget.createBasicTarget(scanPath.getTargetCanonicalPath(), scanPath.getExclusions(), codeLocationName));
        }

        scanJobBuilder.fromBlackDuckServerConfig(blackDuckServerConfig);//when offline, we must still call this with 'null' as a workaround for library issues, so offline scanner must be created with this set to null.
        try {
            return scanJobBuilder.build();
        } catch (IllegalArgumentException e) {
            throw new DetectUserFriendlyException(e.getMessage(), e, ExitCodeType.FAILURE_CONFIGURATION);
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
