package com.blackduck.integration.detectable.detectables.cargo;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;

import java.util.ArrayList;
import java.util.List;

public class CargoDetectableOptions {
    private final EnumListFilter<CargoDependencyType> dependencyTypeFilter;
    private final boolean cargoIgnoreAllWorkspacesMode;
    private final List<String> excludedWorkspaces;

    public CargoDetectableOptions(EnumListFilter<CargoDependencyType> dependencyTypeFilter,
                                  boolean cargoIgnoreAllWorkspacesMode, List<String> excludedWorkspaces) {
        this.dependencyTypeFilter = dependencyTypeFilter;
        this.cargoIgnoreAllWorkspacesMode = cargoIgnoreAllWorkspacesMode;
        this.excludedWorkspaces = excludedWorkspaces;
    }

    public CargoDetectableOptions(EnumListFilter<CargoDependencyType> dependencyTypeFilter,
                                  boolean cargoIgnoreAllWorkspacesMode) {
        this(dependencyTypeFilter, cargoIgnoreAllWorkspacesMode, new ArrayList<>());
    }

    public CargoDetectableOptions(EnumListFilter<CargoDependencyType> dependencyTypeFilter) {
        this(dependencyTypeFilter, false, new ArrayList<>());
    }

    public EnumListFilter<CargoDependencyType> getDependencyTypeFilter() {
        return dependencyTypeFilter;
    }

    public boolean getCargoIgnoreAllWorkspacesMode() {
        return cargoIgnoreAllWorkspacesMode;
    }

    public List<String> getExcludedWorkspaces() {
        return excludedWorkspaces;
    }
}
