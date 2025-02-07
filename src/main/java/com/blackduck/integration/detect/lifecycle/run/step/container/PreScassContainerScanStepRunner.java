package com.blackduck.integration.detect.lifecycle.run.step.container;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import com.blackduck.integration.blackduck.version.BlackDuckVersion;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.step.CommonScanStepRunner;
import com.blackduck.integration.detect.lifecycle.run.step.utility.UploaderHelper;
import com.blackduck.integration.detect.util.bdio.protobuf.DetectProtobufBdioHeaderUtil;
import com.blackduck.integration.detect.workflow.codelocation.CodeLocationNameManager;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.client.uploaders.ContainerUploader;
import com.blackduck.integration.sca.upload.client.uploaders.UploaderFactory;
import com.blackduck.integration.sca.upload.rest.status.DefaultUploadStatus;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class PreScassContainerScanStepRunner extends AbstractContainerScanStepRunner {
    private final String projectGroupName;
    private final Long containerImageSizeInBytes;
    private UploaderFactory uploadFactory;
    private String codeLocationName;
    private static final BlackDuckVersion MIN_MULTIPART_UPLOAD_VERSION = new BlackDuckVersion(2024, 10, 0);
    private static final String STORAGE_CONTAINERS_ENDPOINT = "/api/storage/containers/";
    private static final String STORAGE_IMAGE_CONTENT_TYPE = "application/vnd.blackducksoftware.container-scan-data-1+octet-stream";
    private static final String STORAGE_IMAGE_METADATA_CONTENT_TYPE = "application/vnd.blackducksoftware.container-scan-message-1+json";

    public PreScassContainerScanStepRunner(OperationRunner operationRunner, NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData, Gson gson) throws IntegrationException, OperationException {
        super(operationRunner, projectNameVersion, blackDuckRunData, gson);
        projectGroupName = operationRunner.calculateProjectGroupOptions().getProjectGroup();
        containerImageSizeInBytes = containerImage != null && containerImage.exists() ? containerImage.length() : 0;
    }

    @Override
    public String getCodeLocationName() {
        if (codeLocationName == null) {
            codeLocationName = createContainerScanCodeLocationName();
        }
        return codeLocationName;
    }

    @Override
    protected UUID performBlackduckInteractions() throws IOException, IntegrationException, OperationException {
        UUID scanId = initiateScan();
        logger.info("Container scan initiated. Uploading container scan image.");

        if (canDoMultiPartUpload()) {
            multiPartUploadImage(scanId);
        } else {
            uploadImageToStorageService(scanId); 
        }
        
        uploadImageMetadataToStorageService(scanId);
        return scanId;
    }

    private String createContainerScanCodeLocationName() {
        if (!isContainerImageResolved()) {
            return null;
        }

        CodeLocationNameManager codeLocationNameManager = operationRunner.getCodeLocationNameManager();
        return codeLocationNameManager.createContainerScanCodeLocationName(containerImage, projectNameVersion.getName(), projectNameVersion.getVersion());
    }

    private UUID initiateScan() throws IOException, IntegrationException, OperationException {
        DetectProtobufBdioHeaderUtil detectProtobufBdioHeaderUtil = new DetectProtobufBdioHeaderUtil(
            UUID.randomUUID().toString(),
            SCAN_TYPE,
            projectNameVersion,
            projectGroupName,
            getCodeLocationName(),
            containerImageSizeInBytes);
        File bdioHeaderFile = detectProtobufBdioHeaderUtil.createProtobufBdioHeader(containerRunDirectory);
        String operationName = "Upload Container Scan BDIO Header to Initiate Scan";
        UUID scanId = operationRunner.uploadBdioHeaderToInitiateScan(blackDuckRunData, bdioHeaderFile, operationName);
        if (scanId == null) {
            logger.warn("Scan ID was not found in the response from the server.");
            throw new IntegrationException("Scan ID was not found in the response from the server.");
        }
        String scanIdString = scanId.toString();
        logger.debug("Scan initiated with scan service. Scan ID received: {}", scanIdString);
        return scanId;
    }

    private void uploadImageToStorageService(UUID scanId) throws IOException, IntegrationException, OperationException {
        String storageServiceEndpoint = String.join("", STORAGE_CONTAINERS_ENDPOINT, scanId.toString());
        String operationName = "Upload Container Scan Image";
        logger.debug("Uploading container image artifact to storage endpoint: {}", storageServiceEndpoint);

        try (Response response = operationRunner.uploadFileToStorageService(
            blackDuckRunData,
            storageServiceEndpoint,
            containerImage,
            STORAGE_IMAGE_CONTENT_TYPE,
            operationName
        )
        ) {
            if (response.isStatusCodeSuccess()) {
                logger.debug("Container scan image uploaded to storage service.");
            } else {
                logger.trace("Unable to upload container image. {} {}", response.getStatusCode(), response.getStatusMessage());
                throw new IntegrationException(String.join(" ", "Unable to upload container image. Response code:", String.valueOf(response.getStatusCode()), response.getStatusMessage()));
            }
        }
    }
    
    private DefaultUploadStatus multiPartUploadImage(UUID scanId) throws IntegrationException, IOException {
        String storageServiceEndpoint = String.join("", STORAGE_CONTAINERS_ENDPOINT, scanId.toString());
        ContainerUploader containerUploader = initAndOrGetContainerUploadFactory().createContainerUploader(storageServiceEndpoint);
        
        logger.debug("Performing multipart container image upload to storage endpoint: {}", storageServiceEndpoint);
        DefaultUploadStatus status = containerUploader.upload(containerImage.toPath());
            
        if (status == null || status.isError()) {
            UploaderHelper.handleUploadError(status);
        }
            
        logger.debug("Multipart container scan image uploaded to storage service.");
        return status;
    }

    private void uploadImageMetadataToStorageService(UUID scanId) throws IntegrationException, IOException, OperationException {
        String storageServiceEndpoint = String.join("", STORAGE_CONTAINERS_ENDPOINT, scanId.toString(), "/message");
        String operationName = "Upload Container Scan Image Metadata JSON";
        logger.debug("Uploading container image metadata to storage endpoint: {}", storageServiceEndpoint);

        JsonObject imageMetadataObject = operationRunner.createScanMetadata(scanId, projectNameVersion, CommonScanStepRunner.CONTAINER);

        try (Response response = operationRunner.uploadJsonToStorageService(
            blackDuckRunData,
            storageServiceEndpoint,
            imageMetadataObject.toString(),
            STORAGE_IMAGE_METADATA_CONTENT_TYPE,
            operationName
        )
        ) {
            if (response.isStatusCodeSuccess()) {
                logger.debug("Container scan image metadata uploaded to storage service.");
            } else {
                logger.trace("Unable to upload container image metadata." + response.getStatusCode() + " " + response.getStatusMessage());
                throw new IntegrationException(String.join(" ", "Unable to upload container image metadata. Response code:", String.valueOf(response.getStatusCode()), response.getStatusMessage()));
            }
        }
    }

    private UploaderFactory initAndOrGetContainerUploadFactory() throws IntegrationException {
        if (uploadFactory == null) {
            uploadFactory = UploaderHelper.getUploaderFactory(blackDuckRunData);
        }
        return uploadFactory;
    }
    
    private boolean canDoMultiPartUpload() {
        Optional<BlackDuckVersion> blackDuckVersion = blackDuckRunData.getBlackDuckServerVersion();
        return blackDuckVersion.isPresent() && blackDuckVersion.get().isAtLeast(MIN_MULTIPART_UPLOAD_VERSION);
    }
}
