package com.blackduck.integration.detect.workflow.blackduck.developer.blackduck;

import com.blackduck.integration.blackduck.api.core.response.UrlMultipleResponses;
import com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView;
import com.blackduck.integration.blackduck.http.BlackDuckRequestBuilder;
import com.blackduck.integration.blackduck.service.request.BlackDuckMultipleRequest;
import com.blackduck.integration.blackduck.service.request.BlackDuckResponseRequest;
import com.blackduck.integration.rest.HttpUrl;

public class DetectRapidScanRequestBuilder {
    public static final String CURRENT_MEDIA_TYPE = "application/vnd.blackducksoftware.scan-5+json";
    private final BlackDuckRequestBuilder blackDuckRequestBuilder;

    public DetectRapidScanRequestBuilder() {
        this.blackDuckRequestBuilder = new BlackDuckRequestBuilder()
            .commonGet()
            .acceptMimeType(CURRENT_MEDIA_TYPE);
    }

    public BlackDuckResponseRequest createResponseRequest(HttpUrl httpUrl) {
        return blackDuckRequestBuilder.buildBlackDuckResponseRequest(httpUrl);
    }

    public BlackDuckMultipleRequest<DeveloperScansScanView> createRegularRequest(HttpUrl httpUrl) {
        return blackDuckRequestBuilder.buildBlackDuckRequest(new UrlMultipleResponses<>(httpUrl, DeveloperScansScanView.class));
    }
    public BlackDuckMultipleRequest<DeveloperScansScanView> createFullRequest(HttpUrl httpUrl) {
        return blackDuckRequestBuilder.buildBlackDuckRequest(new UrlMultipleResponses<>(httpUrl, DeveloperScansScanView.class));
    }

}