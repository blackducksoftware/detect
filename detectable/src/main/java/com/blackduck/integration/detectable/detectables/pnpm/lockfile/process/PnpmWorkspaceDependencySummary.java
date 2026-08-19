package com.blackduck.integration.detectable.detectables.pnpm.lockfile.process;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.Predicate;
import org.slf4j.Logger;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;

/**
 * DTO that captures the dependency summary for a single PNPM workspace module.
 * Built via {@link #from(DependencyGraph, boolean)} and logged via {@link #logModuleSummary(Logger, String, DependencyGraph)}.
 */
public class PnpmWorkspaceDependencySummary {

    private static final String VERSION_SEPARATOR = "@";

    /** Shared constant identifying the root workspace module key. Used by both parsers and this class. */
    static final Predicate<String> IS_NODE_ROOT = "."::equals;
    static final String ROOT_MODULE_LABEL = "(root)";

    public final int directCount;
    public final int transitiveCount;
    public final List<String> depNames;

    private PnpmWorkspaceDependencySummary(int directCount, int transitiveCount, List<String> depNames) {
        this.directCount = directCount;
        this.transitiveCount = transitiveCount;
        this.depNames = Collections.unmodifiableList(depNames); // immutable DTO field
    }

    /**
     * Builds a summary by performing a BFS traversal of the dependency graph.
     * The {@link HashSet} ensures each dependency is counted exactly once,
     * correctly handling diamond dependencies (A→D and B→D both shared).
     *
     * @param includeDepNames if {@code true}, populates the full sorted dep name list;
     *                        pass {@code false} (e.g. when only INFO is active) to skip
     *                        the stream/sort/collect and avoid unnecessary CPU cost.
     */
    public static PnpmWorkspaceDependencySummary from(DependencyGraph graph, boolean includeDepNames) {
        Set<Dependency> rootDeps = graph.getRootDependencies();
        Set<Dependency> visited = new HashSet<>();
        Queue<Dependency> queue = new LinkedList<>(rootDeps);
        while (!queue.isEmpty()) {
            Dependency dep = queue.poll();
            if (visited.add(dep)) {
                queue.addAll(graph.getChildrenForParent(dep));
            }
        }
        int directCount = rootDeps.size();
        int transitiveCount = visited.size() - directCount;
        List<String> depNames = includeDepNames
            ? visited.stream()
                .map(dep -> dep.getName() + VERSION_SEPARATOR + dep.getVersion())
                .sorted()
                .collect(Collectors.toList())
            : Collections.emptyList();
        return new PnpmWorkspaceDependencySummary(directCount, transitiveCount, depNames);
    }

    /**
     * Logs the workspace module summary at INFO and DEBUG levels.
     * BFS traversal is skipped entirely when both levels are off.
     * The dep name list is only computed when DEBUG is enabled.
     */
    public static void logModuleSummary(Logger logger, String projectKey, DependencyGraph graph) {
        if (!logger.isInfoEnabled() && !logger.isDebugEnabled()) {
            return;
        }
        String moduleLabel = IS_NODE_ROOT.evaluate(projectKey) ? ROOT_MODULE_LABEL : projectKey; // Fix 4: consistent Predicate
        PnpmWorkspaceDependencySummary summary = from(graph, logger.isDebugEnabled()); // Fix 3: pass flag
        logger.info("Workspace module '{}': {} direct and {} transitive dependencies discovered.",
            moduleLabel, summary.directCount, summary.transitiveCount);
        if (logger.isDebugEnabled()) {
            logger.debug("Workspace module '{}' full dependency list: {}", moduleLabel, summary.depNames);
        }
    }
}
