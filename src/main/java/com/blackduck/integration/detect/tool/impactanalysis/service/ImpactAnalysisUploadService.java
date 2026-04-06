package com.blackduck.integration.detect.tool.impactanalysis.service;

import java.util.Set;

import com.blackduck.integration.blackduck.api.core.BlackDuckPath;
import com.blackduck.integration.blackduck.api.manual.response.BlackDuckResponseResponse;
import com.blackduck.integration.blackduck.codelocation.CodeLocationCreationData;
import com.blackduck.integration.blackduck.codelocation.CodeLocationCreationService;
import com.blackduck.integration.blackduck.service.BlackDuckServicesFactory;
import com.blackduck.integration.blackduck.service.model.NotificationTaskRange;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;
import com.blackduck.integration.util.NoThreadExecutorService;

public class ImpactAnalysisUploadService {
    public static final BlackDuckPath<BlackDuckResponseResponse> IMPACT_ANALYSIS_PATH = new BlackDuckPath<>(
        "/api/scans/vulnerability-impact",
        BlackDuckResponseResponse.class,
        false
    );

    private final ImpactAnalysisBatchRunner impactAnalysisBatchRunner;
    private final CodeLocationCreationService codeLocationCreationService;

    // TODO: Move to BlackDuckServicesFactory in blackduck-common
    public static ImpactAnalysisUploadService create(BlackDuckServicesFactory blackDuckServicesFactory) {
        ImpactAnalysisBatchRunner impactAnalysisBatchRunner = new ImpactAnalysisBatchRunner(
            blackDuckServicesFactory.getLogger(),
            blackDuckServicesFactory.getBlackDuckApiClient(),
            blackDuckServicesFactory.getApiDiscovery(),
            new NoThreadExecutorService(),
            blackDuckServicesFactory.getGson()
        );
        return new ImpactAnalysisUploadService(impactAnalysisBatchRunner, blackDuckServicesFactory.createCodeLocationCreationService());
    }

    public ImpactAnalysisUploadService(ImpactAnalysisBatchRunner impactAnalysisBatchRunner, CodeLocationCreationService codeLocationCreationService) {
        this.impactAnalysisBatchRunner = impactAnalysisBatchRunner;
        this.codeLocationCreationService = codeLocationCreationService;
    }

    public ImpactAnalysisCodeLocationCreationRequest createUploadRequest(ImpactAnalysisBatch impactAnalysisBatch) {
        return new ImpactAnalysisCodeLocationCreationRequest(impactAnalysisBatchRunner, impactAnalysisBatch);
    }

    public CodeLocationCreationData<ImpactAnalysisBatchOutput> uploadImpactAnalysis(ImpactAnalysisCodeLocationCreationRequest uploadRequest) throws IntegrationException {
        return codeLocationCreationService.createCodeLocations(uploadRequest);
    }

    public CodeLocationCreationData<ImpactAnalysisBatchOutput> uploadImpactAnalysis(ImpactAnalysisBatch impactAnalysisBatch) throws IntegrationException {
        ImpactAnalysisCodeLocationCreationRequest uploadRequest = createUploadRequest(impactAnalysisBatch);
        return codeLocationCreationService.createCodeLocationsWithoutNotificationTaskRange(uploadRequest);
    }

    public CodeLocationCreationData<ImpactAnalysisBatchOutput> uploadImpactAnalysis(ImpactAnalysis impactAnalysis) throws IntegrationException {
        ImpactAnalysisBatch impactAnalysisBatch = new ImpactAnalysisBatch(impactAnalysis);
        return uploadImpactAnalysis(impactAnalysisBatch);
    }

    public ImpactAnalysisBatchOutput uploadImpactAnalysisAndWait(ImpactAnalysisCodeLocationCreationRequest uploadRequest, long timeoutInSeconds)
        throws InterruptedException, IntegrationException {
        return codeLocationCreationService.createCodeLocationsAndWait(uploadRequest, timeoutInSeconds);
    }

    public ImpactAnalysisBatchOutput uploadImpactAnalysisAndWait(ImpactAnalysisBatch impactAnalysisBatch, long timeoutInSeconds) throws InterruptedException, IntegrationException {
        ImpactAnalysisCodeLocationCreationRequest uploadRequest = createUploadRequest(impactAnalysisBatch);
        return uploadImpactAnalysisAndWait(uploadRequest, timeoutInSeconds);
    }

    public ImpactAnalysisBatchOutput uploadImpactAnalysisAndWait(ImpactAnalysis impactAnalysis, long timeoutInSeconds) throws InterruptedException, IntegrationException {
        ImpactAnalysisBatch impactAnalysisBatch = new ImpactAnalysisBatch(impactAnalysis);
        return uploadImpactAnalysisAndWait(impactAnalysisBatch, timeoutInSeconds);
    }

    public void waitForImpactAnalysisUpload(
        NotificationTaskRange notificationTaskRange,
        NameVersion projectAndVersion,
        Set<String> codeLocationNames,
        int expectedNotificationCount,
        long timeoutInSeconds
    )
        throws InterruptedException, IntegrationException {
        codeLocationCreationService.waitForCodeLocations(notificationTaskRange, projectAndVersion, codeLocationNames, expectedNotificationCount, timeoutInSeconds);
    }
}
