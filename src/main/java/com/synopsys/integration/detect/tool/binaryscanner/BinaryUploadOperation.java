package com.synopsys.integration.detect.tool.binaryscanner;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.synopsys.blackduck.upload.client.UploaderConfig;
import com.synopsys.blackduck.upload.client.model.BinaryScanRequestData;
import com.synopsys.blackduck.upload.client.uploaders.BinaryUploader;
import com.synopsys.blackduck.upload.client.uploaders.UploaderFactory;
import com.synopsys.blackduck.upload.rest.status.BinaryUploadStatus;
import com.synopsys.integration.detect.configuration.DetectUserFriendlyException;
import com.synopsys.integration.detect.configuration.enumeration.ExitCodeType;
import com.synopsys.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.synopsys.integration.detect.workflow.codelocation.CodeLocationNameManager;
import com.synopsys.integration.detect.workflow.status.Status;
import com.synopsys.integration.detect.workflow.status.StatusEventPublisher;
import com.synopsys.integration.detect.workflow.status.StatusType;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.NameVersion;

public class BinaryUploadOperation {
    private static final String STATUS_KEY = "BINARY_SCAN";
    private static final int MULTIUPLOAD_CHUNK_SIZE = 5242880; // 5 MB chunks specified in bytes
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final StatusEventPublisher statusEventPublisher;

    public BinaryUploadOperation(StatusEventPublisher statusEventPublisher) {
        this.statusEventPublisher = statusEventPublisher;
    }

    public BinaryUploadStatus uploadBinaryScanFile(
        File binaryScanFile,
        NameVersion projectNameVersion, 
        CodeLocationNameManager codeLocationNameManager, 
        BlackDuckRunData blackDuckRunData
    )
        throws DetectUserFriendlyException {
        
        try {
            BinaryUploader binaryUploader = 
                    createMultipartBinaryScanUploader(binaryScanFile, projectNameVersion, blackDuckRunData, codeLocationNameManager);
            
            logger.info("Preparing to upload binary scan file: " + binaryScanFile.getAbsolutePath());       

            BinaryUploadStatus status = binaryUploader.upload(binaryScanFile.toPath());
            
            if (status.isError()) {
                handleUploadError(status);
            }
                    
            logger.info("Successfully uploaded binary scan file: " + binaryScanFile.getAbsolutePath());
            statusEventPublisher.publishStatusSummary(new Status(STATUS_KEY, StatusType.SUCCESS));
            return status;
        } catch (IntegrationException | IOException e) {
            statusEventPublisher.publishStatusSummary(new Status(STATUS_KEY, StatusType.FAILURE));
            throw new DetectUserFriendlyException("Failed to upload binary scan file.", e, ExitCodeType.FAILURE_BLACKDUCK_FEATURE_ERROR);
        }
    }

    private void handleUploadError(BinaryUploadStatus status) throws DetectUserFriendlyException {
        statusEventPublisher.publishStatusSummary(new Status(STATUS_KEY, StatusType.FAILURE));
        
        IntegrationException exception = null;
        
        if (status.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            // This is a special case until we support only 2024.7 and later BlackDuck's that have
            // the multipart upload endpoint.
            exception = new IntegrationException("Submitted file exceeds the file size upload limit.");
        } else if (status.getException().isPresent()) {
            exception = status.getException().get();      
        }
        
        throw new DetectUserFriendlyException("Failed to upload binary scan file.", exception, ExitCodeType.FAILURE_BLACKDUCK_FEATURE_ERROR);
    }
    
    private BinaryUploader createMultipartBinaryScanUploader(File binaryUpload, NameVersion projectNameVersion,
            BlackDuckRunData blackDuckRunData, CodeLocationNameManager codeLocationNameManager) throws IntegrationException {
        String codeLocationName = codeLocationNameManager.createBinaryScanCodeLocationName(binaryUpload,
                projectNameVersion.getName(), projectNameVersion.getVersion());

        UploaderConfig.Builder uploaderConfigBuilder = UploaderConfig.createConfigFromProperties(
                blackDuckRunData.getBlackDuckServerConfig().getProxyInfo(), new Properties())
                .setUploadChunkSize(MULTIUPLOAD_CHUNK_SIZE)
                .setTimeoutInSeconds(blackDuckRunData.getBlackDuckServerConfig().getTimeout())
                .setAlwaysTrustServerCertificate(blackDuckRunData.getBlackDuckServerConfig().isAlwaysTrustServerCertificate())
                .setBlackDuckUrl(blackDuckRunData.getBlackDuckServerConfig().getBlackDuckUrl())
                .setApiToken(blackDuckRunData.getBlackDuckServerConfig().getApiToken().get());

        UploaderConfig uploaderConfig = uploaderConfigBuilder.build();
        UploaderFactory uploadFactory = new UploaderFactory(uploaderConfig, new Slf4jIntLogger(logger), new Gson());

        BinaryScanRequestData binaryData = new BinaryScanRequestData(projectNameVersion.getName(),
                projectNameVersion.getVersion(), codeLocationName, "");

        return uploadFactory.createBinaryUploader("/api/uploads", binaryData);
    }
}