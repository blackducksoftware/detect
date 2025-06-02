package com.blackduck.integration.detect.workflow.blackduck.bdio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.codelocation.CodeLocationCreationData;
import com.blackduck.integration.blackduck.codelocation.Result;
import com.blackduck.integration.blackduck.codelocation.upload.UploadBatch;
import com.blackduck.integration.blackduck.codelocation.upload.UploadBatchOutput;
import com.blackduck.integration.blackduck.codelocation.upload.UploadOutput;
import com.blackduck.integration.blackduck.codelocation.upload.UploadTarget;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.run.operation.blackduck.BdioUploadResult;
import com.blackduck.integration.detect.workflow.bdio.BdioResult;
import com.blackduck.integration.exception.IntegrationException;

public abstract class BdioUploadOperation { //TODO: Could use Functional Interface.
    private final Logger logger = LoggerFactory.getLogger(BdioUploadOperation.class);

    public BdioUploadResult uploadBdioFiles(BdioResult bdioResult, String scanId) throws DetectUserFriendlyException {
        UploadBatch uploadBatch = createBatch(bdioResult, scanId);
        CodeLocationCreationData<UploadBatchOutput> response;
        try {
            response = executeUpload(uploadBatch);
        } catch (IntegrationException ex) {
            logger.error("Error uploading bdio files", ex);
            throw new DetectUserFriendlyException("Error uploading bdio files", ex, ExitCodeType.FAILURE_BLACKDUCK_FEATURE_ERROR);
        }
        checkForUploadFailure(response);
        return new BdioUploadResult(response);
    }

    protected abstract CodeLocationCreationData<UploadBatchOutput> executeUpload(UploadBatch uploadBatch) throws IntegrationException;

    private UploadBatch createBatch(BdioResult bdioResult, String scanId) {
        UploadBatch uploadBatch = new UploadBatch();
        for (UploadTarget uploadTarget : bdioResult.getUploadTargets()) {
            logger.debug(String.format("Uploading %s", uploadTarget.getUploadFile().getName()));
            uploadTarget.setScanId(scanId);
            uploadBatch.addUploadTarget(uploadTarget);
        }
        return uploadBatch;
    }

    private void checkForUploadFailure(CodeLocationCreationData<UploadBatchOutput> response) throws DetectUserFriendlyException {
        for (UploadOutput uploadOutput : response.getOutput()) {
            if (uploadOutput.getResult() == Result.FAILURE) {
                logger.error(String.format("Failed to upload code location: %s", uploadOutput.getCodeLocationName()));
                logger.error(String.format("Reason: %s", uploadOutput.getErrorMessage().orElse("Unknown reason.")));
                throw new DetectUserFriendlyException(
                    "An error occurred uploading a bdio file.",
                    uploadOutput.getException().orElse(null),
                    ExitCodeType.FAILURE_BLACKDUCK_FEATURE_ERROR
                );
            }
        }
    }
}
