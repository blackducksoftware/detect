package com.blackduck.integration.detectable.detectables.nuget;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

public class NugetInspectorOptions {
    private final boolean ignoreFailures;
    private final List<String> excludedModules;
    private final List<String> includedModules;
    private final List<String> packagesRepoUrl;
    private final Path nugetConfigPath;
    private final Set<NugetDependencyType> nugetExcludedDependencyTypes;
    private final Path nugetArtifactsPath;

    public NugetInspectorOptions(boolean ignoreFailures, List<String> excludedModules, List<String> includedModules, List<String> packagesRepoUrl, @Nullable Path nugetConfigPath, Set<NugetDependencyType> nugetExcludedDependencyTypes, @Nullable Path nugetArtifactsPath) {
        this.ignoreFailures = ignoreFailures;
        this.excludedModules = excludedModules;
        this.includedModules = includedModules;
        this.packagesRepoUrl = packagesRepoUrl;
        this.nugetConfigPath = nugetConfigPath;
        this.nugetExcludedDependencyTypes = nugetExcludedDependencyTypes;
        this.nugetArtifactsPath = nugetArtifactsPath;
    }

    public boolean isIgnoreFailures() {
        return ignoreFailures;
    }

    public List<String> getExcludedModules() {
        return excludedModules;
    }

    public List<String> getIncludedModules() {
        return includedModules;
    }

    public List<String> getPackagesRepoUrl() {
        return packagesRepoUrl;
    }

    public Optional<Path> getNugetConfigPath() {
        return Optional.ofNullable(nugetConfigPath);
    }

    public Set<NugetDependencyType> getNugetExcludedDependencyTypes() { return nugetExcludedDependencyTypes; }

    public Optional<Path> getNugetArtifactsPath() { return Optional.ofNullable(nugetArtifactsPath); }
}
