/*
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.workflow.blackduck;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.view.CodeLocationView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.blackduck.service.dataservice.CodeLocationService;
import com.synopsys.integration.detect.configuration.DetectUserFriendlyException;
import com.synopsys.integration.detect.configuration.enumeration.ExitCodeType;
import com.synopsys.integration.exception.IntegrationException;

public class DetectCodeLocationUnmapService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final BlackDuckApiClient blackDuckService;
    private final CodeLocationService codeLocationService;

    public DetectCodeLocationUnmapService(BlackDuckApiClient blackDuckService, CodeLocationService codeLocationService) {
        this.blackDuckService = blackDuckService;
        this.codeLocationService = codeLocationService;
    }

    public void unmapCodeLocations(ProjectVersionView projectVersionView) throws DetectUserFriendlyException {
        try {
            List<CodeLocationView> codeLocationViews = blackDuckService.getAllResponses(projectVersionView.metaCodelocationsLink());

            for (CodeLocationView codeLocationView : codeLocationViews) {
                codeLocationService.unmapCodeLocation(codeLocationView);
            }
            logger.info("Successfully unmapped (" + codeLocationViews.size() + ") code locations.");
        } catch (IntegrationException e) {
            String errorMessage = String.format("There was a problem unmapping Code Locations: %s", e.getMessage());
            throw new DetectUserFriendlyException(errorMessage, e, ExitCodeType.FAILURE_GENERAL_ERROR);
        }

    }
}
