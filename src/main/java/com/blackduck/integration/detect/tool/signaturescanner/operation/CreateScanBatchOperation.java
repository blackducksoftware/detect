package com.blackduck.integration.detect.tool.signaturescanner.operation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatch;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatchBuilder;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.ApiScannerInstaller;
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
    private static final SignatureScannerVersion MIN_SCASS_VERSION = new SignatureScannerVersion(1, 0, 0);
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
        @Nullable DockerTargetData dockerTargetData
    )
        throws DetectUserFriendlyException {
        return createScanBatch(detectRunUuid, projectNameVersion, signatureScanPaths, blackDuckRunData, dockerTargetData);
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
        @Nullable BlackDuckRunData blackDuckRunData,
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
        
        attemptToSetCsvArchive(scanJobBuilder, blackDuckRunData);
        
        attemptToSetScassScan(scanJobBuilder, blackDuckRunData);

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
    // TODO might need to fix this version check
    // TODO this is likely okay as we are checking blackduck version and not scan cli
    private void attemptToSetCsvArchive(ScanBatchBuilder scanJobBuilder, BlackDuckRunData blackDuckRunData) {
        if (signatureScannerOptions.getCsvArchive()) {
            if (blackDuckRunData != null 
                    && blackDuckRunData.getBlackDuckServerVersion().isPresent()
                    && !MIN_CSV_ARCHIVE_VERSION.isAtLeast(blackDuckRunData.getBlackDuckServerVersion().get())) {
                logger.error("The associated Black Duck server version is not compatible with the CSV archive feature.");
            }
            scanJobBuilder.csvArchive(signatureScannerOptions.getCsvArchive());        
        }
    }
    
    // TODO this isn't great, if user specifies their own scanner via
    // detect.blackduck.signature.scanner.local.path we don't know what version it is
    // In addition, right now we are always checking based on BlackDuck version and not
    // looking at the blackDuckVersion.txt file.
    // TODO we need to check file to see what version scan cli is or even run it if file isn't there
    // TODO somewhere we need a check for BD too (check with Vlad)
    private void attemptToSetScassScan(ScanBatchBuilder scanJobBuilder, BlackDuckRunData blackDuckRunData) {
        try {
            SignatureScannerVersion signatureScannerVersion = getSignatureScannerVersion();
            
            // blackDuckRunData.getBlackDuckServerVersion().get()
            
            if (blackDuckRunData != null 
                    && blackDuckRunData.getBlackDuckServerVersion().isPresent()
                    && signatureScannerVersion.isAtLeast(MIN_SCASS_VERSION)) {
                scanJobBuilder.scassScan(true);
            }
        } catch (IOException e) {
            // Be cautious and do a non-SCASS scan if we can't obtain the signature scanner version.
            scanJobBuilder.scassScan(false);
        }
    }

    // TODO run --version if local, otherwise read file
    private SignatureScannerVersion getSignatureScannerVersion() throws IOException {
        // If user overrides where the signature scanner is it will be stored here. 
        Optional<Path> localScannerInstallPath = signatureScannerOptions.getLocalScannerInstallPath();
        
        if (localScannerInstallPath.isPresent()) {
            // Run --version to determine version
            Path path = localScannerInstallPath.get();
            
            return null;
        } else {
            // Read blackDuckVersion.txt file to determine version
            File toolsDirectory = directoryManager.getPermanentDirectory();
            File scanCliDirectory = new File (toolsDirectory, ApiScannerInstaller.BLACK_DUCK_SIGNATURE_SCANNER_INSTALL_DIRECTORY);
            File scanCliVersionFile = new File(scanCliDirectory, ApiScannerInstaller.VERSION_FILENAME);
            String localScannerVersion = FileUtils.readFileToString(scanCliVersionFile, Charset.defaultCharset());
            return parseSemVer(localScannerVersion);
        } 
    }
    
    private SignatureScannerVersion parseSemVer(String version) throws IllegalArgumentException {
        // Regular expression to match x.y.z format, optionally followed by a suffix (e.g., -SNAPSHOT)
        String regex = "^(\\d+)\\.(\\d+)\\.(\\d+)(?:[-\\w]*)?$";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(version);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid Signature Scanner version format: " + version);
        }

        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = Integer.parseInt(matcher.group(3));
            return new SignatureScannerVersion(major, minor, patch);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Signature Scanner version components must be integers: " + version, e);
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
