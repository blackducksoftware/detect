package com.blackduck.integration.detect.workflow.blackduck.developer.blackduck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.blackduck.integration.blackduck.api.core.BlackDuckResponse;
import org.apache.http.HttpStatus;

import com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView;
import com.blackduck.integration.blackduck.exception.BlackDuckIntegrationException;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.blackduck.service.request.BlackDuckMultipleRequest;
import com.blackduck.integration.blackduck.service.request.BlackDuckResponseRequest;
import com.blackduck.integration.detect.configuration.enumeration.BlackduckScanMode;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.exception.IntegrationTimeoutException;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.exception.IntegrationRestException;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.wait.ResilientJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetectRapidScanWaitJobFull implements ResilientJob<List<Response>> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final BlackDuckApiClient blackDuckApiClient;
    private final List<HttpUrl> remainingUrls;
    private final List<HttpUrl> completedUrls;

    //This can't be static because the job name could contain the word "Rapid" OR "Stateless" etc.
    private final String JOB_NAME;

    private boolean complete;

    public DetectRapidScanWaitJobFull(BlackDuckApiClient blackDuckApiClient, List<HttpUrl> resultUrl, BlackduckScanMode mode) {
        this.blackDuckApiClient = blackDuckApiClient;
        this.remainingUrls = new ArrayList<>();
        remainingUrls.addAll(resultUrl);
        this.completedUrls = new ArrayList<>(remainingUrls.size());
        JOB_NAME = "Waiting for " + mode.displayName() + " Scans";
    }

    @Override
    public void attemptJob() throws IntegrationException {
        if (remainingUrls.isEmpty()) {
            complete = true;
            return;
        }
        for (HttpUrl url : remainingUrls) {
            if (isComplete(url)) {
                completedUrls.add(url);
            }
        }

        remainingUrls.removeAll(completedUrls);
        complete = remainingUrls.isEmpty();
    }

    private boolean isComplete(HttpUrl url) throws IntegrationException {
        BlackDuckResponseRequest request = new DetectRapidScanRequestBuilder()
                .createResponseRequest(url);
        try (Response response = blackDuckApiClient.execute(request)) {
            return response.isStatusCodeSuccess();
        } catch (IntegrationRestException ex) {
            if (HttpStatus.SC_NOT_FOUND == ex.getHttpStatusCode()) {
                return false;
            } else {
                throw ex;
            }
        } catch (IOException ex) {
            throw new BlackDuckIntegrationException(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean wasJobCompleted() {
        return complete;
    }

    @Override
    public List<Response> onTimeout() throws IntegrationTimeoutException {
        throw new IntegrationTimeoutException("Error getting developer scan result. Timeout may have occurred.");
    }

    @Override
    public List<Response> onCompletion() throws IntegrationException {
        List<Response> allScanResponses = new ArrayList<>();
        for (HttpUrl url : completedUrls) {
            allScanResponses.add(getScanResultsForFULLUrl(url));
        }
        return allScanResponses;
    }

    private Response getScanResultsForFULLUrl(HttpUrl url) throws IntegrationException {
        logger.debug("Fetching full rapid scan results from endpoint: {}", url.string());
        Response newResponse =  blackDuckApiClient.get(url.appendRelativeUrl("full-result"));
        return newResponse;
    }

    @Override
    public String getName() {
        return JOB_NAME;
    }
}