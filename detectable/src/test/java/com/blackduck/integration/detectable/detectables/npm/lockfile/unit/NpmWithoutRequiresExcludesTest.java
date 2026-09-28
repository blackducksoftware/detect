package com.blackduck.integration.detectable.detectables.npm.lockfile.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.npm.NpmDependencyType;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.NpmDependency;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.NpmProject;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.PackageLock;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfileGraphTransformer;
import com.blackduck.integration.detectable.util.graph.GraphAssert;

public class NpmWithoutRequiresExcludesTest {
    @Test
    public void testDevDependencyExcluded() {
        PackageLock packageLock = new PackageLock();

        List<NpmDependency> resolvedDependencies = new ArrayList<>();
        resolvedDependencies.add(new NpmDependency("example", "1.0.0", true, true, true));
        NpmProject npmProject = new NpmProject(
            StringUtils.EMPTY,
            StringUtils.EMPTY,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            resolvedDependencies
        );

        NpmLockfileGraphTransformer graphTransformer = new NpmLockfileGraphTransformer(
            EnumListFilter.fromExcluded(NpmDependencyType.DEV, NpmDependencyType.PEER, NpmDependencyType.OPTIONAL)
        );
        
        // Test with packages for v2/v3 lockfile
        packageLock.packages = new HashMap<>();
        
        DependencyGraph graph = graphTransformer.transform(packageLock, npmProject, Collections.emptyList(), null);

        GraphAssert graphAssert = new GraphAssert(Forge.NPMJS, graph);
        graphAssert.hasRootSize(0);
        
        // Test with dependencies for v1 lockfile
        packageLock.dependencies = new HashMap<>();
        graph = graphTransformer.transform(packageLock, npmProject, Collections.emptyList(), null);

        graphAssert = new GraphAssert(Forge.NPMJS, graph);
        graphAssert.hasRootSize(0);
    }
    
    @Test
    public void testOptionalDependencyExcluded() {
        PackageLock packageLock = new PackageLock();

        List<NpmDependency> resolvedDependencies = new ArrayList<>();
        resolvedDependencies.add(new NpmDependency("example", "1.0.0", false, false, true));
        NpmProject npmProject = new NpmProject(
            StringUtils.EMPTY,
            StringUtils.EMPTY,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            resolvedDependencies
        );

        NpmLockfileGraphTransformer graphTransformer = new NpmLockfileGraphTransformer(
            EnumListFilter.fromExcluded(NpmDependencyType.OPTIONAL)
        );
        
        // Test with packages for v2/v3 lockfile
        packageLock.packages = new HashMap<>();
        
        DependencyGraph graph = graphTransformer.transform(packageLock, npmProject, Collections.emptyList(), null);

        GraphAssert graphAssert = new GraphAssert(Forge.NPMJS, graph);
        graphAssert.hasRootSize(0);
        
        // Test with dependencies for v1 lockfile
        packageLock.dependencies = new HashMap<>();
        graph = graphTransformer.transform(packageLock, npmProject, Collections.emptyList(), null);

        graphAssert = new GraphAssert(Forge.NPMJS, graph);
        graphAssert.hasRootSize(0);
        
        // Clear filtering and check we get a dependency
        graphTransformer = new NpmLockfileGraphTransformer(EnumListFilter.fromExcluded());
        graph = graphTransformer.transform(packageLock, npmProject, Collections.emptyList(), null);
        
        graphAssert = new GraphAssert(Forge.NPMJS, graph);
        graphAssert.hasRootSize(1);
    }
}
