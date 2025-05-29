package com.blackduck.integration.detect.lifecycle.run.step;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.version.BlackDuckVersion;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.CommonScanResult;
import com.blackduck.integration.detect.lifecycle.run.data.ScanCreationResponse;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.operation.blackduck.ScassScanInitiationResult;
import com.blackduck.integration.detect.util.bdio.protobuf.DetectProtobufBdioHeaderUtil;
import com.blackduck.integration.detect.workflow.codelocation.CodeLocationNameManager;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;

/**
 * This class is intended to host common code for running SCASS and the newer BDBA based scans
 * if SCASS it not enabled. Right now this code powers binary and container scans but further
 * refactoring might be possible once package manager and signature scans are supported natively
 * by Detect.
 */
public class CommonScanStepRunner {
    private static final BlackDuckVersion MIN_SCASS_SCAN_VERSION = new BlackDuckVersion(2025, 1, 1);
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    // Supported scan types
    public static final String BINARY = "BINARY";
    public static final String CONTAINER = "CONTAINER";
    
    // TODO I think this is right from scan summary representation endpoint
    public static final String PACKAGE_MANAGER = "PACKAGE_MANAGER";

    public static boolean areScassScansPossible(Optional<BlackDuckVersion> blackDuckVersion) {
        return blackDuckVersion.isPresent() && blackDuckVersion.get().isAtLeast(MIN_SCASS_SCAN_VERSION);
    }
    
    public CommonScanResult performCommonScan(NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData,
            Optional<File> scanFile, OperationRunner operationRunner, Gson gson, String scanType, String codeLocationName) throws OperationException, IntegrationException {

        if (codeLocationName == null || codeLocationName.isEmpty()) {
            codeLocationName = createCodeLocationName(scanFile, projectNameVersion, scanType, operationRunner.getCodeLocationNameManager());
        }
            
        // call BlackDuck to create a scanID and determine where to upload the file
        ScassScanInitiationResult initResult = operationRunner.initiateScan(
            projectNameVersion, 
            scanFile.get(),
            getOutputDirectory(operationRunner, scanType),
            blackDuckRunData, 
            scanType, 
            gson,
            codeLocationName
        );
        
        String operationName = String.format("%s Upload", 
                scanType.substring(0, 1).toUpperCase() + scanType.substring(1).toLowerCase());
        
        String finalCL = codeLocationName;
        
        return operationRunner.getAuditLog().namedPublic(operationName, () -> {
            UUID scanId = performCommonUpload(projectNameVersion, blackDuckRunData, scanFile, operationRunner, scanType,
                    initResult, finalCL);
            
            return new CommonScanResult(scanId, finalCL);
        });
    }

    public UUID performCommonUpload(NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData,
            Optional<File> scanFile, OperationRunner operationRunner, String scanType,
            ScassScanInitiationResult initResult, String codeLocationName) throws IntegrationException, OperationException {
        ScanCreationResponse scanCreationResponse = initResult.getScanCreationResponse();

        UUID scanId = UUID.fromString(scanCreationResponse.getScanId());
        
        if (StringUtils.isNotEmpty(scanCreationResponse.getUploadUrl())) {
            try {
                // This is a SCASS capable server server, SCASS is enabled, and we can access the upload URL.
                ScassScanStepRunner scassScanStepRunner = createScassScanStepRunner(blackDuckRunData);
                scassScanStepRunner.runScassScan(Optional.of(initResult.getFileToUpload()), scanCreationResponse);

                return scanId;
            } catch (IntegrationException e) {
                // If we can't access the SCASS uplaod URL, we create a new scanId so we can try the BDBA flow.
                // Note: as of 2025.1.1 there is no endpoint to cancel a SCASS scan.
                logger.info("Error uploading to SCASS URL: " + e.getMessage());
                scanId = createFallbackScanId(operationRunner, scanType, projectNameVersion, codeLocationName, scanFile.get().length(), blackDuckRunData);
            }
        }
        // This is a SCASS capable server server but SCASS is not enabled or the GCP URL is inaccessible.
        BdbaScanStepRunner bdbaScanStepRunner = createBdbaScanStepRunner(operationRunner);
        bdbaScanStepRunner.runBdbaScan(projectNameVersion, blackDuckRunData, scanFile, scanId.toString(), scanType);

        return scanId;
    }
    
    private UUID createFallbackScanId(OperationRunner operationRunner, String scanType, NameVersion projectNameVersion, String codeLocationName, long fileLength, BlackDuckRunData blackDuckRunData) throws IntegrationException {
        String projectGroupName = operationRunner.calculateProjectGroupOptions().getProjectGroup();

        DetectProtobufBdioHeaderUtil detectProtobufBdioHeaderUtil = new DetectProtobufBdioHeaderUtil(
            UUID.randomUUID().toString(),
            scanType,
            projectNameVersion,
            projectGroupName,
            codeLocationName,
            fileLength);
        
        File bdioHeaderFile;

        try {
            bdioHeaderFile = detectProtobufBdioHeaderUtil.createProtobufBdioHeader(getOutputDirectory(operationRunner, scanType));
        } catch (IOException e) {
            throw new IntegrationException("Unable to create new scan. Ensure the file and output directory are accessible.");
        }
        
        return operationRunner.initiatePreScassScan(blackDuckRunData, bdioHeaderFile);
    }

    private File getOutputDirectory(OperationRunner operationRunner, String scanType) throws IntegrationException {
        switch (scanType) {
            case PACKAGE_MANAGER:
                return operationRunner.getDirectoryManager().getBdioOutputDirectory();
            case BINARY:
                return operationRunner.getDirectoryManager().getBinaryOutputDirectory();
            case CONTAINER:
                return operationRunner.getDirectoryManager().getContainerOutputDirectory();
            default:
                throw new IntegrationException("Unexpected scan type:" + scanType);
        }
    }
    
    private String createCodeLocationName(Optional<File> scanFile, NameVersion projectNameVersion, String scanType, CodeLocationNameManager codeLocationNameManager) throws IntegrationException {
        switch (scanType) {
        case BINARY:
            return codeLocationNameManager.createBinaryScanCodeLocationName(scanFile.get(), projectNameVersion.getName(), projectNameVersion.getVersion());
        case CONTAINER:
            return codeLocationNameManager.createContainerScanCodeLocationName(scanFile.get(), projectNameVersion.getName(), projectNameVersion.getVersion());
        default:
            throw new IntegrationException("Unexpected scan type:" + scanType);
        }
    }

    public ScassScanStepRunner createScassScanStepRunner(BlackDuckRunData blackDuckRunData) {
        return new ScassScanStepRunner(blackDuckRunData);
    }
    
    public BdbaScanStepRunner createBdbaScanStepRunner(OperationRunner operationRunner) {
        return new BdbaScanStepRunner(operationRunner);
    }
}
