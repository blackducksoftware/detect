package com.blackduck.integration.detect.workflow.blackduck.bdio;

import com.blackduck.integration.blackduck.codelocation.CodeLocationCreationData;
import com.blackduck.integration.blackduck.codelocation.intelligentpersistence.IntelligentPersistenceService;
import com.blackduck.integration.blackduck.codelocation.upload.UploadBatch;
import com.blackduck.integration.blackduck.codelocation.upload.UploadBatchOutput;
import com.blackduck.integration.detect.Application;
import com.blackduck.integration.exception.IntegrationException;

public class IntelligentPersistentUploadOperation extends BdioUploadOperation {
    private final IntelligentPersistenceService intelligentPersistenceService;
    private final Long timeout;

    public IntelligentPersistentUploadOperation(IntelligentPersistenceService intelligentPersistenceService, Long timeout) {
        this.intelligentPersistenceService = intelligentPersistenceService;
        this.timeout = timeout;
    }

    @Override
    protected CodeLocationCreationData<UploadBatchOutput> executeUpload(UploadBatch uploadBatch) throws IntegrationException {
        return intelligentPersistenceService.uploadBdioWithoutNotificationsQuery(uploadBatch, timeout, Application.START_TIME);
    }
}
