package com.blackduck.integration.detect.tool.impactanalysis.service;

import com.blackduck.integration.blackduck.codelocation.CodeLocationCreationRequest;
import com.blackduck.integration.blackduck.exception.BlackDuckIntegrationException;

public class ImpactAnalysisCodeLocationCreationRequest extends CodeLocationCreationRequest<ImpactAnalysisBatchOutput> {
    private final ImpactAnalysisBatchRunner impactAnalysisBatchRunner;
    private final ImpactAnalysisBatch impactAnalysisBatch;

    public ImpactAnalysisCodeLocationCreationRequest(ImpactAnalysisBatchRunner impactAnalysisBatchRunner, ImpactAnalysisBatch impactAnalysisBatch) {
        this.impactAnalysisBatchRunner = impactAnalysisBatchRunner;
        this.impactAnalysisBatch = impactAnalysisBatch;
    }

    @Override
    public ImpactAnalysisBatchOutput executeRequest() throws BlackDuckIntegrationException {
        return impactAnalysisBatchRunner.executeUploads(impactAnalysisBatch);
    }

}
