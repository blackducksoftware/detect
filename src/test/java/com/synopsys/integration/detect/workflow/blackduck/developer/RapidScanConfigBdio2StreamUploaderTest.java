package com.synopsys.integration.detect.workflow.blackduck.developer;

import java.nio.charset.StandardCharsets;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.core.BlackDuckPath;
import com.synopsys.integration.blackduck.api.generated.discovery.ApiDiscovery;
import com.synopsys.integration.blackduck.api.generated.view.DeveloperScansScanView;
import com.synopsys.integration.blackduck.bdio2.model.BdioFileContent;
import com.synopsys.integration.blackduck.http.BlackDuckRequestBuilder;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.blackduck.service.request.BlackDuckResponseRequest;
import com.synopsys.integration.detect.workflow.blackduck.developer.blackduck.RapidScanConfigBdio2StreamUploader;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.rest.response.Response;

public class RapidScanConfigBdio2StreamUploaderTest {
    @Test
    public void test429Retry() throws IntegrationException, InterruptedException {
        HttpUrl url = new HttpUrl("https://localhost/api/developer-scans");
        BdioFileContent header = new BdioFileContent("mock-header", "mock-content");
        String contentType = "application/vnd.blackducksoftware.developer-scan-1-ld-2+json";
        
        BlackDuckApiClient blackDuckApiClient = Mockito.mock(BlackDuckApiClient.class);
        ApiDiscovery apiDiscovery = Mockito.mock(ApiDiscovery.class);
        Response response = Mockito.mock(Response.class);
        
        IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));
        
        BlackDuckResponseRequest request = new BlackDuckRequestBuilder()
                .postString(header.getContent(), ContentType.create(contentType, StandardCharsets.UTF_8))
                .addHeader("Content-type", contentType)
                .buildBlackDuckResponseRequest(url);
        
        RapidScanConfigBdio2StreamUploader uploader = new RapidScanConfigBdio2StreamUploader(
                blackDuckApiClient,
                apiDiscovery,
                logger,
                new BlackDuckPath("/api/developer-scans", DeveloperScansScanView.class, false),
                contentType);
        
        Mockito.when(blackDuckApiClient.executeAndRetrieveResponse(request)).thenReturn(response);
        Mockito.when(response.getHeaderValue("retry-after"))
            .thenReturn("1")
            .thenReturn(null);
        
        Mockito.when(response.getStatusCode())
            .thenReturn(429)
            .thenReturn(200);
        
        uploader.recursiveExecute(request, 0, 0, 300);
        
        // Test that we made two calls, the 429 initial response, and the 200 success
        Mockito.verify(blackDuckApiClient, Mockito.times(2)).executeAndRetrieveResponse(request);
    }
}
