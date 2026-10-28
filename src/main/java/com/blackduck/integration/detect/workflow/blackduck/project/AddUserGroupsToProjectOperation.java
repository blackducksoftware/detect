package com.blackduck.integration.detect.workflow.blackduck.project;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.blackduck.integration.blackduck.api.manual.view.ProjectView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.service.dataservice.ProjectUsersService;
import com.blackduck.integration.blackduck.service.model.ProjectVersionWrapper;
import com.blackduck.integration.exception.IntegrationException;

public class AddUserGroupsToProjectOperation {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ProjectUsersService projectUsersService;

    public AddUserGroupsToProjectOperation(ProjectUsersService projectUsersService) {
        this.projectUsersService = projectUsersService;
    }

    public void addUserGroupsToProject(ProjectVersionWrapper projectVersionWrapper, List<String> groupsToAddToProject) throws IntegrationException {
        ProjectView projectView = projectVersionWrapper.getProjectView();
        for (String userGroupName : groupsToAddToProject) {
            if (StringUtils.isNotBlank(userGroupName)) {
                logger.debug("Adding user group {} to project {}", userGroupName, projectView.getName());
                projectUsersService.addGroupToProject(projectView, userGroupName);
            }
        }
    }
}
