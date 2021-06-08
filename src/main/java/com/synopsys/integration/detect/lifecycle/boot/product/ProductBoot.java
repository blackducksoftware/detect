/*
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.lifecycle.boot.product;

import java.io.IOException;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.discovery.ApiDiscovery;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.detect.configuration.DetectProperties;
import com.synopsys.integration.detect.configuration.DetectUserFriendlyException;
import com.synopsys.integration.detect.configuration.enumeration.ExitCodeType;
import com.synopsys.integration.detect.lifecycle.boot.decision.BlackDuckDecision;
import com.synopsys.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.synopsys.integration.detect.lifecycle.run.data.ProductRunData;
import com.synopsys.integration.detect.util.filter.DetectToolFilter;
import com.synopsys.integration.detect.workflow.blackduck.analytics.AnalyticsConfigurationService;
import com.synopsys.integration.detect.workflow.blackduck.analytics.AnalyticsSetting;
import com.synopsys.integration.detect.workflow.phonehome.PhoneHomeManager;
import com.synopsys.integration.exception.IntegrationException;

public class ProductBoot {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final BlackDuckConnectivityChecker blackDuckConnectivityChecker;
    private final AnalyticsConfigurationService analyticsConfigurationService;
    private final ProductBootFactory productBootFactory;
    private final ProductBootOptions productBootOptions;

    public ProductBoot(BlackDuckConnectivityChecker blackDuckConnectivityChecker, AnalyticsConfigurationService analyticsConfigurationService, ProductBootFactory productBootFactory,
        ProductBootOptions productBootOptions) {
        this.blackDuckConnectivityChecker = blackDuckConnectivityChecker;
        this.analyticsConfigurationService = analyticsConfigurationService;
        this.productBootFactory = productBootFactory;
        this.productBootOptions = productBootOptions;
    }

    public ProductRunData boot(BlackDuckDecision blackDuckDecision, DetectToolFilter detectToolFilter) throws DetectUserFriendlyException {
        if (!blackDuckDecision.shouldRun()) {
            throw new DetectUserFriendlyException(
                "Your environment was not sufficiently configured to run Black Duck or Polaris. Please configure your environment for at least one product.  See online help at: https://detect.synopsys.com/doc/",
                ExitCodeType.FAILURE_CONFIGURATION);

        }

        logger.debug("Detect product boot start.");

        BlackDuckRunData blackDuckRunData = getBlackDuckRunData(blackDuckDecision, productBootFactory, blackDuckConnectivityChecker, productBootOptions, analyticsConfigurationService);

        if (productBootOptions.isTestConnections()) {
            logger.debug(String.format("%s is set to 'true' so Detect will not run.", DetectProperties.DETECT_TEST_CONNECTION.getProperty().getName()));
            return null;
        }

        logger.debug("Detect product boot completed.");
        return new ProductRunData(blackDuckRunData, detectToolFilter);
    }

    @Nullable
    private BlackDuckRunData getBlackDuckRunData(BlackDuckDecision blackDuckDecision, ProductBootFactory productBootFactory, BlackDuckConnectivityChecker blackDuckConnectivityChecker, ProductBootOptions productBootOptions,
        AnalyticsConfigurationService analyticsConfigurationService) throws DetectUserFriendlyException {

        if (!blackDuckDecision.shouldRun()) {
            return null;
        }

        if (blackDuckDecision.isOffline()) {
            return BlackDuckRunData.offline();
        }

        logger.debug("Will boot Black Duck product.");
        BlackDuckServerConfig blackDuckServerConfig = productBootFactory.createBlackDuckServerConfig();
        BlackDuckConnectivityResult blackDuckConnectivityResult = blackDuckConnectivityChecker.determineConnectivity(blackDuckServerConfig);

        if (blackDuckConnectivityResult.isSuccessfullyConnected()) {
            BlackDuckServicesFactory blackDuckServicesFactory = blackDuckConnectivityResult.getBlackDuckServicesFactory();

            if (shouldUsePhoneHome(analyticsConfigurationService, blackDuckServicesFactory.getApiDiscovery(), blackDuckServicesFactory.getBlackDuckApiClient())) {
                PhoneHomeManager phoneHomeManager = productBootFactory.createPhoneHomeManager(blackDuckServicesFactory);
                return BlackDuckRunData.online(blackDuckDecision.scanMode(), blackDuckServicesFactory, phoneHomeManager, blackDuckConnectivityResult.getBlackDuckServerConfig());
            } else {
                logger.debug("Skipping phone home due to Black Duck global settings.");
                return BlackDuckRunData.onlineNoPhoneHome(blackDuckDecision.scanMode(), blackDuckServicesFactory, blackDuckConnectivityResult.getBlackDuckServerConfig());
            }
        } else {
            if (productBootOptions.isIgnoreConnectionFailures()) {
                logger.info(String.format("Failed to connect to Black Duck: %s", blackDuckConnectivityResult.getFailureReason()));
                logger.info(String.format("%s is set to 'true' so Detect will simply disable the Black Duck product.", DetectProperties.DETECT_IGNORE_CONNECTION_FAILURES.getProperty().getName()));
                return null;
            } else {
                throw new DetectUserFriendlyException("Could not communicate with Black Duck: " + blackDuckConnectivityResult.getFailureReason(), ExitCodeType.FAILURE_BLACKDUCK_CONNECTIVITY);
            }
        }
    }

    private boolean shouldUsePhoneHome(AnalyticsConfigurationService analyticsConfigurationService, ApiDiscovery apiDiscovery, BlackDuckApiClient blackDuckService) {
        try {
            AnalyticsSetting analyticsSetting = analyticsConfigurationService.fetchAnalyticsSetting(apiDiscovery, blackDuckService);
            return analyticsSetting.isEnabled();
        } catch (IntegrationException | IOException e) {
            logger.trace("Failed to check analytics setting on Black Duck. Likely this Black Duck instance does not support it.", e);
            return true; // Skip phone home will be applied at the library level.
        }
    }
}
