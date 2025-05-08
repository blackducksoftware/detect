package com.blackduck.integration.detectable.detectables.npm.lockfile.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.NpmDependency;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.NpmProject;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.NpmRequires;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.PackageLock;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfileGraphTransformer;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfilePackager;
import com.blackduck.integration.detectable.util.FunctionalTestFiles;
import com.blackduck.integration.detectable.util.graph.GraphAssert;

public class NpmLockfileGraphTransformerTest {
    
    private Gson gson;
    private ExternalIdFactory externalIdFactory;
    private NpmLockfilePackager packager;
    
    @BeforeEach
    public void setup() {
        gson = new Gson();
        externalIdFactory = new ExternalIdFactory();
        packager = new NpmLockfilePackager(gson, externalIdFactory, null, null);
    }
    
    @Test
    public void testWorkspaceDependenciesTransformedAsDirectDependenciesWithWildcards() {
        // Prepare the package-lock.json.
        String lockFileText = FunctionalTestFiles.asString("/npm/packages-linkage-test/package-lock-wildcards.json");
        validateDirectDependencies(lockFileText);
    }
    
    @Test
    public void testWorkspaceDependenciesTransformedAsDirectDependenciesWithRelativePaths() {
        // Prepare the package-lock.json.
        String lockFileText = FunctionalTestFiles.asString("/npm/packages-linkage-test/package-lock-relative.json");
        validateDirectDependencies(lockFileText);
    }
    
    @Test
    public void testWorkspaceDependenciesTransformedAsDirectDependenciesWithRelativeAndWildcards() {
        // Prepare the package-lock.json.
        String lockFileText = FunctionalTestFiles.asString("/npm/packages-linkage-test/package-lock-wildcards-and-relative.json");
        validateDirectDependencies(lockFileText);
    }
    
    /**
     * This method attempts to validate that the NpmLockfileGraphTransformer.transformTreeToGraph function
     * correctly turns open source packages directly associated with workspaces into direct dependencies
     * in the project. They are not transitive dependencies simply because the workspace is an intermediary 
     * between them and the project.
     */
    private void validateDirectDependencies(String lockFileText) {
        lockFileText = packager.removePathInfoFromPackageName(lockFileText);   
        PackageLock packageLock = gson.fromJson(lockFileText, PackageLock.class);  

        List<NpmDependency> resolvedDependencies = new ArrayList<>();
        List<NpmDependency> simpleDependencies = new ArrayList<>();
        List<NpmRequires> requires = new ArrayList<>();
        
        // Mimic the dependency construction of other areas of the code. This is a simplification
        // as normally the requires would be in one area of the structure and the dependencies
        // in another but this is a unit test and we are only concerned about workspace dependencies becoming
        // direct dependencies in the final graph.
        NpmDependency workspaceDependency = new NpmDependency("packages/a", "1.0.0", false, false);
        NpmDependency simpleDependency1 = new NpmDependency("abbrev", "^2.0.0", false, false);
        NpmDependency simpleDependency2 = new NpmDependency("send", "0.17.2", false, false);
        NpmRequires workspaceRequires1 = new NpmRequires("abbrev", "^2.0.0");
        NpmRequires workspaceRequires2 = new NpmRequires("send", "0.17.2");
        requires.add(workspaceRequires1);
        requires.add(workspaceRequires2);
        simpleDependencies.add(simpleDependency1);
        simpleDependencies.add(simpleDependency2);
        workspaceDependency.addAllDependencies(simpleDependencies);
        workspaceDependency.addAllRequires(requires);
        resolvedDependencies.add(workspaceDependency);
        
        NpmProject npmProject = new NpmProject(
            StringUtils.EMPTY,
            StringUtils.EMPTY,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            resolvedDependencies
        );
        
        NpmLockfileGraphTransformer graphTransformer = new NpmLockfileGraphTransformer(null);
        List<String> workspaces = new ArrayList<>();
        workspaces.add("packages/a");
        DependencyGraph graph = graphTransformer.transform(packageLock, npmProject, Collections.emptyList(), workspaces);
        
        GraphAssert graphAssert = new GraphAssert(Forge.NPMJS, graph);
        graphAssert.hasRootDependency(externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "abbrev", "^2.0.0"));
        graphAssert.hasRootDependency(externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "send", "0.17.2"));
    }
}
