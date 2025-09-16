package com.blackduck.integration.detect.workflow.blackduck.integratedmatching;

import org.apache.http.entity.ContentType;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.blackduck.integration.blackduck.api.core.BlackDuckPath;
import com.blackduck.integration.blackduck.api.core.BlackDuckResponse;
import com.blackduck.integration.blackduck.api.generated.discovery.ApiDiscovery;
import com.blackduck.integration.blackduck.http.BlackDuckRequestBuilder;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.blackduck.service.DataService;
import com.blackduck.integration.blackduck.service.request.BlackDuckResponseRequest;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.model.ScanCountsPayload;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.log.IntLogger;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.response.Response;

public class CorrelatedScanCountUploadService extends DataService {
    // TODO: This feature is being developed before the Black Duck endpoint is released, so this code
    // can't adhere to the standard pattern yet.
    // Once this endpoint/payload is added to the Black Duck swagger doc, and blackduck-common-api is
    // regenerated from it, this service can move to blackduck-common, and these details can be sourced
    // from blackduck-common-api, as with other services/endpoints.
    private static final String CORRELATED_SCAN_COUNT_ENDPOINT_FORMAT_STRING = "/api/scan-correlations/%s/counts";
    private static final String CONTENT_TYPE_HEADER_KEY = "Content-Type";
    private static final String CONTENT_TYPE_HEADER_VALUE = "application/vnd.blackducksoftware.scan-5+json";
    private final Gson gson;

    public CorrelatedScanCountUploadService(
        Gson gson,
        BlackDuckApiClient blackDuckApiClient,
        ApiDiscovery apiDiscovery,
        IntLogger logger
    ) {
        super(blackDuckApiClient, apiDiscovery, logger);
        this.gson = gson;
    }

    public Response uploadCorrelatedScanCounts(String correlationId, ScanCountsPayload scanCountsPayload) throws IntegrationException {
        HttpUrl url = buildEndpointUrl(correlationId);

        logger.info(String.format("Uploading scanCountsPayload: correlationId: %s; scanCountsPayload: %s",
            correlationId, scanCountsPayload));
        JsonElement scanCountsElement = gson.toJsonTree(scanCountsPayload);

        BlackDuckResponseRequest request = new BlackDuckRequestBuilder()
            .addHeader(CONTENT_TYPE_HEADER_KEY, CONTENT_TYPE_HEADER_VALUE)
            .postObject(scanCountsElement, ContentType.APPLICATION_JSON)
            .buildBlackDuckResponseRequest(url);

        Response response = blackDuckApiClient.execute(request);
        logger.debug(String.format("uploadCorrelatedScanCounts(): Black Duck SCA response status: %d", response.getStatusCode()));
        return response;
    }

    private HttpUrl buildEndpointUrl(final String correlationId) {
        String urlString = String.format(CORRELATED_SCAN_COUNT_ENDPOINT_FORMAT_STRING, correlationId);
        BlackDuckPath<BlackDuckResponse> blackDuckPath = new BlackDuckPath<>(urlString, BlackDuckResponse.class, false);
        return apiDiscovery.metaSingleResponse(blackDuckPath).getUrl();
    }
}
