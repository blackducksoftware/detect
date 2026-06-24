package com.blackduck.integration.detectable.detectables.npm.lockfile;

import java.util.List;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.npm.NpmDependencyType;

// TODO: Identical to NpmPackageJsonParseDetectableOptions. Similar to NpmCliExtractorOptions. Common base Options class? JM-01/2022
public class NpmLockfileOptions {
    private final EnumListFilter<NpmDependencyType> npmDependencyTypeFilter;
    private final List<String> excludedWorkspaceNames;
    private final List<String> includedWorkspaceNames;

    public NpmLockfileOptions(EnumListFilter<NpmDependencyType> npmDependencyTypeFilter) {
        this(npmDependencyTypeFilter, java.util.Collections.emptyList(), java.util.Collections.emptyList());
    }

    public NpmLockfileOptions(
            EnumListFilter<NpmDependencyType> npmDependencyTypeFilter,
            List<String> excludedWorkspaceNames,
            List<String> includedWorkspaceNames) {
        this.npmDependencyTypeFilter = npmDependencyTypeFilter;
        this.excludedWorkspaceNames = excludedWorkspaceNames;
        this.includedWorkspaceNames = includedWorkspaceNames;
    }

    public EnumListFilter<NpmDependencyType> getNpmDependencyTypeFilter() {
        return npmDependencyTypeFilter;
    }

    public List<String> getExcludedWorkspaceNames() {
        return excludedWorkspaceNames;
    }

    public List<String> getIncludedWorkspaceNames() {
        return includedWorkspaceNames;
    }
}
