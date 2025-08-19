package com.blackduck.integration.detectable.detectables.pnpm.lockfile;

import java.util.List;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmDependencyType;

public class PnpmLockOptions {
    private final EnumListFilter<PnpmDependencyType> dependencyTypeFilter;
    private final List<String> excludedDirectories;
    private final List<String> includedDirectories;

    public PnpmLockOptions(
            EnumListFilter<PnpmDependencyType> dependencyTypeFilter, 
            List<String> excludedDirectories, 
            List<String> includedDirectories) {
        this.dependencyTypeFilter = dependencyTypeFilter;
        this.excludedDirectories = excludedDirectories;
        this.includedDirectories = includedDirectories;
    }

    public EnumListFilter<PnpmDependencyType> getDependencyTypeFilter() {
        return dependencyTypeFilter;
    }
    
    public List<String> getExcludedDirectories() {
        return excludedDirectories;
    }

    public List<String> getIncludedDirectories() {
        return includedDirectories;
    }
}
