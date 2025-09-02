package com.blackduck.integration.detect.lifecycle.run.step;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.service.model.ProjectVersionWrapper;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.workflow.blackduck.project.customfields.CustomFieldDocument;
import com.blackduck.integration.detect.workflow.blackduck.project.options.CloneFindResult;
import com.blackduck.integration.detect.workflow.blackduck.project.options.FindCloneOptions;
import com.blackduck.integration.detect.workflow.blackduck.project.options.ParentProjectMapOptions;
import com.blackduck.integration.detect.workflow.blackduck.project.options.ProjectGroupFindResult;
import com.blackduck.integration.detect.workflow.blackduck.project.options.ProjectGroupOptions;
import com.blackduck.integration.detect.workflow.blackduck.project.options.ProjectVersionLicenseFindResult;
import com.blackduck.integration.detect.workflow.blackduck.project.options.ProjectVersionLicenseOptions;
import com.blackduck.integration.util.NameVersion;

public class BlackDuckProjectVersionStepRunner {
    private final OperationRunner operationRunner;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public BlackDuckProjectVersionStepRunner(OperationRunner operationRunner) {
        this.operationRunner = operationRunner;
    }

    ProjectVersionWrapper runAll(NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData) throws DetectUserFriendlyException, OperationException {
        CloneFindResult cloneFindResult = findClone(projectNameVersion.getName(), blackDuckRunData);
        ProjectGroupFindResult projectGroupFindResult = findProjectGroup(blackDuckRunData);
        ProjectVersionLicenseFindResult projectVersionLicensesFindResult = findLicense(blackDuckRunData);
        ProjectVersionWrapper projectVersion = operationRunner.syncProjectVersion(
            projectNameVersion,
            projectGroupFindResult,
            cloneFindResult,
            projectVersionLicensesFindResult,
            blackDuckRunData
        );

        ParentProjectMapOptions mapOptions = operationRunner.calculateParentProjectMapOptions();
        if (StringUtils.isNotBlank(mapOptions.getParentProjectName()) || StringUtils.isNotBlank(mapOptions.getParentProjectVersionName())) {
            operationRunner.mapToParentProject(mapOptions.getParentProjectName(), mapOptions.getParentProjectVersionName(), projectVersion, blackDuckRunData);
        }

        String applicationId = operationRunner.calculateApplicationId();
        if (StringUtils.isBlank(applicationId)) {
            logger.debug("No 'Application ID' to set.");
        } else {
            operationRunner.setApplicationId(applicationId, projectVersion, blackDuckRunData);
        }

        CustomFieldDocument customFieldDocument = operationRunner.calculateCustomFields();
        if (customFieldDocument == null || (customFieldDocument.getProject().size() == 0 && customFieldDocument.getVersion().size() == 0)) {
            logger.debug("No custom fields to set.");
        } else {
            operationRunner.updateCustomFields(customFieldDocument, projectVersion, blackDuckRunData);
        }

        List<String> userGroups = operationRunner.calculateUserGroups();
        if (userGroups == null) {
            logger.debug("No user groups to set.");
        } else {
            operationRunner.addUserGroups(userGroups, projectVersion, blackDuckRunData);
        }

        List<String> tags = operationRunner.calculateTags();
        if (tags == null) {
            logger.debug("No tags to set.");
        } else {
            operationRunner.addTags(tags, projectVersion, blackDuckRunData);
        }

        return projectVersion;
    }

    private ProjectGroupFindResult findProjectGroup(BlackDuckRunData blackDuckRunData) throws OperationException {
        ProjectGroupOptions projectGroupOptions = operationRunner.calculateProjectGroupOptions();
        if (StringUtils.isNotBlank(projectGroupOptions.getProjectGroup())) {
            logger.info("Will look for project group named: " + projectGroupOptions.getProjectGroup());
            return ProjectGroupFindResult.of(operationRunner.findProjectGroup(blackDuckRunData, projectGroupOptions.getProjectGroup()));
        } else {
            logger.debug("No project group was supplied. Will not assign a project group.");
            return ProjectGroupFindResult.skip();
        }
    }

    private CloneFindResult findClone(String projectName, BlackDuckRunData blackDuckRunData) throws OperationException {
        FindCloneOptions cloneOptions = operationRunner.calculateCloneOptions();
        if (cloneOptions.getCloneLatestProjectVersion()) {
            logger.debug("Cloning the most recent project version.");
            return operationRunner.findLatestProjectVersionCloneUrl(blackDuckRunData, projectName);
        } else if (StringUtils.isNotBlank(cloneOptions.getCloneVersionName())) {
            return operationRunner.findNamedCloneUrl(blackDuckRunData, projectName, cloneOptions.getCloneVersionName());
        } else {
            logger.debug("No clone project or version name supplied. Will not clone.");
            return CloneFindResult.empty();
        }
    }

    private ProjectVersionLicenseFindResult findLicense(BlackDuckRunData blackDuckRunData) throws OperationException {
        ProjectVersionLicenseOptions projectVersionLicenseOptions = operationRunner.calculateProjectVersionLicenses();
        if (StringUtils.isNotBlank(projectVersionLicenseOptions.getLicenseName())) {
            return ProjectVersionLicenseFindResult.of(operationRunner.findLicenseUrl(blackDuckRunData, projectVersionLicenseOptions.getLicenseName()));
        } else {
            logger.debug("No project version licenses were supplied.  Will not update licenses.");
            return ProjectVersionLicenseFindResult.empty();
        }
    }
}
