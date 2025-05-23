package com.blackduck.integration.detect.workflow.blackduck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.api.generated.view.BomStatusScanView;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.log.Slf4jIntLogger;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.wait.ResilientJobConfig;
import com.blackduck.integration.wait.ResilientJobExecutor;
import com.blackduck.integration.wait.tracker.WaitIntervalTracker;
import com.blackduck.integration.wait.tracker.WaitIntervalTrackerFactory;

public class BomScanWaitOperation {
    
    private final BlackDuckApiClient blackDuckApiClient;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public BomScanWaitOperation(BlackDuckApiClient blackDuckApiClient) {
        this.blackDuckApiClient = blackDuckApiClient;
    }

    public BomStatusScanView waitForScan(HttpUrl scanUrl, long timeoutInSeconds, int maxWaitInSeconds) throws InterruptedException, IntegrationException {
        WaitIntervalTracker waitIntervalTracker = WaitIntervalTrackerFactory.createProgressive(timeoutInSeconds, maxWaitInSeconds);
        ResilientJobConfig waitJobConfig = new ResilientJobConfig(new Slf4jIntLogger(logger), System.currentTimeMillis(), waitIntervalTracker);
        
        DetectBomScanWaitJob waitJob = new DetectBomScanWaitJob(blackDuckApiClient, scanUrl);
        
        ResilientJobExecutor jobExecutor = new ResilientJobExecutor(waitJobConfig);
        return jobExecutor.executeJob(waitJob);
    }

}