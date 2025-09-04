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

    private ProjectSyncOptions(
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ProjectVersionPhaseType projectVersionPhase;
        private ProjectVersionDistributionType projectVersionDistribution;
        private Integer projectTier;
        private String projectDescription;
        private String projectVersionNotes;
        private List<ProjectCloneCategoriesType> cloneCategories;
        private Boolean forceProjectVersionUpdate;
        private String projectVersionNickname;
        private Boolean projectLevelAdjustments;
        private Boolean deepLicenseEnabled;

        public Builder projectVersionPhase(ProjectVersionPhaseType projectVersionPhase) {
            this.projectVersionPhase = projectVersionPhase;
            return this;
        }

        public Builder projectVersionDistribution(ProjectVersionDistributionType projectVersionDistribution) {
            this.projectVersionDistribution = projectVersionDistribution;
            return this;
        }

        public Builder projectTier(Integer projectTier) {
            this.projectTier = projectTier;
            return this;
        }

        public Builder projectDescription(String projectDescription) {
            this.projectDescription = projectDescription;
            return this;
        }

        public Builder projectVersionNotes(String projectVersionNotes) {
            this.projectVersionNotes = projectVersionNotes;
            return this;
        }

        public Builder cloneCategories(List<ProjectCloneCategoriesType> cloneCategories) {
            this.cloneCategories = cloneCategories;
            return this;
        }

        public Builder forceProjectVersionUpdate(Boolean forceProjectVersionUpdate) {
            this.forceProjectVersionUpdate = forceProjectVersionUpdate;
            return this;
        }

        public Builder projectVersionNickname(String projectVersionNickname) {
            this.projectVersionNickname = projectVersionNickname;
            return this;
        }

        public Builder projectLevelAdjustments(Boolean projectLevelAdjustments) {
            this.projectLevelAdjustments = projectLevelAdjustments;
            return this;
        }

        public Builder deepLicenseEnabled(Boolean deepLicenseEnabled) {
            this.deepLicenseEnabled = deepLicenseEnabled;
            return this;
        }

        public ProjectSyncOptions build() {
            return new ProjectSyncOptions(
                projectVersionPhase,
                projectVersionDistribution,
                projectTier,
                projectDescription,
                projectVersionNotes,
                cloneCategories,
                forceProjectVersionUpdate,
                projectVersionNickname,
                projectLevelAdjustments,
                deepLicenseEnabled
            );
        }
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
