package com.blackduck.integration.detect.workflow.blackduck.project;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.api.generated.view.CodeLocationView;
import com.blackduck.integration.blackduck.api.generated.view.ProjectVersionView;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.blackduck.service.dataservice.CodeLocationService;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.exception.IntegrationException;

public class UnmapCodeLocationsOperation {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final BlackDuckApiClient blackDuckService;
    private final CodeLocationService codeLocationService;

    public UnmapCodeLocationsOperation(BlackDuckApiClient blackDuckService, CodeLocationService codeLocationService) {
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
