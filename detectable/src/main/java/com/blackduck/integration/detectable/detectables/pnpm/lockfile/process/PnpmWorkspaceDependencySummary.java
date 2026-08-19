package com.blackduck.integration.detectable.detectables.pnpm.lockfile.process;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;

/**
 * DTO that captures the dependency summary for a single PNPM workspace module.
 * Built via {@link #from(DependencyGraph)} and logged via {@link #logModuleSummary(Logger, String, DependencyGraph)}.
 */
public class PnpmWorkspaceDependencySummary {

    private static final String VERSION_SEPARATOR = "@";
    private static final String ROOT_MODULE_LABEL = "(root)";
    private static final String ROOT_KEY = ".";

    public final int directCount;
    public final int transitiveCount;
    public final List<String> depNames;

    private PnpmWorkspaceDependencySummary(int directCount, int transitiveCount, List<String> depNames) {
        this.directCount = directCount;
        this.transitiveCount = transitiveCount;
        this.depNames = depNames;
    }

    /**
     * Builds a summary by performing a BFS traversal of the dependency graph.
     * The {@link java.util.HashSet} ensures each dependency is counted exactly once,
     * correctly handling diamond dependencies (A→D and B→D both shared).
     */
    public static PnpmWorkspaceDependencySummary from(DependencyGraph graph) {
        Set<Dependency> visited = new HashSet<>();
        Queue<Dependency> queue = new LinkedList<>(graph.getRootDependencies());
        while (!queue.isEmpty()) {
            Dependency dep = queue.poll();
            if (visited.add(dep)) {
                queue.addAll(graph.getChildrenForParent(dep));
            }
        }
        int directCount = graph.getRootDependencies().size();
        int transitiveCount = visited.size() - directCount;
        List<String> depNames = visited.stream()
            .map(dep -> dep.getName() + VERSION_SEPARATOR + dep.getVersion())
            .sorted()
            .collect(Collectors.toList());
        return new PnpmWorkspaceDependencySummary(directCount, transitiveCount, depNames);
    }

    /**
     * Logs the workspace module summary at INFO and DEBUG levels.
     * The BFS traversal in {@link #from(DependencyGraph)} is only performed when
     * at least INFO or DEBUG logging is enabled, avoiding unnecessary CPU cost.
     */
    public static void logModuleSummary(Logger logger, String projectKey, DependencyGraph graph) {
        if (!logger.isInfoEnabled() && !logger.isDebugEnabled()) {
            return;
        }
        String moduleLabel = ROOT_KEY.equals(projectKey) ? ROOT_MODULE_LABEL : projectKey;
        PnpmWorkspaceDependencySummary summary = from(graph);
        logger.info("Workspace module '{}': {} direct and {} transitive dependencies discovered.",
            moduleLabel, summary.directCount, summary.transitiveCount);
        if (logger.isDebugEnabled()) {
            logger.debug("Workspace module '{}' full dependency list: {}", moduleLabel, summary.depNames);
        }
    }
}

