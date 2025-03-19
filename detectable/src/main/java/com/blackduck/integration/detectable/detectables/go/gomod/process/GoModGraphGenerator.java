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
        String moduleName = projectModule.getPath(); // version is null for main module (commons-service) What is path?
        // we could grab name and version from projectModule. version is null though. Might not always be the case.
        NameVersion moduleNameVersion = new NameVersion(moduleName, projectModule.getVersion());
        if (goRelationshipManager.hasRelationshipsForNEW(moduleNameVersion)) { // has to be name version, but main module doesnt have version
            goRelationshipManager.getRelationshipsForNEW(moduleNameVersion).stream()
                .map(relationship -> relationship.getChild())
                .forEach(childNameVersion -> addModuleToGraph(childNameVersion, null, graph, goRelationshipManager, goModDependencyManager));
        }

        return new CodeLocation(graph, externalIdFactory.createNameVersionExternalId(Forge.GOLANG, projectModule.getPath(), projectModule.getVersion()));
    }

    private void addModuleToGraph( // recursiveeeeeeeeeee -- first time we call this, parent is null.
        NameVersion moduleNameVersion, // need to change this to NameVersion
        @Nullable Dependency parent,
        DependencyGraph graph,
        GoRelationshipManager goRelationshipManager,
        GoModDependencyManager goModDependencyManager
    ) {
        if (goRelationshipManager.isNotUsedByMainModule(moduleNameVersion.getName())) { // keeping that method call the same to indicate we exclude by name not name and version. pretty sure excluded modules dont come with version? somewhere in the go mod outputs they also have no version i think?
            logger.debug("Excluding module '{}' because it is not used by the main module.", moduleNameVersion.getName()); // confirm excluded modules are not impacted.. modules == deps?
            return;
        }

        Dependency dependency = goModDependencyManager.getDependencyForModuleNameVersion(moduleNameVersion); // oooookay several layers deep
        if (parent != null) {
            graph.addChildWithParent(dependency, parent);
        } else {
            graph.addDirectDependency(dependency);
        }

        if (!fullyGraphedModules.contains(moduleNameVersion) && goRelationshipManager.hasRelationshipsForNEW(moduleNameVersion)) {
            fullyGraphedModules.add(moduleNameVersion);
            List<GoGraphRelationship> projectRelationships = goRelationshipManager.getRelationshipsForNEW(moduleNameVersion);
            for (GoGraphRelationship projectRelationship : projectRelationships) {
                addModuleToGraph(projectRelationship.getChild(), dependency, graph, goRelationshipManager, goModDependencyManager);
            }
        }
    }
}
