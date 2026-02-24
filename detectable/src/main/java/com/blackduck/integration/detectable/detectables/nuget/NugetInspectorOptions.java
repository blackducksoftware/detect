package com.blackduck.integration.detectable.detectables.nuget;

import java.io.File;
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
    private final Path inspectedFilesInfoPath;
    private final File nugetInspectorPath;

    private NugetInspectorOptions(Builder builder) {
        this.ignoreFailures = builder.ignoreFailures;
        this.excludedModules = builder.excludedModules;
        this.includedModules = builder.includedModules;
        this.packagesRepoUrl = builder.packagesRepoUrl;
        this.nugetConfigPath = builder.nugetConfigPath;
        this.nugetExcludedDependencyTypes = builder.nugetExcludedDependencyTypes;
        this.nugetArtifactsPath = builder.nugetArtifactsPath;
        this.inspectedFilesInfoPath = builder.inspectedFilesInfoPath;
        this.nugetInspectorPath = builder.nugetInspectorPath;
    }

    public static class Builder {
        private boolean ignoreFailures;
        private List<String> excludedModules;
        private List<String> includedModules;
        private List<String> packagesRepoUrl;
        private Path nugetConfigPath;
        private Set<NugetDependencyType> nugetExcludedDependencyTypes;
        private Path nugetArtifactsPath;
        private Path inspectedFilesInfoPath;
        private File nugetInspectorPath;

        public Builder ignoreFailures(boolean ignoreFailures) {
            this.ignoreFailures = ignoreFailures;
            return this;
        }
        public Builder excludedModules(List<String> excludedModules) {
            this.excludedModules = excludedModules;
            return this;
        }
        public Builder includedModules(List<String> includedModules) {
            this.includedModules = includedModules;
            return this;
        }
        public Builder packagesRepoUrl(List<String> packagesRepoUrl) {
            this.packagesRepoUrl = packagesRepoUrl;
            return this;
        }
        public Builder nugetConfigPath(@Nullable Path nugetConfigPath) {
            this.nugetConfigPath = nugetConfigPath;
            return this;
        }
        public Builder nugetExcludedDependencyTypes(Set<NugetDependencyType> nugetExcludedDependencyTypes) {
            this.nugetExcludedDependencyTypes = nugetExcludedDependencyTypes;
            return this;
        }
        public Builder nugetArtifactsPath(@Nullable Path nugetArtifactsPath) {
            this.nugetArtifactsPath = nugetArtifactsPath;
            return this;
        }
        public Builder inspectedFilesInfoPath(@Nullable Path inspectedFilesInfoPath) {
            this.inspectedFilesInfoPath = inspectedFilesInfoPath;
            return this;
        }
        public Builder nugetInspectorPath(@Nullable File nugetInspectorPath) {
            this.nugetInspectorPath = nugetInspectorPath;
            return this;
        }
        public NugetInspectorOptions build() {
            return new NugetInspectorOptions(this);
        }
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

    public Optional<Path> getInspectedFilesInfoPath() { return Optional.ofNullable(inspectedFilesInfoPath); }

    public Optional<File> getNugetInspectorPath() {
        return Optional.ofNullable(nugetInspectorPath);
    }
}
