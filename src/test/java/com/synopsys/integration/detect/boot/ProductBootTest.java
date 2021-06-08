/**
 * synopsys-detect
 *
 * Copyright (c) 2020 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.detect.boot;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.detect.configuration.DetectUserFriendlyException;
import com.synopsys.integration.detect.configuration.enumeration.BlackduckScanMode;
import com.synopsys.integration.detect.lifecycle.boot.decision.BlackDuckDecision;
import com.synopsys.integration.detect.lifecycle.boot.product.BlackDuckConnectivityChecker;
import com.synopsys.integration.detect.lifecycle.boot.product.BlackDuckConnectivityResult;
import com.synopsys.integration.detect.lifecycle.boot.product.ProductBoot;
import com.synopsys.integration.detect.lifecycle.boot.product.ProductBootFactory;
import com.synopsys.integration.detect.lifecycle.boot.product.ProductBootOptions;
import com.synopsys.integration.detect.lifecycle.run.data.ProductRunData;
import com.synopsys.integration.detect.workflow.blackduck.analytics.AnalyticsConfigurationService;
import com.synopsys.integration.detect.workflow.blackduck.analytics.AnalyticsSetting;
import com.synopsys.integration.exception.IntegrationException;

public class ProductBootTest {
    @Test
    public void bothProductsSkippedThrows() {
        Assertions.assertThrows(DetectUserFriendlyException.class, () -> testBoot(BlackDuckDecision.skip(), new ProductBootOptions(false, false)));
    }

    @Test
    public void blackDuckConnectionFailureThrows() {
        BlackDuckConnectivityResult connectivityResult = BlackDuckConnectivityResult.failure("Failed to connect");

        Assertions.assertThrows(DetectUserFriendlyException.class, () -> testBoot(BlackDuckDecision.runOnline(BlackduckScanMode.INTELLIGENT), new ProductBootOptions(false, false), connectivityResult));
    }

    @Test
    public void blackDuckFailureWithIgnoreReturnsFalse() throws DetectUserFriendlyException, IOException, IntegrationException {
        BlackDuckConnectivityResult connectivityResult = BlackDuckConnectivityResult.failure("Failed to connect");

        ProductRunData productRunData = testBoot(BlackDuckDecision.runOnline(BlackduckScanMode.INTELLIGENT), new ProductBootOptions(true, false), connectivityResult);

        Assertions.assertFalse(productRunData.shouldUseBlackDuckProduct());
    }

    @Test
    public void blackDuckConnectionFailureWithTestThrows() {
        BlackDuckConnectivityResult connectivityResult = BlackDuckConnectivityResult.failure("Failed to connect");

        Assertions.assertThrows(DetectUserFriendlyException.class, () -> testBoot(BlackDuckDecision.runOnline(BlackduckScanMode.INTELLIGENT), new ProductBootOptions(false, true), connectivityResult));
    }

    @Test
    public void blackDuckConnectionSuccessWithTestReturnsNull() throws DetectUserFriendlyException, IOException, IntegrationException {
        BlackDuckConnectivityResult connectivityResult = BlackDuckConnectivityResult.success(Mockito.mock(BlackDuckServicesFactory.class), Mockito.mock(BlackDuckServerConfig.class));

        ProductRunData productRunData = testBoot(BlackDuckDecision.runOnline(BlackduckScanMode.INTELLIGENT), new ProductBootOptions(false, true), connectivityResult);

        Assertions.assertNull(productRunData);
    }

    @Test
    public void blackDuckOnlyWorks() throws DetectUserFriendlyException, IOException, IntegrationException {
        BlackDuckConnectivityResult connectivityResult = BlackDuckConnectivityResult.success(Mockito.mock(BlackDuckServicesFactory.class), Mockito.mock(BlackDuckServerConfig.class));
        ProductRunData productRunData = testBoot(BlackDuckDecision.runOnline(BlackduckScanMode.INTELLIGENT), new ProductBootOptions(false, false), connectivityResult);

        Assertions.assertTrue(productRunData.shouldUseBlackDuckProduct());
    }

    private ProductRunData testBoot(BlackDuckDecision blackDuckDecision, ProductBootOptions productBootOptions) throws DetectUserFriendlyException, IOException, IntegrationException {
        return testBoot(blackDuckDecision, productBootOptions, null);
    }

    private ProductRunData testBoot(BlackDuckDecision blackDuckDecision, ProductBootOptions productBootOptions,
        BlackDuckConnectivityResult blackDuckconnectivityResult) throws DetectUserFriendlyException, IOException, IntegrationException {
        ProductBootFactory productBootFactory = Mockito.mock(ProductBootFactory.class);
        Mockito.when(productBootFactory.createPhoneHomeManager(Mockito.any())).thenReturn(null);

        BlackDuckConnectivityChecker blackDuckConnectivityChecker = Mockito.mock(BlackDuckConnectivityChecker.class);
        Mockito.when(blackDuckConnectivityChecker.determineConnectivity(Mockito.any())).thenReturn(blackDuckconnectivityResult);

        AnalyticsConfigurationService analyticsConfigurationService = Mockito.mock(AnalyticsConfigurationService.class);
        Mockito.when(analyticsConfigurationService.fetchAnalyticsSetting(Mockito.any(), Mockito.any())).thenReturn(new AnalyticsSetting("analytics", true));

        ProductBoot productBoot = new ProductBoot(blackDuckConnectivityChecker, analyticsConfigurationService, productBootFactory, productBootOptions);

        return productBoot.boot(blackDuckDecision, null);
    }
}
