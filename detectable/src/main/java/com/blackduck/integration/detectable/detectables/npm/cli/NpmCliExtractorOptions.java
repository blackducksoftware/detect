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
    private final boolean ignoreAllWorkspaces;

    public NpmCliExtractorOptions(EnumListFilter<NpmDependencyType> npmDependencyTypeFilter,
            String npmArguments) {
        this(npmDependencyTypeFilter, npmArguments,
            Collections.emptyList(), Collections.emptyList(), false);
    }

    public NpmCliExtractorOptions(EnumListFilter<NpmDependencyType> npmDependencyTypeFilter,
            String npmArguments,
            List<String> excludedWorkspaceNames,
            List<String> includedWorkspaceNames) {
        this(npmDependencyTypeFilter, npmArguments, excludedWorkspaceNames, includedWorkspaceNames, false);
    }

    public NpmCliExtractorOptions(EnumListFilter<NpmDependencyType> npmDependencyTypeFilter,
            String npmArguments,
            List<String> excludedWorkspaceNames,
            List<String> includedWorkspaceNames,
            boolean ignoreAllWorkspaces) {
        this.npmDependencyTypeFilter = npmDependencyTypeFilter;
        this.npmArguments = npmArguments;
        this.excludedWorkspaceNames = excludedWorkspaceNames;
        this.includedWorkspaceNames = includedWorkspaceNames;
        this.ignoreAllWorkspaces = ignoreAllWorkspaces;
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

    public boolean isIgnoreAllWorkspaces() {
        return ignoreAllWorkspaces;
    }
}
