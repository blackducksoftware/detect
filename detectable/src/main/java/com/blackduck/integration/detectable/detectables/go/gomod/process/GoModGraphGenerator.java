package com.blackduck.integration.detectable.detectables.go.gomod.process;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final Set<String> fullyGraphedModules = new HashSet<>();

    public GoModGraphGenerator(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public CodeLocation generateGraph(GoListModule projectModule, GoRelationshipManager goRelationshipManager, GoModDependencyManager goModDependencyManager) {
        DependencyGraph graph = new BasicDependencyGraph();
        String moduleName = projectModule.getPath(); // version is null for main module (commons-service) What is path?
        if (goRelationshipManager.hasRelationshipsFor(moduleName)) { // has to be name version, but main module doesnt have version
            goRelationshipManager.getRelationshipsFor(moduleName).stream()
                .map(relationship -> relationship.getChild().getName())
                .forEach(childName -> addModuleToGraph(childName, null, graph, goRelationshipManager, goModDependencyManager));
        }

        return new CodeLocation(graph, externalIdFactory.createNameVersionExternalId(Forge.GOLANG, projectModule.getPath(), projectModule.getVersion()));
    }

    private void addModuleToGraph( // recursiveeeeeeeeeee -- first time we call this, parent is null.
        String moduleName,
        @Nullable Dependency parent,
        DependencyGraph graph,
        GoRelationshipManager goRelationshipManager,
        GoModDependencyManager goModDependencyManager
    ) {
        if (goRelationshipManager.isNotUsedByMainModule(moduleName)) {
            logger.debug("Excluding module '{}' because it is not used by the main module.", moduleName); // confirm excluded modules are not impacted.. modules == deps?
            return;
        }

        Dependency dependency = goModDependencyManager.getDependencyForModule(moduleName); // just the name, what?
        if (parent != null) {
            graph.addChildWithParent(dependency, parent);
        } else {
            graph.addDirectDependency(dependency); // do we do this for anyone except main module?
        }

        if (!fullyGraphedModules.contains(moduleName) && goRelationshipManager.hasRelationshipsFor(moduleName)) {
            fullyGraphedModules.add(moduleName);
            List<GoGraphRelationship> projectRelationships = goRelationshipManager.getRelationshipsFor(moduleName);
            for (GoGraphRelationship projectRelationship : projectRelationships) {
                addModuleToGraph(projectRelationship.getChild().getName(), dependency, graph, goRelationshipManager, goModDependencyManager);
            }
        }
    }
}
