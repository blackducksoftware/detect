package com.blackduck.integration.detect.workflow.blackduck.developer.blackduck;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;

import com.blackduck.integration.blackduck.api.core.BlackDuckPath;
import com.blackduck.integration.blackduck.api.generated.discovery.ApiDiscovery;
import com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView;
import com.blackduck.integration.blackduck.bdio2.model.BdioFileContent;
import com.blackduck.integration.blackduck.exception.BlackDuckIntegrationException;
import com.blackduck.integration.blackduck.http.BlackDuckRequestBuilder;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.blackduck.service.request.BlackDuckRequestBuilderEditor;
import com.blackduck.integration.blackduck.service.request.BlackDuckResponseRequest;
import com.blackduck.integration.detect.Application;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.log.IntLogger;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.response.Response;

public class RapidScanConfigBdio2StreamUploader {
    // IDETECT-2756
    public static final String PROJECT_NAME_HEADER = "X-BD-PROJECT-NAME";
    public static final String VERSION_NAME_HEADER = "X-BD-VERSION-NAME";

    public static final String HEADER_CONTENT_TYPE = "Content-type";
    public static final String HEADER_X_BD_MODE = "X-BD-MODE";
    public static final String HEADER_X_BD_DOCUMENT_COUNT = "X-BD-DOCUMENT-COUNT";

    private final BlackDuckApiClient blackDuckApiClient;
    private final ApiDiscovery apiDiscovery;
    private final IntLogger logger;
    private final BlackDuckPath<DeveloperScansScanView> scanPath;
    private final String contentType;

    public RapidScanConfigBdio2StreamUploader(
        BlackDuckApiClient blackDuckApiClient,
        ApiDiscovery apiDiscovery,
        IntLogger logger,
        BlackDuckPath<DeveloperScansScanView> scanPath,
        String contentType) {
        this.blackDuckApiClient = blackDuckApiClient;
        this.apiDiscovery = apiDiscovery;
        this.logger = logger;
        this.scanPath = scanPath;
        this.contentType = contentType;
    }

    public HttpUrl start(BdioFileContent header, BlackDuckRequestBuilderEditor editor, long detectTimeout) throws IntegrationException, InterruptedException {
        HttpUrl url = apiDiscovery.metaSingleResponse(scanPath).getUrl();
        BlackDuckResponseRequest request = new BlackDuckRequestBuilder()
            .postString(header.getContent(), ContentType.create(contentType, StandardCharsets.UTF_8))
            .addHeader(HEADER_CONTENT_TYPE, contentType)
            .apply(editor)
            .buildBlackDuckResponseRequest(url);
        Response response = recursiveExecute(request, 0L, 0, detectTimeout);
        String location = response.getHeaderValue("location");
        
        if (location == null) {
            throw new IntegrationException("Attempted to create a scan by uploading to " + url.toString()
                    + " but failed with " + response.getStatusCode() + " response code from the Black Duck SCA server.");
        }
        
        HttpUrl responseUrl = new HttpUrl(location);
        logger.debug(String.format("Starting upload to %s", responseUrl.toString()));
        return responseUrl;
    }
    
    public Response recursiveExecute(BlackDuckResponseRequest request, long waitInMillis, int backoffRetryCount, long detectTimeout) throws InterruptedException, IntegrationException {
        Thread.sleep(waitInMillis);
        Response response = blackDuckApiClient.executeAndRetrieveResponse(request);
        String retryAfterInSeconds = response.getHeaderValue("retry-after");
        if (OperationRunner.RETRYABLE_AFTER_WAIT_HTTP_EXCEPTIONS.contains(response.getStatusCode()) 
                && null != retryAfterInSeconds 
                && !retryAfterInSeconds.equals("0")) {
            // Response code is one of 408, 429, 502, 503, 504.
            long retryAfterInMillis = Long.parseLong(retryAfterInSeconds) * 1000;
            if (isDetectTimeoutExceededBy(retryAfterInMillis, detectTimeout)) {
                throw new BlackDuckIntegrationException("Detect timeout exceeded or will be exceeded due to server being busy.");
            }
            logger.debug("Received code " + response.getStatusCode() + ". Retrying upload in " + retryAfterInSeconds + " seconds.");
            return recursiveExecute(request, retryAfterInMillis, 0, detectTimeout);
        } else if (OperationRunner.RETRYABLE_WITH_BACKOFF_HTTP_EXCEPTIONS.contains(response.getStatusCode())) {
            // Response code is 425 or 500.
            long fibonacciWaitInMillis = (long) OperationRunner.calculateMaxWaitInSeconds(backoffRetryCount) * 1000;
            if (isDetectTimeoutExceededBy(fibonacciWaitInMillis, detectTimeout)) {
                throw new BlackDuckIntegrationException("Detect timeout exceeded or will be exceeded due to a temporary unavailability of the server.");
            }
            logger.debug("Received code " + response.getStatusCode() + ". Backing off and retrying upload in " + fibonacciWaitInMillis + " milliseconds.");
            backoffRetryCount++;
            return recursiveExecute(request, fibonacciWaitInMillis, backoffRetryCount, detectTimeout);
        }
        return response;
    }
    
    private boolean isDetectTimeoutExceededBy(long waitInMillis, long detectTimeout) {
        long startTime = Application.START_TIME;
        long currentTime = System.currentTimeMillis();
        return (currentTime - startTime + waitInMillis) > (detectTimeout * 1000);
    }

    public HttpUrl startWithConfig(File zippedConfigAndHeader, BlackDuckRequestBuilderEditor editor, long detectTimeout) throws IntegrationException, InterruptedException {
        HttpUrl url = apiDiscovery.metaSingleResponse(scanPath).getUrl();
        BlackDuckResponseRequest request = new BlackDuckRequestBuilder()
            .postFile(zippedConfigAndHeader, ContentType.create("application/zip"))
            .addHeader(HEADER_CONTENT_TYPE, "application/vnd.blackducksoftware.developer-scan-1-ld-2-yaml-1+zip")
            .apply(editor)
            .buildBlackDuckResponseRequest(url);
        Response response = recursiveExecute(request, 0L, 0, detectTimeout);
        HttpUrl responseUrl = new HttpUrl(response.getHeaderValue("location"));
        logger.debug(String.format("Starting upload to %s", responseUrl.toString()));
        return responseUrl;
    }

    public void append(HttpUrl url, int count, BdioFileContent bdioFileContent, BlackDuckRequestBuilderEditor editor) throws IntegrationException {
        logger.debug(String.format("Appending file %s, to %s with count %d", bdioFileContent.getFileName(), url.toString(), count));
        BlackDuckResponseRequest request = new BlackDuckRequestBuilder()
            .putString(bdioFileContent.getContent(), ContentType.create(contentType, StandardCharsets.UTF_8))
            .addHeader(HEADER_CONTENT_TYPE, contentType)
            .addHeader(HEADER_X_BD_MODE, "append")
            .addHeader(HEADER_X_BD_DOCUMENT_COUNT, String.valueOf(count))
            .apply(editor)
            .buildBlackDuckResponseRequest(url);
        blackDuckApiClient.execute(request);  // 202 accepted
    }

    public void finish(HttpUrl url, int count, BlackDuckRequestBuilderEditor editor) throws IntegrationException {
        logger.debug(String.format("Finishing upload to %s with count %d", url.toString(), count));
        BlackDuckResponseRequest request = new BlackDuckRequestBuilder()
            .putString(StringUtils.EMPTY, ContentType.create(contentType, StandardCharsets.UTF_8))
            .addHeader(HEADER_CONTENT_TYPE, contentType)
            .addHeader(HEADER_X_BD_MODE, "finish")
            .addHeader(HEADER_X_BD_DOCUMENT_COUNT, String.valueOf(count))
            .apply(editor)
            .buildBlackDuckResponseRequest(url);
        blackDuckApiClient.execute(request);
    }
}
