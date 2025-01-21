package com.blackduck.integration.detect.lifecycle.run.step;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.blackduck.integration.blackduck.version.BlackDuckVersion;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.ScanCreationResponse;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.operation.blackduck.ScassScanInitiationResult;
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
    private static final BlackDuckVersion MIN_SCASS_SCAN_VERSION = new BlackDuckVersion(2025, 1, 0);
    
    // Supported scan types
    public static final String BINARY = "BINARY";
    public static final String CONTAINER = "CONTAINER";

    public static boolean areScassScansPossible(Optional<BlackDuckVersion> blackDuckVersion) {
        return blackDuckVersion.isPresent() && blackDuckVersion.get().isAtLeast(MIN_SCASS_SCAN_VERSION);
    }
    
    public UUID performCommonScan(NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData,
            Optional<File> scanFile, OperationRunner operationRunner, Gson gson, String scanType) throws OperationException, IntegrationException {

        String codeLocationName = createCodeLocationName(scanFile, projectNameVersion, scanType, operationRunner.getCodeLocationNameManager());
        
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

        ScanCreationResponse scanCreationResponse = initResult.getScanCreationResponse();

        String scanId = scanCreationResponse.getScanId();
        String uploadUrl = scanCreationResponse.getUploadUrl();
        
        String operationName = String.format("%s Upload", 
                scanType.substring(0, 1).toUpperCase() + scanType.substring(1).toLowerCase());
        
        return operationRunner.getAuditLog().namedPublic(operationName, () -> {
            if (StringUtils.isNotEmpty(uploadUrl)) {
                // This is a SCASS capable server server and SCASS is enabled.
                ScassScanStepRunner scassScanStepRunner = createScassScanStepRunner(blackDuckRunData);
                scassScanStepRunner.runScassScan(Optional.of(initResult.getZipFile()), scanCreationResponse);
            } else {
                // This is a SCASS capable server server but SCASS is not enabled.
                BdbaScanStepRunner bdbaScanStepRunner = createBdbaScanStepRunner(operationRunner);

                bdbaScanStepRunner.runBdbaScan(projectNameVersion, blackDuckRunData, scanFile, scanId, scanType);
            }

            return UUID.fromString(scanId);
        });
    }
    
    private File getOutputDirectory(OperationRunner operationRunner, String scanType) throws IntegrationException {
        switch (scanType) {
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
