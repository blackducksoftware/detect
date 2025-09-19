package com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model;

import java.util.List;
import java.util.Set;

/**
 * Represents the parsed content of a go.mod file with all directives.
 */
public class GoModFileContent {
    private final String moduleName;
    private final String goVersion;
    private final String toolchainVersion;
    private final List<GoModuleInfo> directDependencies;
    private final List<GoModuleInfo> indirectDependencies;
    private final Set<GoModuleInfo> excludedModules;
    private final List<GoReplaceDirective> replaceDirectives;
    private final Set<GoModuleInfo> retractedVersions;

    public GoModFileContent(
            String moduleName,
            String goVersion,
            String toolchainVersion,
            List<GoModuleInfo> directDependencies,
            List<GoModuleInfo> indirectDependencies,
            Set<GoModuleInfo> excludedModules,
            List<GoReplaceDirective> replaceDirectives,
            Set<GoModuleInfo> retractedVersions) {
        this.moduleName = moduleName;
        this.goVersion = goVersion;
        this.toolchainVersion = toolchainVersion;
        this.directDependencies = directDependencies;
        this.indirectDependencies = indirectDependencies;
        this.excludedModules = excludedModules;
        this.replaceDirectives = replaceDirectives;
        this.retractedVersions = retractedVersions;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getGoVersion() {
        return goVersion;
    }

    public String getToolchainVersion() {
        return toolchainVersion;
    }

    public List<GoModuleInfo> getDirectDependencies() {
        return directDependencies;
    }

    public List<GoModuleInfo> getIndirectDependencies() {
        return indirectDependencies;
    }

    public Set<GoModuleInfo> getExcludedModules() {
        return excludedModules;
    }

    public List<GoReplaceDirective> getReplaceDirectives() {
        return replaceDirectives;
    }

    public Set<GoModuleInfo> getRetractedVersions() {
        return retractedVersions;
    }

    @Override
    public String toString() {
        return "GoModFileContent{" +
                "moduleName='" + moduleName + '\'' +
                ", goVersion='" + goVersion + '\'' +
                ", toolchainVersion='" + toolchainVersion + '\'' +
                ", directDependencies=" + directDependencies.size() +
                ", indirectDependencies=" + indirectDependencies.size() +
                ", excludedModules=" + excludedModules.size() +
                ", replaceDirectives=" + replaceDirectives.size() +
                ", retractedVersions=" + retractedVersions.size() +
                '}';
    }
}
