package com.blackduck.integration.detect.configuration;

import java.util.List;

/**
 * POJO representing project settings from the project settings JSON.
 * 
 * Expected JSON structure:
 * {
 *   "name": "project-name",
 *   "description": "project description", 
 *   "tier": 3,
 *   "applicationId": "app-id",
 *   "groupName": "group-name",
 *   "tags": ["tag1", "tag2"],
 *   "userGroups": ["group1", "group2"],
 *   "levelAdjustments": true,
 *   "deepLicense": true,
 *   "cloneCategories": "ALL",
 *   "version": { ... }
 * }
 */
public class ProjectSettings {
    private String name;
    private String description;
    private Integer tier;
    private String applicationId;
    private String groupName;
    private List<String> tags;
    private List<String> userGroups;
    private Boolean levelAdjustments;
    private Boolean deepLicense;
    private String cloneCategories;
    private VersionSettings version;

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

    public Integer getTier() {
        return tier;
    }

    public void setTier(Integer tier) {
        this.tier = tier;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getUserGroups() {
        return userGroups;
    }

    public void setUserGroups(List<String> userGroups) {
        this.userGroups = userGroups;
    }

    public Boolean getLevelAdjustments() {
        return levelAdjustments;
    }

    public void setLevelAdjustments(Boolean levelAdjustments) {
        this.levelAdjustments = levelAdjustments;
    }

    public Boolean getDeepLicense() {
        return deepLicense;
    }

    public void setDeepLicense(Boolean deepLicense) {
        this.deepLicense = deepLicense;
    }

    public String getCloneCategories() {
        return cloneCategories;
    }

    public void setCloneCategories(String cloneCategories) {
        this.cloneCategories = cloneCategories;
    }

    public VersionSettings getVersion() {
        return version;
    }

    public void setVersion(VersionSettings version) {
        this.version = version;
    }
}