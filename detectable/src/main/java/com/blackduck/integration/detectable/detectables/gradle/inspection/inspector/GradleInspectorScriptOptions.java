package com.blackduck.integration.detectable.detectables.gradle.inspection.inspector;

import java.util.List;

public class GradleInspectorScriptOptions {
    private final List<String> excludedProjectNames;
    private final List<String> includedProjectNames;
    private final List<String> excludedProjectPaths;
    private final List<String> includedProjectPaths;
    private final List<String> excludedConfigurationNames;
    private final List<String> includedConfigurationNames;
    private final String gradleInspectorRepositoryUrl;
    private final boolean rootOnly;

    public GradleInspectorScriptOptions(
        List<String> excludedProjectNames,
        List<String> includedProjectNames,
        List<String> excludedProjectPaths,
        List<String> includedProjectPaths,
        List<String> excludedConfigurationNames,
        List<String> includedConfigurationNames,
        String gradleInspectorRepositoryUrl,
        boolean rootOnly
    ) {
        this.excludedProjectNames = excludedProjectNames;
        this.includedProjectNames = includedProjectNames;
        this.excludedProjectPaths = excludedProjectPaths;
        this.includedProjectPaths = includedProjectPaths;
        this.excludedConfigurationNames = excludedConfigurationNames;
        this.includedConfigurationNames = includedConfigurationNames;
        this.gradleInspectorRepositoryUrl = gradleInspectorRepositoryUrl;
        this.rootOnly = rootOnly;
    }

    public String getGradleInspectorRepositoryUrl() {
        return gradleInspectorRepositoryUrl;
    }

    public List<String> getExcludedProjectNames() {
        return excludedProjectNames;
    }

    public List<String> getIncludedProjectNames() {
        return includedProjectNames;
    }

    public List<String> getExcludedConfigurationNames() {
        return excludedConfigurationNames;
    }

    public List<String> getIncludedConfigurationNames() {
        return includedConfigurationNames;
    }

    public List<String> getExcludedProjectPaths() {
        return excludedProjectPaths;
    }

    public List<String> getIncludedProjectPaths() {
        return includedProjectPaths;
    }

    public boolean isRootOnly() {
        return rootOnly;
    }
}
