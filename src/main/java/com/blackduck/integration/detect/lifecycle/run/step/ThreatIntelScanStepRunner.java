package com.blackduck.integration.detect.lifecycle.run.step;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.bdio.proto.domain.ScanType;
import com.blackduck.integration.blackduck.version.BlackDuckVersion;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.util.bdio.protobuf.DetectProtobufBdioHeaderUtil;
import com.blackduck.integration.detect.workflow.codelocation.CodeLocationNameManager;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.util.NameVersion;

@Deprecated
public class ThreatIntelScanStepRunner {
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final NameVersion projectNameVersion;
    private final String projectGroupName;
    private UUID scanId;
    private final OperationRunner operationRunner;
    private final File threatIntelRunDirectory;
    private final BlackDuckRunData blackDuckRunData;
    private String codeLocationName;
    private static final String STORAGE_UPLOAD_ENDPOINT = "/api/storage/rldata/";
    private static final String STORAGE_RL_CONTENT_TYPE = "application/vnd.blackducksoftware.rl-data-1+octet-stream";
    private static final BlackDuckVersion MIN_BLACK_DUCK_VERSION = new BlackDuckVersion(2024, 4, 0);

    @Deprecated
    public ThreatIntelScanStepRunner(OperationRunner operationRunner, BlackDuckRunData blackDuckRunData, NameVersion projectNameVersion) {
        this.operationRunner = operationRunner;
        this.blackDuckRunData = blackDuckRunData;
        this.projectNameVersion = projectNameVersion;
        
        threatIntelRunDirectory = operationRunner.getDirectoryManager().getThreatIntelOutputDirectory();
        projectGroupName = operationRunner.calculateProjectGroupOptions().getProjectGroup();
    }

    @Deprecated
    public Optional<UUID> invokeThreatIntelWorkflow() {
        try {
            // insert deprecation msg?
            logger.debug("Determining if configuration is valid to run a Threat Intel scan.");
            if (!isThreatIntelEligible()) {
                return Optional.ofNullable(scanId);
            }
            if (!isBlackDuckVersionValid()) {
                String minBlackDuckVersion = String.join(".", 
                        Integer.toString(MIN_BLACK_DUCK_VERSION.getMajor()),
                        Integer.toString(MIN_BLACK_DUCK_VERSION.getMinor()),
                        Integer.toString(MIN_BLACK_DUCK_VERSION.getPatch()));
                throw new IntegrationException("Threat Intel scan is only supported with BlackDuck version "
                        + minBlackDuckVersion + " or greater. Threat Intel scan could not be run.");
            }

            codeLocationName = createCodeLocationName();
            File fileToUpload = new File(operationRunner.getThreatIntelScanFilePath().get());
            
            // Generate BDIO header and obtain scanID
            initiateScan(fileToUpload.length());
            
            logger.info("Theat Intel scan initiated. Uploading file to scan.");
            
            uploadFileToStorageService(fileToUpload);
            
            logger.info("Threat Intel file upload complete.");
            
            // publish success event
            operationRunner.publishThreatIntelSuccess();

        } catch (IntegrationException | IOException | OperationException e) {
            operationRunner.publishThreatIntelFailure(e);
            return Optional.empty();
        }

        return Optional.ofNullable(scanId);
    }

    @Deprecated
    public String getCodeLocationName() {
        return codeLocationName;
    }

    private void uploadFileToStorageService(File fileToUpload) throws IOException, OperationException, IntegrationException {
        String storageServiceEndpoint = String.join("", STORAGE_UPLOAD_ENDPOINT, scanId.toString());
        String operationName = "Upload Threat Intel File";
        logger.debug("Uploading Threat Intel file to storage endpoint: {}", storageServiceEndpoint);
        
        Map<String, String> headers = new HashMap<>();

        String encodedName = URLEncoder.encode(fileToUpload.getName(), "UTF-8");
        headers.put("X-Filename", encodedName);
        
        try (Response response = operationRunner.uploadFileToStorageServiceWithHeaders(
                blackDuckRunData,
                storageServiceEndpoint,
                fileToUpload, 
                STORAGE_RL_CONTENT_TYPE,
                operationName,
                headers)
            ) {
            if (response.isStatusCodeSuccess()) {
                logger.debug("Threat Intel file uploaded to storage service.");
            } else {
                logger.trace("Unable to upload Threat Intel file. {} {}", response.getStatusCode(), response.getStatusMessage());
                throw new IntegrationException(String.join(" ", "Unable to upload Threat Intel file. Response code:", String.valueOf(response.getStatusCode()), response.getStatusMessage()));
            }
        }
    }

    private void initiateScan(long fileSize) throws IOException, IntegrationException, OperationException {
        DetectProtobufBdioHeaderUtil detectProtobufBdioHeaderUtil = new DetectProtobufBdioHeaderUtil(
            UUID.randomUUID().toString(),
            ScanType.RL.name(),
            projectNameVersion,
            projectGroupName,
            codeLocationName,
            fileSize);
        File bdioHeaderFile = detectProtobufBdioHeaderUtil.createProtobufBdioHeader(threatIntelRunDirectory);
        String operationName = "Upload Threat Intel Scan BDIO Header to Initiate Scan";
        scanId = operationRunner.uploadBdioHeaderToInitiateScan(blackDuckRunData, bdioHeaderFile, operationName);
        if (scanId == null) {
            logger.warn("Scan ID was not found in the response from the server.");
            throw new IntegrationException("Scan ID was not found in the response from the server.");
        }
        String scanIdString = scanId.toString();
        logger.debug("Scan initiated with scan service. Scan ID received: {}", scanIdString);
    }

    private boolean isBlackDuckVersionValid() {
        Optional<BlackDuckVersion> blackDuckVersion = blackDuckRunData.getBlackDuckServerVersion();
        return blackDuckVersion.isPresent() && blackDuckVersion.get().isAtLeast(MIN_BLACK_DUCK_VERSION);
    }
    
    private boolean isThreatIntelEligible() throws IOException {
        if (!operationRunner.getThreatIntelScanFilePath().isPresent()) {
            logger.info("No detect.threatintel.scan.file.path property was provided. Skipping Threat Intel scan.");
            return false;
        }
        
        String scanFilePath = operationRunner.getThreatIntelScanFilePath().get();
        
        File scanFile = new File(scanFilePath);
        
        if (!Files.isReadable(scanFile.toPath())) {
            throw new IOException("Unable to access file: " + scanFilePath  + ". Please ensure the file exists and is readable by Detect.");
        }
    
        return true;
    }
    
    private String createCodeLocationName() {
        CodeLocationNameManager codeLocationNameManager = operationRunner.getCodeLocationNameManager();
        File targetFile = new File(operationRunner.getThreatIntelScanFilePath().get());
        return codeLocationNameManager.createThreatIntelScanCodeLocationName(targetFile, projectNameVersion.getName(), projectNameVersion.getVersion());
    }
}
