package com.blackduck.integration.detectable.detectables.yarn;

import java.util.List;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;

public class YarnLockOptions {
    private final EnumListFilter<YarnDependencyType> yarnDependencyTypeFilter;
    private final List<String> excludedWorkspaceNamePatterns;
    private final List<String> includedWorkspaceNamePatterns;
    private final Boolean yarnIgnoreAllWorkspacesMode;

    public YarnLockOptions(
            EnumListFilter<YarnDependencyType> yarnDependencyTypeFilter, 
            List<String> excludedWorkspaceNamePatterns, 
            List<String> includedWorkspaceNamePatterns,
            Boolean yarnIgnoreAllWorkspacesMode) {
        this.yarnDependencyTypeFilter = yarnDependencyTypeFilter;
        this.excludedWorkspaceNamePatterns = excludedWorkspaceNamePatterns;
        this.includedWorkspaceNamePatterns = includedWorkspaceNamePatterns;
        this.yarnIgnoreAllWorkspacesMode = yarnIgnoreAllWorkspacesMode;
    }

    public EnumListFilter<YarnDependencyType> getYarnDependencyTypeFilter() {
        return yarnDependencyTypeFilter;
    }

    public List<String> getExcludedWorkspaceNamePatterns() {
        return excludedWorkspaceNamePatterns;
    }

    public List<String> getIncludedWorkspaceNamePatterns() {
        return includedWorkspaceNamePatterns;
    }
    
    public Boolean getYarnIgnoreAllWorkspacesMode() {
        return yarnIgnoreAllWorkspacesMode;
    }
}
