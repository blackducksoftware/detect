package com.blackduck.integration.detectable.detectables.cargo;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;

import java.util.ArrayList;
import java.util.List;

public class CargoDetectableOptions {
    private final EnumListFilter<CargoDependencyType> dependencyTypeFilter;
    private final boolean cargoIgnoreAllWorkspacesMode;
    private final boolean isDefaultFeaturesDisabled;
    private final List<String> excludedWorkspaces;
    private final List<String> includedWorkspaces;
    private final List<String> includedFeatures;

    public CargoDetectableOptions(EnumListFilter<CargoDependencyType> dependencyTypeFilter,
                                  boolean cargoIgnoreAllWorkspacesMode,
                                  boolean isDefaultFeaturesDisabled,
                                  List<String> includedWorkspaces,
                                  List<String> excludedWorkspaces,
                                  List<String> includedFeatures) {
        this.dependencyTypeFilter = dependencyTypeFilter;
        this.cargoIgnoreAllWorkspacesMode = cargoIgnoreAllWorkspacesMode;
        this.isDefaultFeaturesDisabled = isDefaultFeaturesDisabled;
        this.includedWorkspaces = includedWorkspaces;
        this.excludedWorkspaces = excludedWorkspaces;
        this.includedFeatures = includedFeatures;
    }

    public CargoDetectableOptions(EnumListFilter<CargoDependencyType> dependencyTypeFilter) {
        this(dependencyTypeFilter, false, false, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
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

    public List<String> getIncludedFeatures() {
        return includedFeatures;
    }

    public boolean isDefaultFeaturesDisabled() {
        return isDefaultFeaturesDisabled;
    }
}
