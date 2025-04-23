package com.blackduck.integration.detectable.detectables.cargo;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;

public class CargoCliDetectableOptions {
    private final EnumListFilter<CargoDependencyType> dependencyTypeFilter;

    public CargoCliDetectableOptions(EnumListFilter<CargoDependencyType> dependencyTypeFilter) {
        this.dependencyTypeFilter = dependencyTypeFilter;
    }

    public EnumListFilter<CargoDependencyType> getDependencyTypeFilter() {
        return dependencyTypeFilter;
    }
}
