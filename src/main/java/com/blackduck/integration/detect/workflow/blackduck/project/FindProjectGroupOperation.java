package com.blackduck.integration.detect.workflow.blackduck.project;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.api.core.response.UrlMultipleResponses;
import com.blackduck.integration.blackduck.api.generated.discovery.ApiDiscovery;
import com.blackduck.integration.blackduck.api.generated.view.ProjectGroupsView;
import com.blackduck.integration.blackduck.http.BlackDuckQuery;
import com.blackduck.integration.blackduck.http.BlackDuckRequestBuilder;
import com.blackduck.integration.blackduck.http.BlackDuckRequestFilter;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.blackduck.service.request.BlackDuckMultipleRequest;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.HttpUrl;

public class FindProjectGroupOperation {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final BlackDuckApiClient blackDuckApiClient;
    private final UrlMultipleResponses<ProjectGroupsView> projectGroupsResponses;

    public FindProjectGroupOperation(BlackDuckApiClient blackDuckApiClient, ApiDiscovery apiDiscovery) {
        this.blackDuckApiClient = blackDuckApiClient;
        projectGroupsResponses = apiDiscovery.metaMultipleResponses(ApiDiscovery.PROJECT_GROUPS_PATH);
    }

    public HttpUrl findProjectGroup(String projectGroupName) throws IntegrationException, DetectUserFriendlyException {
        BlackDuckRequestBuilder blackDuckRequestBuilder = new BlackDuckRequestBuilder()
            .commonGet()
            .addBlackDuckQuery(new BlackDuckQuery("name", projectGroupName))
            .addBlackDuckFilter(BlackDuckRequestFilter.createFilterWithSingleValue("exactName", "true"));

        BlackDuckMultipleRequest<ProjectGroupsView> requestMultiple = blackDuckRequestBuilder.buildBlackDuckRequest(projectGroupsResponses);
        List<ProjectGroupsView> response = blackDuckApiClient.getAllResponses(requestMultiple);
        if (response.size() != 1) {
            throw new DetectUserFriendlyException(
                "Project Group Name must have exactly 1 match on Black Duck SCA, instead '" + projectGroupName + "' had " + response.size() + " matches.",
                ExitCodeType.FAILURE_BLACKDUCK_FEATURE_ERROR
            );
        }
        ProjectGroupsView result = response.get(0);
        return result.getHref();
    }
}
