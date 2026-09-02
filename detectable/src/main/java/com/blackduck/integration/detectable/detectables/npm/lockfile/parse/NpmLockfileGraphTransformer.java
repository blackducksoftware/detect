package com.blackduck.integration.detectable.detectables.npm.lockfile.parse;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.npm.NpmDependencyType;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.NpmDependency;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.NpmProject;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.NpmRequires;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.PackageLock;
import com.blackduck.integration.util.NameVersion;

public class NpmLockfileGraphTransformer {
    private final Logger logger = LoggerFactory.getLogger(NpmLockfileGraphTransformer.class);
    private final EnumListFilter<NpmDependencyType> npmDependencyTypeFilter;

    public NpmLockfileGraphTransformer(EnumListFilter<NpmDependencyType> npmDependencyTypeFilter) {
        // Treat null as "include all types" so callers that only care about workspace handling
        // can pass null without risking NPEs when shouldInclude/shouldExclude are called.
        this.npmDependencyTypeFilter = npmDependencyTypeFilter != null
            ? npmDependencyTypeFilter
            : EnumListFilter.excludeNone();
    }

    public DependencyGraph transform(PackageLock packageLock, NpmProject project, List<NameVersion> externalDependencies, List<String> workspaces) {
        DependencyGraph dependencyGraph = new BasicDependencyGraph();

        logger.debug("Processing project.");
        if (packageLock.packages != null || packageLock.dependencies != null) {
            logger.debug(String.format("Found %d packages in the lockfile.",
                    packageLock.packages != null ? packageLock.packages.size() : packageLock.dependencies.size()));

            createGraphFromResolvedDependencies(project, externalDependencies, workspaces, dependencyGraph);
            addRootDependencies(project, dependencyGraph, externalDependencies, workspaces);

            logger.debug(String.format("Found %d root dependencies.", dependencyGraph.getRootDependencies().size()));
        } else {
            logger.debug("Lock file did not have a 'packages' or 'dependencies' section.");
        }

        return dependencyGraph;
    }

    private void createGraphFromResolvedDependencies(NpmProject project, List<NameVersion> externalDependencies, List<String> workspaces, DependencyGraph dependencyGraph) {
        for (NpmDependency resolved : project.getResolvedDependencies()) {
            transformTreeToGraph(resolved, project, dependencyGraph, externalDependencies, workspaces);
        }
    }

    private void addRootDependencies(NpmProject project, DependencyGraph dependencyGraph, List<NameVersion> externalDependencies, List<String> workspaces) {
        boolean atLeastOneRequired = !project.getDeclaredDependencies().isEmpty()
            || !project.getDeclaredDevDependencies().isEmpty()
            || !project.getDeclaredPeerDependencies().isEmpty();

        // Two cases must be distinguished here:
        //
        // Case 1 — No package.json was provided (workspaces == null):
        //   We have only a lock file with no package.json to tell us which entries are "declared".
        //   The previous fallback (add all resolved entries to root) is the correct behaviour here.
        //
        // Case 2 — A package.json WAS provided (workspaces != null), but the declared-dep lists
        //   are all empty. This happens when the root package.json has no direct dependencies of
        //   its own AND all workspace packages were excluded by detect.npm.excluded.workspaces.
        //   Because the workspace-filter strips those packages from combinedPackageJson before
        //   NpmProject is built, every declared-dep list ends up empty even though a valid
        //   package.json existed. Falling through to the "add all" fallback in this case causes
        //   every lock-file entry to be promoted to the root.
        //
        // The fix: use the explicit-declaration path whenever a package.json is present
        // (workspaces != null), even if every declared list is empty. That correctly produces
        // zero root dependencies when the root declares nothing and all workspaces are filtered.
        // The "add all" fallback is preserved only for the true no-package.json case.
        if (atLeastOneRequired || workspaces != null) {
            addRootDependencies(project.getResolvedDependencies(), project.getDeclaredDependencies(), dependencyGraph, externalDependencies);
            if (npmDependencyTypeFilter.shouldInclude(NpmDependencyType.DEV)) {
                addRootDependencies(project.getResolvedDependencies(), project.getDeclaredDevDependencies(), dependencyGraph, externalDependencies);
            }
            if (npmDependencyTypeFilter.shouldInclude(NpmDependencyType.PEER)) {
                addRootDependencies(project.getResolvedDependencies(), project.getDeclaredPeerDependencies(), dependencyGraph, externalDependencies);
            }
            if (npmDependencyTypeFilter.shouldInclude(NpmDependencyType.OPTIONAL)) {
                addRootDependencies(project.getResolvedDependencies(), project.getDeclaredOptionalDependencies(), dependencyGraph, externalDependencies);
            }
        } else {
            // No package.json provided — fall back to treating all resolved lock-file entries as root deps.
            project.getResolvedDependencies()
                .stream()
                .filter(this::shouldIncludeDependency)
                .forEach(dependencyGraph::addChildToRoot);
        }
    }

    private void addRootDependencies(
        List<NpmDependency> resolvedDependencies,
        List<NpmRequires> requires,
        DependencyGraph dependencyGraph,
        List<NameVersion> externalDependencies
    ) {
        for (NpmRequires dependency : requires) {
            Dependency resolved = lookupProjectOrExternal(dependency.getName(), resolvedDependencies, externalDependencies);
            if (resolved != null) {
                dependencyGraph.addChildToRoot(resolved);
            } else {
                logger.debug("No resolved dependency found for dependency package: {}", dependency.getName());
            }
        }
    }

    private void transformTreeToGraph(NpmDependency npmDependency, NpmProject npmProject, DependencyGraph dependencyGraph, List<NameVersion> externalDependencies, List<String> workspaces) {
        if (!shouldIncludeDependency(npmDependency)) {
            return;
        }

        if (workspaces != null && !StringUtils.isBlank(npmDependency.getName()) &&
                workspaces.stream().anyMatch(x -> x.equals(npmDependency.getName()))) {
            dependencyGraph.addDirectDependency(npmDependency);
            addWorkspaceRequires(npmDependency, npmProject, dependencyGraph, externalDependencies);
        } else {
            npmDependency.getRequires().forEach(required -> {
                logger.trace(String.format("Required package: %s of version: %s", required.getName(), required.getFuzzyVersion()));
                NpmDependency resolved = lookupDependency(required.getName(), npmDependency, npmProject, externalDependencies);
                if (resolved == null) {
                    logger.debug("No resolved dependency found for required package: {}", required.getName());
                } else {
                    logger.trace(String.format("Found package: %s with version: %s", resolved.getName(), resolved.getVersion()));
                    if (shouldIncludeDependency(resolved)) {
                        dependencyGraph.addChildWithParent(resolved, npmDependency);
                    }
                }
            });
        }

        npmDependency.getDependencies().forEach(child -> transformTreeToGraph(child, npmProject, dependencyGraph, externalDependencies, workspaces));
    }

    /**
     * Adds all requires under a workspace dependency directly to the root. Workspace dependencies'
     * own deps are treated as direct project dependencies in Black Duck.
     */
    private void addWorkspaceRequires(NpmDependency npmDependency, NpmProject npmProject, DependencyGraph dependencyGraph, List<NameVersion> externalDependencies) {
        for (NpmRequires required : npmDependency.getRequires()) {
            NpmDependency workspaceDependency = lookupDependency(required.getName(), npmDependency, npmProject, externalDependencies);

            if (workspaceDependency != null) {
                if ((workspaceDependency.isDevDependency() && npmDependencyTypeFilter.shouldExclude(NpmDependencyType.DEV))
                        || (workspaceDependency.isPeerDependency() && npmDependencyTypeFilter.shouldExclude(NpmDependencyType.PEER))
                        || (workspaceDependency.isOptionalDependency() && npmDependencyTypeFilter.shouldExclude(NpmDependencyType.OPTIONAL))) {
                    continue;
                }
                dependencyGraph.addChildrenToRoot(workspaceDependency);
            }
        }
    }

    private NpmDependency lookupProjectOrExternal(String name, List<NpmDependency> projectResolvedDependencies, List<NameVersion> externalDependencies) {
        NpmDependency projectDependency = firstDependencyWithName(projectResolvedDependencies, name);
        if (projectDependency != null) {
            return projectDependency;
        }

        Optional<NameVersion> externalNameVersion = externalDependencies.stream().filter(it -> it.getName().equals(name)).findFirst();
        return externalNameVersion.map(nameVersion ->
            new NpmDependency(nameVersion.getName(), nameVersion.getVersion(), false, false, false)
        ).orElse(null);
    }

    //returns the first dependency in the following order: directly under this dependency, under a parent, under the project, under external dependencies
    private NpmDependency lookupDependency(String name, NpmDependency npmDependency, NpmProject project, List<NameVersion> externalDependencies) {
        NpmDependency resolved = firstDependencyWithName(npmDependency.getDependencies(), name);

        if (resolved != null) {
            return resolved;
        } else if (npmDependency.getParent().isPresent()) {
            return lookupDependency(name, npmDependency.getParent().get(), project, externalDependencies);
        } else {
            return lookupProjectOrExternal(name, project.getResolvedDependencies(), externalDependencies);
        }
    }

    private NpmDependency firstDependencyWithName(List<NpmDependency> dependencies, String name) {
        for (NpmDependency current : dependencies) {
            if (current.getName().equals(name)) {
                return current;
            }
        }
        return null;
    }

    private boolean shouldIncludeDependency(NpmDependency packageLockDependency) {
        return !packageLockDependency.getName().contains("@rush-temp") && ((!packageLockDependency.isDevDependency() && !packageLockDependency.isPeerDependency() && !packageLockDependency.isOptionalDependency()) // If the type is not dev or peer, we always want to include it.
            || (packageLockDependency.isDevDependency() && npmDependencyTypeFilter.shouldInclude(NpmDependencyType.DEV))
            || (packageLockDependency.isPeerDependency() && npmDependencyTypeFilter.shouldInclude(NpmDependencyType.PEER))
            || (packageLockDependency.isOptionalDependency() && npmDependencyTypeFilter.shouldInclude(NpmDependencyType.OPTIONAL)));
    }
}
