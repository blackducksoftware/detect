package com.blackduck.integration.detectable.detectables.go.gomod.process;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.blackduck.integration.bdio.model.externalid.ExternalId;
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
        if (goRelationshipManager.hasRelationshipsForNEW(moduleNameVersion)) {
            goRelationshipManager.getRelationshipsForNEW(moduleNameVersion).stream()
                .map(relationship -> relationship.getChild())
                .forEach(childNameVersion -> addModuleToGraph(childNameVersion, null, graph, goRelationshipManager, goModDependencyManager)); // this w/ null is actually called 119 times == number of main -> (direct or indirect) dep.
        }

//        graph.getChildrenForParent(viper180ext)
        ExternalId viper180ext = externalIdFactory.createNameVersionExternalId(Forge.GOLANG, "github.com/spf13/viper","v1.8.1");
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

        Dependency dependency = goModDependencyManager.getDependencyForModule(moduleNameVersion); // if that name and version combo doesn't exist ... we should be skipping it.
        if (parent != null) {
            graph.addChildWithParent(dependency, parent);
        } else {
            if (moduleNameVersion.getName().contains("viper")) {
                System.out.println("processing direct dep viper181"); // by the time we get here ... its already fully graphed?
            }
            graph.addDirectDependency(dependency); // for the viper test ... if the go mod graph output was not modified ... then it is NOT necessarily true that this is a direct dep. sshhhoot
        } // first it adds it to the bigger dependencies map (externalId, dependency)... then it also adds it to the smaller directDependencies list ... whatever that does ok

        if (!fullyGraphedModules.contains(moduleNameVersion) && goRelationshipManager.hasRelationshipsForNEW(moduleNameVersion)) {
            fullyGraphedModules.add(moduleNameVersion);
            List<GoGraphRelationship> projectRelationships = goRelationshipManager.getRelationshipsForNEW(moduleNameVersion);
            for (GoGraphRelationship projectRelationship : projectRelationships) {
                addModuleToGraph(projectRelationship.getChild(), dependency, graph, goRelationshipManager, goModDependencyManager);
            }
        }
    }
}
