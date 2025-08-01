package com.blackduck.integration.detect.workflow.blackduck;

import com.blackduck.integration.blackduck.api.generated.view.BomStatusScanView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.api.generated.enumeration.BomStatusScanStatusType;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.exception.IntegrationTimeoutException;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.wait.ResilientJob;

public class DetectBomScanWaitJob implements ResilientJob<BomStatusScanView> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final BlackDuckApiClient blackDuckApiClient;
    private final HttpUrl scanUrl;
    private BomStatusScanView scanResponse;
    
    private boolean complete;
    private static final String JOB_NAME = "BOM Scan Wait Job ";

    public DetectBomScanWaitJob(BlackDuckApiClient blackDuckApiClient, HttpUrl scanUrl) {
        this.blackDuckApiClient = blackDuckApiClient;
        this.scanUrl = scanUrl;
        complete = false;
    }

    @Override
    public void attemptJob() throws IntegrationException {
        BomStatusScanView initialResponse = 
                blackDuckApiClient.getResponse(scanUrl, BomStatusScanView.class);
        
        BomStatusScanStatusType status = initialResponse.getStatus();
        if (status != BomStatusScanStatusType.BUILDING) {
            if (status == BomStatusScanStatusType.NOT_INCLUDED) {
                logger.warn("Encountered unexpected scan status: {}", status);
            }
            complete = true;
            scanResponse = initialResponse;
        }
    }

    @Override
    public boolean wasJobCompleted() {
        return complete;
    }

    @Override
    public BomStatusScanView onTimeout() throws IntegrationTimeoutException {
        throw new IntegrationTimeoutException("Error waiting for scan to be considered for including in BOM. Timeout may have occurred.");
    }

    @Override
    public BomStatusScanView onCompletion() throws IntegrationException {
        return scanResponse;
    }

    @Override
    public String getName() {
        return JOB_NAME + scanUrl;
    }

}