package com.blackduck.integration.detect.boot;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.blackduck.configuration.BlackDuckServerConfig;
import com.blackduck.integration.blackduck.service.BlackDuckServicesFactory;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.BlackduckScanMode;
import com.blackduck.integration.detect.lifecycle.boot.decision.BlackDuckDecision;
import com.blackduck.integration.detect.lifecycle.boot.product.BlackDuckConnectivityChecker;
import com.blackduck.integration.detect.lifecycle.boot.product.BlackDuckConnectivityResult;
import com.blackduck.integration.detect.lifecycle.boot.product.ProductBoot;
import com.blackduck.integration.detect.lifecycle.boot.product.ProductBootFactory;
import com.blackduck.integration.detect.lifecycle.boot.product.ProductBootOptions;
import com.blackduck.integration.detect.lifecycle.boot.product.version.BlackDuckVersionChecker;
import com.blackduck.integration.detect.lifecycle.boot.product.version.BlackDuckVersionCheckerResult;
import com.blackduck.integration.detect.lifecycle.run.data.ProductRunData;
import com.blackduck.integration.detect.workflow.blackduck.analytics.AnalyticsConfigurationService;
import com.blackduck.integration.detect.workflow.blackduck.analytics.AnalyticsSetting;
import com.blackduck.integration.detect.workflow.blackduck.settings.DetectPropertiesService;
import com.blackduck.integration.exception.IntegrationException;

public class ProductBootTest {
    @Test
    public void bothProductsSkippedThrows() {
        Assertions.assertThrows(DetectUserFriendlyException.class, () -> testBoot(BlackDuckDecision.skip(), new ProductBootOptions(false, false)));
    }

    @Test
    public void blackDuckConnectionFailureThrows() {
        BlackDuckConnectivityResult connectivityResult = BlackDuckConnectivityResult.failure("Failed to connect");

        Assertions.assertThrows(
            DetectUserFriendlyException.class,
            () -> testBoot(BlackDuckDecision.runOnline(BlackduckScanMode.INTELLIGENT, false), new ProductBootOptions(false, false), connectivityResult)
        );
    }

    @Test
    public void blackDuckFailureWithIgnoreReturnsFalse() throws DetectUserFriendlyException, IOException, IntegrationException {
        BlackDuckConnectivityResult connectivityResult = BlackDuckConnectivityResult.failure("Failed to connect");

        ProductRunData productRunData = testBoot(BlackDuckDecision.runOnline(BlackduckScanMode.INTELLIGENT, false), new ProductBootOptions(true, false), connectivityResult);

        Assertions.assertFalse(productRunData.shouldUseBlackDuckProduct());
    }

    @Test
    public void blackDuckConnectionFailureWithTestThrows() {
        BlackDuckConnectivityResult connectivityResult = BlackDuckConnectivityResult.failure("Failed to connect");

        Assertions.assertThrows(
            DetectUserFriendlyException.class,
            () -> testBoot(BlackDuckDecision.runOnline(BlackduckScanMode.INTELLIGENT, false), new ProductBootOptions(false, true), connectivityResult)
        );
    }

    @Test
    public void blackDuckConnectionSuccessWithTestReturnsNull() throws DetectUserFriendlyException, IOException, IntegrationException {
        BlackDuckConnectivityResult connectivityResult = BlackDuckConnectivityResult.success(
            Mockito.mock(BlackDuckServicesFactory.class),
            Mockito.mock(BlackDuckServerConfig.class),
            "Some Version"
        );

        ProductRunData productRunData = testBoot(BlackDuckDecision.runOnline(BlackduckScanMode.INTELLIGENT, false), new ProductBootOptions(false, true), connectivityResult);

        Assertions.assertNull(productRunData);
    }

    @Test
    public void blackDuckOnlyWorks() throws DetectUserFriendlyException, IOException, IntegrationException {
        BlackDuckConnectivityResult connectivityResult = BlackDuckConnectivityResult.success(
            Mockito.mock(BlackDuckServicesFactory.class),
            Mockito.mock(BlackDuckServerConfig.class),
            "Some Version"
        );
        ProductRunData productRunData = testBoot(BlackDuckDecision.runOnline(BlackduckScanMode.INTELLIGENT, false), new ProductBootOptions(false, false), connectivityResult);

        Assertions.assertTrue(productRunData.shouldUseBlackDuckProduct());
    }

    private ProductRunData testBoot(BlackDuckDecision blackDuckDecision, ProductBootOptions productBootOptions)
        throws DetectUserFriendlyException, IOException, IntegrationException {
        return testBoot(blackDuckDecision, productBootOptions, null);
    }

    private ProductRunData testBoot(BlackDuckDecision blackDuckDecision, ProductBootOptions productBootOptions, BlackDuckConnectivityResult blackDuckconnectivityResult)
        throws DetectUserFriendlyException, IOException, IntegrationException {
        ProductBootFactory productBootFactory = Mockito.mock(ProductBootFactory.class);
        Mockito.when(productBootFactory.createPhoneHomeManager(Mockito.any(), Mockito.any())).thenReturn(null);

        BlackDuckVersionChecker blackDuckVersionChecker = Mockito.mock(BlackDuckVersionChecker.class);
        Mockito.when(blackDuckVersionChecker.check(Mockito.anyString())).thenReturn(BlackDuckVersionCheckerResult.passed());
        BlackDuckConnectivityChecker blackDuckConnectivityChecker = Mockito.mock(BlackDuckConnectivityChecker.class);
        Mockito.when(blackDuckConnectivityChecker.determineConnectivity(Mockito.any())).thenReturn(blackDuckconnectivityResult);

        AnalyticsConfigurationService analyticsConfigurationService = Mockito.mock(AnalyticsConfigurationService.class);
        Mockito.when(analyticsConfigurationService.fetchAnalyticsSetting(Mockito.any(), Mockito.any())).thenReturn(new AnalyticsSetting("analytics", true));

        DetectPropertiesService detectPropertiesService = Mockito.mock(DetectPropertiesService.class);
        try {
            Mockito.when(detectPropertiesService.fetchDetectProperties(Mockito.any(), Mockito.any())).thenThrow(new IntegrationException("Endpoint not available"));
        } catch (Exception e) {
            // This won't happen in the mock setup
        }

        ProductBoot productBoot = new ProductBoot(
            blackDuckConnectivityChecker,
            analyticsConfigurationService,
            detectPropertiesService,
            productBootFactory,
            productBootOptions,
            blackDuckVersionChecker
        );

        return productBoot.boot(blackDuckDecision, null);
    }
}
