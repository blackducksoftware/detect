package com.blackduck.integration.detectable.detectables.rush;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;

import java.util.List;

public class RushOptions {
    private final EnumListFilter<RushProjects> rushProjectTypeFilter;
    private final List<String> excludedPackages;
    private final List<String> includedPackages;

    public RushOptions(EnumListFilter<RushProjects> rushProjectTypeFilter, List<String> excludedPackages, List<String> includedPackages) {
        this.rushProjectTypeFilter = rushProjectTypeFilter;
        this.excludedPackages = excludedPackages;
        this.includedPackages = includedPackages;
    }

    public EnumListFilter<RushProjects> getRushProjectTypeFilter() {
        return rushProjectTypeFilter;
    }

    public List<String> getExcludedPackages() {
        return excludedPackages;
    }

    public List<String> getIncludedPackages() {
        return includedPackages;
    }
}
