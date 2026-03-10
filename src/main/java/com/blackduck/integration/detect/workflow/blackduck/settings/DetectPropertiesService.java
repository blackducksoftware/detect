package com.blackduck.integration.detect.workflow.blackduck.settings;

import java.io.IOException;

import com.blackduck.integration.blackduck.api.core.BlackDuckPath;
import com.blackduck.integration.blackduck.api.core.response.UrlSingleResponse;
import com.blackduck.integration.blackduck.api.generated.discovery.ApiDiscovery;
import com.blackduck.integration.blackduck.http.BlackDuckRequestBuilder;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.blackduck.service.request.BlackDuckSingleRequest;
import com.blackduck.integration.exception.IntegrationException;

public class DetectPropertiesService {
    private static final BlackDuckPath<DetectPropertiesSetting> DETECT_PROPERTIES_PATH = new BlackDuckPath<>(
        "/api/settings/detect/properties",
        DetectPropertiesSetting.class,
        false
    );
    private static final String MIME_TYPE = "application/vnd.blackducksoftware.detect-setting-1+json";

    public DetectPropertiesSetting fetchDetectProperties(ApiDiscovery apiDiscovery, BlackDuckApiClient blackDuckApiClient) throws IntegrationException, IOException {
        UrlSingleResponse<DetectPropertiesSetting> urlResponse = apiDiscovery.metaSingleResponse(DETECT_PROPERTIES_PATH);

        BlackDuckSingleRequest<DetectPropertiesSetting> spec = new BlackDuckRequestBuilder()
            .commonGet()
            .acceptMimeType(MIME_TYPE)
            .buildBlackDuckRequest(urlResponse);

        return blackDuckApiClient.getResponse(spec);
    }
}
