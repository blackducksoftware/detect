package com.blackduck.integration.detect.workflow.blackduck.developer;

import java.util.List;

import com.blackduck.integration.rest.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.detect.configuration.enumeration.BlackduckScanMode;
import com.blackduck.integration.detect.workflow.blackduck.developer.blackduck.DetectRapidScanWaitJobFull;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.log.Slf4jIntLogger;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.wait.ResilientJobConfig;
import com.blackduck.integration.wait.ResilientJobExecutor;
import com.blackduck.integration.wait.tracker.WaitIntervalTracker;
import com.blackduck.integration.wait.tracker.WaitIntervalTrackerFactory;

public class RapidModeWaitOperation {
    public static final int DEFAULT_WAIT_INTERVAL_IN_SECONDS = 1;

    private final BlackDuckApiClient blackDuckApiClient;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public RapidModeWaitOperation(BlackDuckApiClient blackDuckApiClient) {
        this.blackDuckApiClient = blackDuckApiClient;
    }

    public List<Response> waitForScans(List<HttpUrl> uploadedScans, long timeoutInSeconds, int waitIntervalInSeconds, BlackduckScanMode mode, int maxWaitInSeconds)
            throws IntegrationException, InterruptedException {
            WaitIntervalTracker waitIntervalTracker = WaitIntervalTrackerFactory.createProgressive(timeoutInSeconds, maxWaitInSeconds);
            ResilientJobConfig waitJobConfig = new ResilientJobConfig(new Slf4jIntLogger(logger), System.currentTimeMillis(), waitIntervalTracker);
            DetectRapidScanWaitJobFull waitJob = new DetectRapidScanWaitJobFull(blackDuckApiClient, uploadedScans, mode);
            ResilientJobExecutor jobExecutor = new ResilientJobExecutor(waitJobConfig);
            return jobExecutor.executeJob(waitJob);
        }
}
