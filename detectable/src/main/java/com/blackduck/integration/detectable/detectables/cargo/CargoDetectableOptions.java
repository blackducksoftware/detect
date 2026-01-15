package com.blackduck.integration.detectable.detectables.cargo;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;

import java.util.ArrayList;
import java.util.List;

public class CargoDetectableOptions {
    private final EnumListFilter<CargoDependencyType> dependencyTypeFilter;
    private final boolean cargoIgnoreAllWorkspacesMode;
    private final List<String> excludedWorkspaces;
    private final List<String> includedWorkspaces;

    public CargoDetectableOptions(EnumListFilter<CargoDependencyType> dependencyTypeFilter,
                                  boolean cargoIgnoreAllWorkspacesMode,
                                  List<String> includedWorkspaces,
                                  List<String> excludedWorkspaces) {
        this.dependencyTypeFilter = dependencyTypeFilter;
        this.cargoIgnoreAllWorkspacesMode = cargoIgnoreAllWorkspacesMode;
        this.includedWorkspaces = includedWorkspaces;
        this.excludedWorkspaces = excludedWorkspaces;
    }

    public CargoDetectableOptions(EnumListFilter<CargoDependencyType> dependencyTypeFilter) {
        this(dependencyTypeFilter, false, new ArrayList<>(), new ArrayList<>());
    }

    public EnumListFilter<CargoDependencyType> getDependencyTypeFilter() {
        return dependencyTypeFilter;
    }

    public boolean getCargoIgnoreAllWorkspacesMode() {
        return cargoIgnoreAllWorkspacesMode;
    }

    public List<String> getIncludedWorkspaces() {
        return includedWorkspaces;
    }

    public List<String> getExcludedWorkspaces() {
        return excludedWorkspaces;
    }
}
