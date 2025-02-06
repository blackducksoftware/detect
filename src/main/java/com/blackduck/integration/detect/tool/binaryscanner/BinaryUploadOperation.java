package com.blackduck.integration.detect.tool.binaryscanner;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.codelocation.CodeLocationCreationData;
import com.blackduck.integration.blackduck.codelocation.Result;
import com.blackduck.integration.blackduck.codelocation.binaryscanner.BinaryScan;
import com.blackduck.integration.blackduck.codelocation.binaryscanner.BinaryScanBatch;
import com.blackduck.integration.blackduck.codelocation.binaryscanner.BinaryScanBatchOutput;
import com.blackduck.integration.blackduck.codelocation.binaryscanner.BinaryScanOutput;
import com.blackduck.integration.blackduck.codelocation.binaryscanner.BinaryScanUploadService;
import com.blackduck.integration.blackduck.exception.BlackDuckIntegrationException;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.step.utility.UploaderHelper;
import com.blackduck.integration.detect.workflow.codelocation.CodeLocationNameManager;
import com.blackduck.integration.detect.workflow.status.Status;
import com.blackduck.integration.detect.workflow.status.StatusEventPublisher;
import com.blackduck.integration.detect.workflow.status.StatusType;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.exception.IntegrationTimeoutException;
import com.blackduck.integration.sca.upload.client.model.BinaryScanRequestData;
import com.blackduck.integration.sca.upload.client.uploaders.BinaryUploader;
import com.blackduck.integration.sca.upload.client.uploaders.UploaderFactory;
import com.blackduck.integration.sca.upload.rest.status.BinaryUploadStatus;
import com.blackduck.integration.util.NameVersion;

public class BinaryUploadOperation {
    private static final String STATUS_KEY = "BINARY_SCAN";
    private static final String BINARY_UPLOAD_FAILURE_MESSAGE = "Failed to upload binary scan file.";
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
            throw new DetectUserFriendlyException(BINARY_UPLOAD_FAILURE_MESSAGE, e, ExitCodeType.FAILURE_BLACKDUCK_FEATURE_ERROR);
        }
    }

    private void handleUploadError(BinaryUploadStatus status) throws DetectUserFriendlyException {
        statusEventPublisher.publishStatusSummary(new Status(STATUS_KEY, StatusType.FAILURE));
        
        IntegrationException exception = null;
        if (status.getException().isPresent()) {
            exception = status.getException().get();      
        }
        
        ExitCodeType type = ExitCodeType.FAILURE_BLACKDUCK_FEATURE_ERROR;
        if (exception instanceof IntegrationTimeoutException) {
            type = ExitCodeType.FAILURE_TIMEOUT;
        }
        
        throw new DetectUserFriendlyException(BINARY_UPLOAD_FAILURE_MESSAGE, exception, type);
    }
    
    private BinaryUploader createMultipartBinaryScanUploader(File binaryUpload, NameVersion projectNameVersion,
            BlackDuckRunData blackDuckRunData, CodeLocationNameManager codeLocationNameManager) throws IntegrationException {
        String codeLocationName = codeLocationNameManager.createBinaryScanCodeLocationName(binaryUpload,
                projectNameVersion.getName(), projectNameVersion.getVersion());

        UploaderFactory uploadFactory = UploaderHelper.getUploaderFactory(blackDuckRunData);

        BinaryScanRequestData binaryData = new BinaryScanRequestData(projectNameVersion.getName(),
                projectNameVersion.getVersion(), codeLocationName, "");

        return uploadFactory.createBinaryUploader("/api/uploads", binaryData);
    }
    
    public CodeLocationCreationData<BinaryScanBatchOutput> uploadLegacyBinaryScanFile(
            File binaryScanFile,
            BinaryScanUploadService binaryScanUploadService,
            CodeLocationNameManager codeLocationNameManager,
            NameVersion projectNameVersion
        )
            throws DetectUserFriendlyException {
            String codeLocationName = codeLocationNameManager.createBinaryScanCodeLocationName(
                binaryScanFile,
                projectNameVersion.getName(),
                projectNameVersion.getVersion()
            );
            try {
                logger.info("Preparing to upload binary scan file: " + binaryScanFile.getAbsolutePath());
                BinaryScan binaryScan = new BinaryScan(binaryScanFile, projectNameVersion.getName(), projectNameVersion.getVersion(), codeLocationName);
                BinaryScanBatch binaryScanBatch = new BinaryScanBatch(binaryScan);
                CodeLocationCreationData<BinaryScanBatchOutput> codeLocationCreationData = binaryScanUploadService.uploadBinaryScan(binaryScanBatch);

                BinaryScanBatchOutput binaryScanBatchOutput = codeLocationCreationData.getOutput();
                // The throwExceptionForError() in BinaryScanBatchOutput has a bug, so doing that work here
                throwExceptionForError(binaryScanBatchOutput);

                logger.info("Successfully uploaded binary scan file: " + binaryScanFile.getAbsolutePath());
                statusEventPublisher.publishStatusSummary(new Status(STATUS_KEY, StatusType.SUCCESS));
                return codeLocationCreationData;
            } catch (IntegrationException e) {
                statusEventPublisher.publishStatusSummary(new Status(STATUS_KEY, StatusType.FAILURE));
                throw new DetectUserFriendlyException(BINARY_UPLOAD_FAILURE_MESSAGE, e, ExitCodeType.FAILURE_BLACKDUCK_FEATURE_ERROR);
            }
        }

        public CodeLocationCreationData<BinaryScanBatchOutput> uploadBinaryScanFiles(
            BinaryScanBatch binaryScanBatch,
            BinaryScanUploadService binaryScanUploadService
        )
            throws DetectUserFriendlyException {
            try {
                CodeLocationCreationData<BinaryScanBatchOutput> codeLocationCreationData = binaryScanUploadService.uploadBinaryScan(binaryScanBatch);

                BinaryScanBatchOutput binaryScanBatchOutput = codeLocationCreationData.getOutput();
                // The throwExceptionForError() in BinaryScanBatchOutput has a bug, so doing that work here
                throwExceptionForError(binaryScanBatchOutput);
                statusEventPublisher.publishStatusSummary(new Status(STATUS_KEY, StatusType.SUCCESS));
                return codeLocationCreationData;
            } catch (IntegrationException e) {
                statusEventPublisher.publishStatusSummary(new Status(STATUS_KEY, StatusType.FAILURE));
                throw new DetectUserFriendlyException(BINARY_UPLOAD_FAILURE_MESSAGE, e, ExitCodeType.FAILURE_BLACKDUCK_FEATURE_ERROR);
            }
        }

        // BinaryScanBatchOutput used to do this, but our understanding of what needs to happen has been
        // changing rapidly. Once we're confident we know what it should do, it should presumably move back there.
        private void throwExceptionForError(BinaryScanBatchOutput binaryScanBatchOutput) throws BlackDuckIntegrationException {
            for (BinaryScanOutput binaryScanOutput : binaryScanBatchOutput) {
                if (binaryScanOutput.getResult() == Result.FAILURE) {
                    // Black Duck responses are single-line message (loggable as-is), but nginx Bad Gateway responses are
                    // multi-line html with the message embedded (that mess up the log).
                    // cleanResponse() attempts to produce something reasonable to log in either case
                    String cleanedBlackDuckResponse = cleanResponse(binaryScanOutput.getResponse());
                    String uploadErrorMessage = String.format(
                        "Error when uploading binary scan: %s (Black Duck response: %s)",
                        binaryScanOutput.getErrorMessage().orElse(binaryScanOutput.getStatusMessage()),
                        cleanedBlackDuckResponse
                    );
                    logger.error(uploadErrorMessage);
                    throw new BlackDuckIntegrationException(uploadErrorMessage);
                }
            }
        }

        private String cleanResponse(String response) {
            if (StringUtils.isBlank(response)) {
                return "";
            }
            String lengthLimitedResponse = StringUtils.substring(response, 0, 200);
            if (!lengthLimitedResponse.equals(response)) {
                lengthLimitedResponse = lengthLimitedResponse + "...";
            }
            String[] searchList = { "\n", "\r" };
            String[] replacementList = { " ", "" };
            return StringUtils.replaceEachRepeatedly(lengthLimitedResponse, searchList, replacementList);
        }
}