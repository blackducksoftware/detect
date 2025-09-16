package com.blackduck.integration.detect.tool.impactanalysis.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.http.HttpHeaders;

import com.google.gson.Gson;
import com.blackduck.integration.blackduck.api.generated.discovery.ApiDiscovery;
import com.blackduck.integration.blackduck.exception.BlackDuckApiException;
import com.blackduck.integration.blackduck.http.BlackDuckRequestBuilder;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.blackduck.service.request.BlackDuckResponseRequest;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.util.NameVersion;

public class ImpactAnalysisCallable implements Callable<ImpactAnalysisOutput> {
    private final Gson gson;
    private final BlackDuckApiClient blackDuckApiClient;
    private final ApiDiscovery apiDiscovery;
    private final ImpactAnalysis impactAnalysis;
    private final NameVersion projectAndVersion;
    private final String codeLocationName;

    public ImpactAnalysisCallable(Gson gson, BlackDuckApiClient blackDuckApiClient, ApiDiscovery apiDiscovery, ImpactAnalysis impactAnalysis) {
        this.gson = gson;
        this.blackDuckApiClient = blackDuckApiClient;
        this.apiDiscovery = apiDiscovery;
        this.impactAnalysis = impactAnalysis;
        this.projectAndVersion = impactAnalysis.getProjectAndVersion();
        this.codeLocationName = impactAnalysis.getCodeLocationName();
    }

    @Override
    public ImpactAnalysisOutput call() {
        try {
            BlackDuckResponseRequest request = createRequest();
            try (Response response = blackDuckApiClient.execute(request)) {
                return ImpactAnalysisOutput.FROM_RESPONSE(gson, projectAndVersion, codeLocationName, response);
            }
        } catch (BlackDuckApiException apiException) {
            String errorMessage = String.format(
                "Failed to upload impact analysis file: %s; Black Duck SCA response: %s [Black Duck error code: %s]",
                impactAnalysis.getImpactAnalysisPath().toAbsolutePath(),
                apiException.getMessage(),
                apiException.getBlackDuckErrorCode()
            );
            return ImpactAnalysisOutput.FAILURE(
                projectAndVersion,
                codeLocationName,
                errorMessage,
                apiException,
                apiException.getBlackDuckErrorCode(),
                apiException.getMessage(),
                apiException.getOriginalIntegrationRestException().getHttpStatusCode(),
                apiException.getOriginalIntegrationRestException().getHttpResponseContent()
            );
        } catch (Exception e) {
            String errorMessage = String.format("Failed to upload impact analysis file: %s because %s", impactAnalysis.getImpactAnalysisPath().toAbsolutePath(), e.getMessage());
            return ImpactAnalysisOutput.FAILURE(projectAndVersion, codeLocationName, errorMessage, e, null, null, 0, null);
        }
    }

    private BlackDuckResponseRequest createRequest() {
        Map<String, File> fileMap = new HashMap<>();
        fileMap.put("file", impactAnalysis.getImpactAnalysisPath().toFile());

        HttpUrl url = apiDiscovery.getUrl(ImpactAnalysisUploadService.IMPACT_ANALYSIS_PATH);

        return new BlackDuckRequestBuilder()
            .postMultipart(fileMap, new HashMap<>())
            // ejk - at least against 2021.4.1, Black Duck won't handle
            // an Accept application/json, so we have to explicitly
            // accept anything
            // (IDETECT-2664 sorry for no link. Security.)
            .addHeader(HttpHeaders.ACCEPT, "*/*")
            .buildBlackDuckResponseRequest(url);
    }

}
