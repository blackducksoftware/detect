package com.blackduck.integration.detectable.detectables.cargo;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;

public class CargoDetectableOptions {
    private final EnumListFilter<CargoDependencyType> dependencyTypeFilter;
    private final boolean cargoIgnoreAllWorkspacesMode;

    public CargoDetectableOptions(EnumListFilter<CargoDependencyType> dependencyTypeFilter,
                                  boolean cargoIgnoreAllWorkspacesMode) {
        this.dependencyTypeFilter = dependencyTypeFilter;
        this.cargoIgnoreAllWorkspacesMode = cargoIgnoreAllWorkspacesMode;
    }

    public EnumListFilter<CargoDependencyType> getDependencyTypeFilter() {
        return dependencyTypeFilter;
    }

    public Boolean getCargoIgnoreAllWorkspacesMode() {
        return cargoIgnoreAllWorkspacesMode;
    }
}
