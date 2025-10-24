package com.blackduck.integration.detectable.detectables.go.gomod.process;

import com.blackduck.integration.util.NameVersion;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.go.gomod.model.GoListAllData;
import com.blackduck.integration.detectable.detectables.go.gomod.model.ReplaceData;
import com.blackduck.integration.detectable.util.KBComponentHelpers;

public class GoModDependencyManager {
    private final ExternalIdFactory externalIdFactory;
    private final Map<String, Dependency> modulesAsDependencies;
    private final Map<String, NameVersion> originalRequiredNameAndVersion;
    private final KBComponentHelpers kbComponentHelpers = new KBComponentHelpers();

    public GoModDependencyManager(List<GoListAllData> allRequiredModules, ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
        originalRequiredNameAndVersion = new HashMap<>();
        modulesAsDependencies = convertModulesToDependencies(allRequiredModules);

    }

    /**
     * A Go Module consists of a path (aka its name) and its version. Before converting to a Dependency, we apply any
     * replace directives and commit hash version truncation when applicable.
     * @param allModules
     * @return
     */
    private Map<String, Dependency> convertModulesToDependencies(List<GoListAllData> allModules) {
        Map<String, Dependency> dependencies = new HashMap<>();

        for (GoListAllData module : allModules) {
            // Keep track of original module path and version (without replacements, without truncating commit hash version)
            NameVersion originalNameVersion = new NameVersion(module.getPath(), module.getVersion());
            originalRequiredNameAndVersion.putIfAbsent(module.getPath(), originalNameVersion);

            // Apply replacements and version truncation if applicable
            String name = Optional.ofNullable(module.getReplace())
                .map(ReplaceData::getPath)
                .orElse(module.getPath());
            String version = Optional.ofNullable(module.getReplace())
                .map(ReplaceData::getVersion)
                .orElse(module.getVersion());
            if (version != null) {
                String kbCompatibleVersion = kbComponentHelpers.getKbCompatibleVersion(version);
                if (!version.equals(kbCompatibleVersion)) {
                    version = kbCompatibleVersion;
                }
            }

            //  Add dependency (will have replaced name/version + truncated commit hash if applicable)
            dependencies.put(module.getPath(), convertToDependency(name, version));
        }

        return dependencies;
    }

    /**
     * @param moduleName The module name used for storing relationships that are used for graph building.
     * @return The module name and version before any replace directives were applied. If version was modified
     * for KB compatibility, this will return the version before any commit hash truncation was applied.
     */
    public NameVersion getOriginalRequiredNameAndVersion(String moduleName) {
           return originalRequiredNameAndVersion.getOrDefault(moduleName, new NameVersion("", ""));
    }

    public Collection<Dependency> getRequiredDependencies() {
        return modulesAsDependencies.values();
    }

    public Dependency getDependencyForModule(String moduleName) {
        return modulesAsDependencies.getOrDefault(moduleName, convertToDependency(moduleName, null));
    }

    private Dependency convertToDependency(String moduleName, @Nullable String moduleVersion) {
        return new Dependency(moduleName, moduleVersion, externalIdFactory.createNameVersionExternalId(Forge.GOLANG, moduleName, moduleVersion), null);
    }
}
