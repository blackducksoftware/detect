package com.blackduck.integration.detectable.detectables.npm.packagejson;

import java.util.Collections;
import java.util.List;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.npm.NpmDependencyType;

public class NpmPackageJsonParseDetectableOptions {
    private final EnumListFilter<NpmDependencyType> npmDependencyTypeFilter;
    private final List<String> excludedWorkspaceNames;
    private final List<String> includedWorkspaceNames;
    private final boolean ignoreAllWorkspaces;

    public NpmPackageJsonParseDetectableOptions(EnumListFilter<NpmDependencyType> npmDependencyTypeFilter) {
        this(npmDependencyTypeFilter, Collections.emptyList(), Collections.emptyList(), false);
    }

    public NpmPackageJsonParseDetectableOptions(
            EnumListFilter<NpmDependencyType> npmDependencyTypeFilter,
            List<String> excludedWorkspaceNames,
            List<String> includedWorkspaceNames,
            boolean ignoreAllWorkspaces) {
        this.npmDependencyTypeFilter = npmDependencyTypeFilter;
        this.excludedWorkspaceNames = excludedWorkspaceNames;
        this.includedWorkspaceNames = includedWorkspaceNames;
        this.ignoreAllWorkspaces = ignoreAllWorkspaces;
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

    public boolean isIgnoreAllWorkspaces() {
        return ignoreAllWorkspaces;
    }
}
