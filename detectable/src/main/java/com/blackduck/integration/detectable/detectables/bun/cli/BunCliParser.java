package com.blackduck.integration.detectable.detectables.bun.cli;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.bun.BunPackageNameUtils;
import com.blackduck.integration.util.NameVersion;

public class BunCliParser {
    private static final Logger logger = LoggerFactory.getLogger(BunCliParser.class);
    // Every tree-prefix token is exactly 4 characters: "├── ", "└── ", "│   ", "    "
    private static final int PREFIX_WIDTH = 4;

    private final ExternalIdFactory externalIdFactory;

    public BunCliParser(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    /**
     * Parses the stdout of {@code bun pm list --all} into a dependency graph.
     *
     * <p>{@code bun pm list --all} outputs a hierarchical tree where level-0 entries (connector
     * at column 0) are direct dependencies and level-N entries (connector at column {@code N * 4})
     * are transitives nested under their actual parents.  For example:
     * <pre>
     *   └── zaproxy@0.3.0             ← level 0: direct dep
     *       ├── lodash@4.16.6         ← level 1: transitive
     *       └── request@2.75.0        ← level 1: transitive
     *           ├── aws-sign2@0.6.0   ← level 2: transitive
     *           └── bl@1.1.2          ← level 2: transitive
     *               └── readable-stream@2.0.6  ← level 3
     * </pre>
     *
     * <h3>Stack invariant (Ivy/Cargo DFS pattern)</h3>
     * <p>Before pushing the current node, the stack is trimmed to exactly {@code level} entries
     * ({@code while stack.size() > level: pop}).  After the push the stack holds the full
     * ancestor chain from the root down to the current node, so {@code stack.peek()} is always
     * the correct parent for the next deeper entry.
     *
     * <h3>Deduplication</h3>
     * <p>A single {@link Dependency} object is shared for every occurrence of the same
     * {@code name@version}.  When bun repeats a transitive under multiple parents (or uses
     * a "deduped" annotation to mark a non-repeated entry), the cache ensures all parent-child
     * links converge on the same node.
     *
     * @param lines                  stdout lines from {@code bun list}
     * @param directDepNameVersions  optional set of {@code "name@version"} strings; when
     *                               non-null, only level-0 entries in this set are added as
     *                               direct deps — useful for tests or future filter needs.
     *                               Pass {@code null} (or use {@link #parse(List)}) to treat
     *                               every level-0 entry as a direct dep.
     */
    public DependencyGraph parse(List<String> lines, Set<String> directDepNameVersions) {
        DependencyGraph graph = new BasicDependencyGraph();
        Deque<Dependency> stack = new ArrayDeque<>();
        Map<String, Dependency> depCache = new HashMap<>();

        for (String line : lines) {
            // Locate the tree connector: ├ (U+251C) or └ (U+2514).
            // Lines without either are headers, blank lines, or continuation pipes — skip them.
            int connectorIdx = line.indexOf('├');
            if (connectorIdx < 0) {
                connectorIdx = line.indexOf('└');
            }
            if (connectorIdx < 0) {
                continue;
            }

            // 0-indexed level: level 0 = direct dep (connector at col 0),
            // level 1 = child of direct (connector at col 4), level 2 at col 8, …
            int level = connectorIdx / PREFIX_WIDTH;

            // Everything after "├── " or "└── " (4 chars) is "name@version[annotation]"
            String entry = line.substring(connectorIdx + PREFIX_WIDTH);
            if (entry.isEmpty()) {
                continue;
            }

            // Split "name@version" or "@scope/name@version" on the last '@'
            NameVersion rawNv = BunPackageNameUtils.parseNameVersion(entry);
            if (rawNv == null) {
                logger.warn("Skipping bun list line — could not parse name@version: {}", line);
                continue;
            }
            String name = rawNv.getName();
            // Strip trailing annotations such as "1.2.3 deduped" or "1.2.3 (circular)"
            String version = sanitizeVersion(rawNv.getVersion().trim());
            String cacheKey = name + "@" + version;

            // Deduplication: share one Dependency object per name@version so that all
            // parent-child links referencing the same package converge on a single node.
            Dependency dep = depCache.computeIfAbsent(cacheKey, k -> {
                ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, name, version);
                return new Dependency(name, version, externalId);
            });

            // Trim the ancestor stack so its size equals this node's level.
            // After this, stack.peek() is the parent (or the stack is empty for level 0).
            while (stack.size() > level) {
                stack.pop();
            }

            if (level == 0) {
                if (directDepNameVersions == null || directDepNameVersions.contains(cacheKey)) {
                    graph.addDirectDependency(dep);
                }
            } else if (!stack.isEmpty()) {
                graph.addParentWithChild(stack.peek(), dep);
            } else {
                logger.warn("No parent on stack for {}@{} at level {}", name, version, level);
            }

            stack.push(dep);
        }

        return graph;
    }

    /** Convenience overload — all level-0 entries become direct deps. */
    public DependencyGraph parse(List<String> lines) {
        return parse(lines, null);
    }

    private String sanitizeVersion(String raw) {
        // Remove bun-specific trailing annotations (e.g. "1.2.3 deduped", "1.2.3 (circular)")
        int space = raw.indexOf(' ');
        return space > 0 ? raw.substring(0, space) : raw;
    }
}
