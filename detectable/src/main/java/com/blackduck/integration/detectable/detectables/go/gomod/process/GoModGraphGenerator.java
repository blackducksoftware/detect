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

    public CodeLocation generateGraph(GoListModule projectModule, GoRelationshipManager goRelationshipManager, GoModDependencyManager goModDependencyManager, Set<String> excludedModules) {
        DependencyGraph graph = new BasicDependencyGraph();
        String mainModuleName = projectModule.getPath();
        NameVersion mainModuleNameVersion = new NameVersion(mainModuleName, projectModule.getVersion());
        if (goRelationshipManager.hasRelationshipsFor(mainModuleNameVersion)) {
            goRelationshipManager.getRelationshipsFor(mainModuleNameVersion).stream()
                .map(relationship -> relationship.getChild())
                .forEach(childNameVersion -> addModuleToGraph(childNameVersion, null, graph, goRelationshipManager, goModDependencyManager));
        }

        addOrphanModules(graph, goModDependencyManager, excludedModules, mainModuleNameVersion);

        return new CodeLocation(graph, externalIdFactory.createNameVersionExternalId(Forge.GOLANG, projectModule.getPath(), projectModule.getVersion()));
    }

    private void addOrphanModules(DependencyGraph graph, GoModDependencyManager goModDependencyManager, Set<String> excludedModules, NameVersion mainModuleNameVersion) {
        // quick check areThereAnyOrphansToAdd()
        // skip main
        // skip unused or vendored IF flag was set. soooo we need excluded modules.
        for (Dependency requiredDependency : goModDependencyManager.getRequiredDependencies()) {
            if (!graph.hasDependency(requiredDependency) && !excludedModules.contains(requiredDependency.getName()) && isNotMainModule(requiredDependency.getName(), requiredDependency.getVersion(), mainModuleNameVersion)) {
                logger.debug("Adding orphan module '{}' as a direct dependency because no parent was found.", requiredDependency.getName());
                graph.addDirectDependency(requiredDependency);
            }
        }
    }

    private boolean isNotMainModule(String name, String version, NameVersion mainModuleNameVersion) {
        return !(name.equalsIgnoreCase(mainModuleNameVersion.getName()) && version == null);
    }

    private void addModuleToGraph(
        NameVersion moduleNameVersion,
        @Nullable Dependency parent,
        DependencyGraph graph,
        GoRelationshipManager goRelationshipManager,
        GoModDependencyManager goModDependencyManager
    ) {
        if (goRelationshipManager.isModuleExcluded(moduleNameVersion.getName())) {
            logger.debug("Excluding module '{}' because it is not used by the main module.", moduleNameVersion.getName());
            return;
        }

        Dependency dependency = goModDependencyManager.getDependencyForModule(moduleNameVersion.getName());
        // To prevent false positives, always grab the version of the module chosen by Go's minimal version selection
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
