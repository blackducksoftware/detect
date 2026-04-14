package com.blackduck.integration.detect.configuration;

/**
 * POJO representing project settings from the project settings JSON.
 * 
 * Expected JSON structure:
 * {
 *   "name": "project-name",
 *   "description": "project description",
 *   "projectTier": 3,
 *   "projectLevelAdjustments": true,
 *   "deepLicenseDataEnabled": true,
 *   "cloneCategories": "ALL",
 *   "versionRequest": { ... }
 * }
 */
public class ProjectSettings {
    private String name;
    private String description;
    private Integer projectTier;
    private Boolean projectLevelAdjustments;
    private Boolean deepLicenseDataEnabled;
    private String cloneCategories;
    private VersionSettings versionRequest;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getProjectTier() {
        return projectTier;
    }

    public void setProjectTier(Integer projectTier) {
        this.projectTier = projectTier;
    }

    public Boolean getProjectLevelAdjustments() {
        return projectLevelAdjustments;
    }

    public void setProjectLevelAdjustments(Boolean projectLevelAdjustments) {
        this.projectLevelAdjustments = projectLevelAdjustments;
    }

    public Boolean getLicenseDeepLicense() {
        return deepLicenseDataEnabled;
    }

    public void setLicenseDeepLicense(Boolean deepLicenseDataEnabled) {
        this.deepLicenseDataEnabled = deepLicenseDataEnabled;
    }

    public String getCloneCategories() {
        return cloneCategories;
    }

    public void setCloneCategories(String cloneCategories) {
        this.cloneCategories = cloneCategories;
    }

    public VersionSettings getVersionRequest() {
        return versionRequest;
    }

    public void setVersionRequest(VersionSettings versionRequest) {
        this.versionRequest = versionRequest;
    }
}