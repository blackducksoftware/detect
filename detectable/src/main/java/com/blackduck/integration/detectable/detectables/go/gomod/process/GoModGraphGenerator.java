package com.blackduck.integration.detectable.detectables.go.gomod.process;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.blackduck.integration.util.NameVersion;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.go.gomod.model.GoGraphRelationship;
import com.blackduck.integration.detectable.detectables.go.gomod.model.GoListModule;

public class GoModGraphGenerator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ExternalIdFactory externalIdFactory;
    private final Set<NameVersion> fullyGraphedModules = new HashSet<>();

    public GoModGraphGenerator(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public CodeLocation generateGraph(GoListModule projectModule, GoRelationshipManager goRelationshipManager, GoModDependencyManager goModDependencyManager) {
        DependencyGraph graph = new BasicDependencyGraph();
        String moduleName = projectModule.getPath();
        NameVersion moduleNameVersion = new NameVersion(moduleName, projectModule.getVersion());
        if (goRelationshipManager.hasRelationshipsFor(moduleNameVersion)) {
            goRelationshipManager.getRelationshipsFor(moduleNameVersion).stream() // ideally this traverses over each direct dep, but that's not true if the go mog graph just says "(main module does not need .. module)"
                .map(relationship -> relationship.getChild())
                .forEach(childNameVersion -> addModuleToGraph(childNameVersion, null, graph, goRelationshipManager, goModDependencyManager));
        }

        return new CodeLocation(graph, externalIdFactory.createNameVersionExternalId(Forge.GOLANG, projectModule.getPath(), projectModule.getVersion()));
    }

    private void addModuleToGraph(
        NameVersion moduleNameVersion,
        @Nullable Dependency parent,
        DependencyGraph graph,
        GoRelationshipManager goRelationshipManager,
        GoModDependencyManager goModDependencyManager
    ) {
        if (goRelationshipManager.isModuleExcluded(moduleNameVersion.getName())) { // keeping that method call the same to indicate we exclude by name not name and version. pretty sure excluded modules dont come with version? somewhere in the go mod outputs they also have no version i think?
            logger.debug("Excluding module '{}' because it is not used by the main module.", moduleNameVersion.getName()); // confirm excluded modules are not impacted.. modules == deps?
            return;
        }

        Dependency dependency = goModDependencyManager.getDependencyForModule(moduleNameVersion.getName());
        NameVersion moduleNameSelectedVersion = new NameVersion(dependency.getName(), goModDependencyManager.getOriginalVersionFromKbCompatibleVersion(dependency.getVersion()));

        if (parent != null) {
            graph.addChildWithParent(dependency, parent);
        } else {
            graph.addDirectDependency(dependency);
        }

        if (!fullyGraphedModules.contains(moduleNameSelectedVersion) && goRelationshipManager.hasRelationshipsFor(moduleNameSelectedVersion)) {
            fullyGraphedModules.add(moduleNameSelectedVersion);
            List<GoGraphRelationship> projectRelationships = goRelationshipManager.getRelationshipsFor(moduleNameSelectedVersion);
            for (GoGraphRelationship projectRelationship : projectRelationships) {
                addModuleToGraph(projectRelationship.getChild(), dependency, graph, goRelationshipManager, goModDependencyManager);
            }
        }
    }
}
