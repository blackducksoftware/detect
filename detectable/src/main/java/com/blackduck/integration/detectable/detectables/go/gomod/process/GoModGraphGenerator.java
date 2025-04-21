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

        addOrphanModules(graph, goModDependencyManager, goRelationshipManager, excludedModules, mainModuleNameVersion);

        return new CodeLocation(graph, externalIdFactory.createNameVersionExternalId(Forge.GOLANG, projectModule.getPath(), projectModule.getVersion()));
    }

    /**
     * An orphan is a required Go module whose parent cannot be determined from the outputs of 'go mod why' or 'go mod graph'
     * @param graph
     * @param goModDependencyManager
     * @param excludedModules vendored or unused modules
     * @param mainModuleNameVersion
     */
    private void addOrphanModules(DependencyGraph graph, GoModDependencyManager goModDependencyManager, GoRelationshipManager goRelationshipManager, Set<String> excludedModules, NameVersion mainModuleNameVersion) {
        // TODO a quick check afirst reThereAnyOrphansToAddInTheFirstPlace() to improve performance (check if graph has the same # of components as the # of required modules?)
        for (Dependency requiredDependency : goModDependencyManager.getRequiredDependencies()) {
            NameVersion requiredDepNameVersion = new NameVersion(requiredDependency.getName(), requiredDependency.getVersion());
            if (!graph.hasDependency(requiredDependency) && !excludedModules.contains(requiredDependency.getName())
                    && isNotMainModule(requiredDependency.getName(), requiredDependency.getVersion(), mainModuleNameVersion)
                    && !goRelationshipManager.childExcludedForGoodReason(requiredDepNameVersion)) {
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
        if (goRelationshipManager.isModuleExcluded(moduleNameVersion)) {
            logger.debug("Excluding module '{}' because it is not used by the main module. All children of this module will also be excluded.", moduleNameVersion.getName());
            // Before returning we should make note of all the children of this excluded module so we do not assume they are true orphans at a later step and incorrectly add them back to the graph
            goRelationshipManager.addChildrenToExcludedModules(moduleNameVersion);
            return;
        }

        // To prevent false positives, always grab the version of the module chosen by Go's minimal version selection.
        Dependency dependency = goModDependencyManager.getDependencyForModule(moduleNameVersion.getName()); // Always returns the replaced path and version if replace directives were applied.
        NameVersion originalNameAndVersion = goModDependencyManager.getOriginalRequiredNameAndVersion(moduleNameVersion.getName());

        if (parent != null) {
            graph.addChildWithParent(dependency, parent);
        } else {
            graph.addDirectDependency(dependency);
        }

        if (!fullyGraphedModules.contains(moduleNameVersion) && goRelationshipManager.hasRelationshipsFor(originalNameAndVersion)) {
            fullyGraphedModules.add(moduleNameVersion);
            List<GoGraphRelationship> projectRelationships = goRelationshipManager.getRelationshipsFor(originalNameAndVersion);
            for (GoGraphRelationship projectRelationship : projectRelationships) {
                addModuleToGraph(projectRelationship.getChild(), dependency, graph, goRelationshipManager, goModDependencyManager);
            }
        }
    }
}
