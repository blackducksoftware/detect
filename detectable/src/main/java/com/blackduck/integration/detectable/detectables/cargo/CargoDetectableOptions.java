package com.blackduck.integration.detectable.detectables.cargo;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;

public class CargoDetectableOptions {
    private final EnumListFilter<CargoDependencyType> dependencyTypeFilter;

    public CargoDetectableOptions(EnumListFilter<CargoDependencyType> dependencyTypeFilter) {
        this.dependencyTypeFilter = dependencyTypeFilter;
    }

    public EnumListFilter<CargoDependencyType> getDependencyTypeFilter() {
        return dependencyTypeFilter;
    }
}
