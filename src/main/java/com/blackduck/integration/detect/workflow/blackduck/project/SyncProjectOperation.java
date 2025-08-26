package com.blackduck.integration.detect.workflow.blackduck.project;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.api.generated.enumeration.ProjectCloneCategoriesType;
import com.blackduck.integration.blackduck.service.dataservice.ProjectService;
import com.blackduck.integration.blackduck.service.model.ProjectSyncModel;
import com.blackduck.integration.blackduck.service.model.ProjectVersionWrapper;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.workflow.blackduck.project.options.CloneFindResult;
import com.blackduck.integration.detect.workflow.blackduck.project.options.ProjectGroupFindResult;
import com.blackduck.integration.detect.workflow.blackduck.project.options.ProjectSyncOptions;
import com.blackduck.integration.detect.workflow.blackduck.project.options.ProjectVersionLicenseFindResult;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;

public class SyncProjectOperation {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ProjectService projectService;

    public SyncProjectOperation(ProjectService projectService) {
        this.projectService = projectService;
    }

    public ProjectVersionWrapper sync(
        NameVersion projectNameVersion,
        ProjectGroupFindResult projectGroupFindResult,
        CloneFindResult cloneFindResult,
        ProjectVersionLicenseFindResult projectVersionLicensesFindResult,
        ProjectSyncOptions projectSyncOptions
    ) throws DetectUserFriendlyException, IntegrationException {
        ProjectSyncModel projectSyncModel = createProjectSyncModel(
            projectNameVersion,
            projectGroupFindResult,
            cloneFindResult,
            projectVersionLicensesFindResult,
            projectSyncOptions
        );
        boolean forceUpdate = projectSyncOptions.isForceProjectVersionUpdate();
        return projectService.syncProjectAndVersion(projectSyncModel, forceUpdate);
    }

    public ProjectSyncModel createProjectSyncModel(
        NameVersion projectNameVersion,
        ProjectGroupFindResult projectGroupFindResult,
        CloneFindResult cloneFindResult,
        ProjectVersionLicenseFindResult projectVersionLicensesFindResult,
        ProjectSyncOptions projectSyncOptions
    ) {
        ProjectSyncModel projectSyncModel = ProjectSyncModel.createWithDefaults(projectNameVersion.getName(), projectNameVersion.getVersion());

        // TODO: Handle a boolean property not being set in detect configuration - ie need to determine if this property actually exists in the ConfigurableEnvironment - just omit this one?
        projectSyncModel.setProjectLevelAdjustments(projectSyncOptions.getProjectLevelAdjustments());

        Optional.ofNullable(projectSyncOptions.getProjectVersionPhase()).ifPresent(projectSyncModel::setPhase);
        Optional.ofNullable(projectSyncOptions.getProjectVersionDistribution()).ifPresent(projectSyncModel::setDistribution);
        Optional.ofNullable(projectSyncOptions.isForceProjectVersionUpdate()).ifPresent(projectSyncModel::setUpdate);

        Integer projectTier = projectSyncOptions.getProjectTier();
        if (null != projectTier && projectTier >= 1 && projectTier <= 5) {
            projectSyncModel.setProjectTier(projectTier);
        }

        String description = projectSyncOptions.getProjectDescription();
        if (StringUtils.isNotBlank(description)) {
            projectSyncModel.setDescription(description);
        }

        String releaseComments = projectSyncOptions.getProjectVersionNotes();
        if (StringUtils.isNotBlank(releaseComments)) {
            projectSyncModel.setReleaseComments(releaseComments);
        }

        List<ProjectCloneCategoriesType> cloneCategories = projectSyncOptions.getCloneCategories();
        projectSyncModel.setCloneCategories(cloneCategories);

        String nickname = projectSyncOptions.getProjectVersionNickname();
        if (StringUtils.isNotBlank(nickname)) {
            projectSyncModel.setNickname(nickname);
        }

        if (cloneFindResult.getCloneUrl().isPresent()) {
            logger.debug("Cloning project version from release url: {}", cloneFindResult.getCloneUrl().get());
            projectSyncModel.setCloneFromReleaseUrl(cloneFindResult.getCloneUrl().get().string());
        }

        projectGroupFindResult.getProjectGroup().ifPresent(projectGroupUrl -> {
            logger.debug("Setting project group to url: {}", projectGroupUrl);
            projectSyncModel.setProjectGroup(projectGroupUrl.string());
        });

        projectVersionLicensesFindResult.getLicenseUrl().ifPresent(projectSyncModel::setVersionLicenseUrl);

        if (projectSyncOptions.isDeepLicenseEnabled()) {
            projectSyncModel.setDeepLicenseDataEnabled(true);
            projectSyncModel.setDeepLicenseDataSnippetEnabled(true);
        }
        
        return projectSyncModel;
    }
}
