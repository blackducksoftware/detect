package com.blackduck.integration.detect.workflow.blackduck.project.options;

import java.util.List;

import com.blackduck.integration.blackduck.api.generated.enumeration.ProjectCloneCategoriesType;
import com.blackduck.integration.blackduck.api.generated.enumeration.ProjectVersionDistributionType;
import com.blackduck.integration.blackduck.api.manual.temporary.enumeration.ProjectVersionPhaseType;

public class ProjectSyncOptions {
    private final ProjectVersionPhaseType projectVersionPhase;
    private final ProjectVersionDistributionType projectVersionDistribution;
    private final Integer projectTier;
    private final String projectDescription;
    private final String projectVersionNotes;
    private final List<ProjectCloneCategoriesType> cloneCategories;
    private final Boolean forceProjectVersionUpdate;
    private final String projectVersionNickname;
    private final Boolean projectLevelAdjustments;
    private final Boolean deepLicenseEnabled;

    public ProjectSyncOptions(
        ProjectVersionPhaseType projectVersionPhase,
        ProjectVersionDistributionType projectVersionDistribution,
        Integer projectTier,
        String projectDescription,
        String projectVersionNotes,
        List<ProjectCloneCategoriesType> cloneCategories,
        Boolean forceProjectVersionUpdate,
        String projectVersionNickname,
        Boolean projectLevelAdjustments,
        Boolean deepLicenseEnabled
    ) {
        this.projectVersionPhase = projectVersionPhase;
        this.projectVersionDistribution = projectVersionDistribution;
        this.projectTier = projectTier;
        this.projectDescription = projectDescription;
        this.projectVersionNotes = projectVersionNotes;
        this.cloneCategories = cloneCategories;
        this.forceProjectVersionUpdate = forceProjectVersionUpdate;
        this.projectVersionNickname = projectVersionNickname;
        this.projectLevelAdjustments = projectLevelAdjustments;
        this.deepLicenseEnabled = deepLicenseEnabled;
    }

    public ProjectVersionPhaseType getProjectVersionPhase() {
        return projectVersionPhase;
    }

    public ProjectVersionDistributionType getProjectVersionDistribution() {
        return projectVersionDistribution;
    }

    public Integer getProjectTier() {
        return projectTier;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public String getProjectVersionNotes() {
        return projectVersionNotes;
    }

    public List<ProjectCloneCategoriesType> getCloneCategories() {
        return cloneCategories;
    }

    public Boolean isForceProjectVersionUpdate() {
        return forceProjectVersionUpdate;
    }

    public String getProjectVersionNickname() {
        return projectVersionNickname;
    }

    public Boolean getProjectLevelAdjustments() {
        return projectLevelAdjustments;
    }

    public Boolean isDeepLicenseEnabled() {
        return deepLicenseEnabled;
    }
}
