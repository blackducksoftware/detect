package com.blackduck.integration.detectable.detectables.npm.cli;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.npm.NpmDependencyType;

public class NpmCliExtractorOptions {
    private final EnumListFilter<NpmDependencyType> npmDependencyTypeFilter;
    private final String npmArguments;
    private final List<String> excludedWorkspaceNames;
    private final List<String> includedWorkspaceNames;

    public NpmCliExtractorOptions(EnumListFilter<NpmDependencyType> npmDependencyTypeFilter,
            String npmArguments) {
        this(npmDependencyTypeFilter, npmArguments,
            Collections.emptyList(), Collections.emptyList());
    }

    public NpmCliExtractorOptions(EnumListFilter<NpmDependencyType> npmDependencyTypeFilter,
            String npmArguments,
            List<String> excludedWorkspaceNames,
            List<String> includedWorkspaceNames) {
        this.npmDependencyTypeFilter = npmDependencyTypeFilter;
        this.npmArguments = npmArguments;
        this.excludedWorkspaceNames = excludedWorkspaceNames;
        this.includedWorkspaceNames = includedWorkspaceNames;
    }

    public EnumListFilter<NpmDependencyType> getDependencyTypeFilter() {
        return npmDependencyTypeFilter;
    }

    public Optional<String> getNpmArguments() {
        return Optional.ofNullable(npmArguments);
    }

    public List<String> getExcludedWorkspaceNames() {
        return excludedWorkspaceNames;
    }

    public List<String> getIncludedWorkspaceNames() {
        return includedWorkspaceNames;
    }
}
