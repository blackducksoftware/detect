package com.blackduck.integration.detectable.detectables.bazel.aspect;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Builds a {@link DependencyGraph} from the aspect metadata produced by
 * {@link BazelAspectExtractor}, adding nodes directly into an existing graph.
 *
 * <p>Supported ecosystems:
 * <ul>
 *   <li><b>Maven</b> ({@code rules_jvm_external}) — coordinates from {@code maven_coordinates=g:a:v} tag.</li>
 *   <li><b>Rust</b> ({@code crates_repository}) — coordinates from {@code crate-name=} tag + {@code version} attribute.</li>
 *   <li><b>Python</b> ({@code pip.parse}) — coordinates from {@code pypi_name=} and {@code pypi_version=} tags.</li>
 * </ul>
 *
 * <p>Nodes without coordinates (C++ BCR targets, local user targets) are skipped for BOM purposes
 * but are still used as graph connectors during traversal to establish direct/transitive edges.
 */
public class AspectDependencyGraphBuilder {
    private static final Logger logger = LoggerFactory.getLogger(AspectDependencyGraphBuilder.class);

    // Use the same forge constants as the existing Cargo and pip detectors — these are
    // the identifiers BD's KB recognises for crates.io and PyPI components.
    private static final Forge CRATES_IO_FORGE = Forge.CRATES;
    private static final Forge PYPI_FORGE = Forge.PYPI;

    private final ExternalIdFactory externalIdFactory;

    public AspectDependencyGraphBuilder(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    /**
     * Populates {@code graph} with the Maven/Rust/Python dependency nodes found in {@code nodes},
     * preserving direct/transitive edges relative to {@code rootTarget}.
     *
     * <p>Direct deps of local targets (the user's own code) are added as root children.
     * Their transitive deps are linked with parent edges.
     *
     * @param graph      the graph to populate — typically already containing BCR module entries
     * @param nodes      aspect metadata for every target visited during the build
     * @param rootTarget the user's Bazel target (e.g. {@code //src/java:app})
     */
    public void populateGraph(DependencyGraph graph, List<AspectNodeMetadata> nodes, String rootTarget) {
        Map<String, AspectNodeMetadata> byLabel = indexByLabel(nodes);
        Map<String, Dependency> depByLabel = buildDependencyMap(nodes);

        // Walk from every local (user) target in the aspect output.
        // This handles both a single target (//src/java:app → @@//src/java:app)
        // and wildcards (//... → multiple @@//src/*:app targets).
        // Local targets are identified by label starting with @@// (workspace-local in Bzlmod form).
        Set<String> visited = new HashSet<>();
        int localEntryPoints = 0;
        for (AspectNodeMetadata node : nodes) {
            if (!node.isExternalRepo()) {
                walkFromRoot(graph, node, byLabel, depByLabel, visited);
                localEntryPoints++;
            }
        }

        if (localEntryPoints == 0) {
            logger.warn("Aspect: no local (user) targets found in {} aspect nodes for target '{}' — skipping graph population",
                nodes.size(), rootTarget);
            return;
        }

        logger.info("Aspect: added {} Maven/Rust/Python node(s) to the dependency graph from {} total aspect nodes ({} entry point(s))",
            depByLabel.size(), nodes.size(), localEntryPoints);
    }

    private void walkFromRoot(DependencyGraph graph,
                              AspectNodeMetadata node,
                              Map<String, AspectNodeMetadata> byLabel,
                              Map<String, Dependency> depByLabel,
                              Set<String> visited) {
        if (!visited.add(node.getLabel())) {
            return;
        }

        for (AspectNodeDep edge : node.getDirectDeps()) {
            String depLabel = edge.getLabel();
            Dependency dep = depByLabel.get(depLabel);
            AspectNodeMetadata depNode = byLabel.get(depLabel);

            if (dep != null) {
                if (!node.isExternalRepo()) {
                    // Direct dep of a local (user) target → root child
                    graph.addChildToRoot(dep);
                    logger.debug("Aspect: {} → {} (direct)", node.getLabel(), depLabel);
                }
                // Recurse into this dep's children to add transitive edges
                if (depNode != null && visited.add(dep.getExternalId().toString() + depLabel)) {
                    addTransitiveChildren(graph, dep, depNode, byLabel, depByLabel, new HashSet<>(visited));
                }
            } else if (depNode != null && !depNode.isExternalRepo()) {
                // Local intermediate target (e.g. //src/cpp:greeter_lib) — follow through it
                walkFromRoot(graph, depNode, byLabel, depByLabel, visited);
            }
        }
    }

    private void addTransitiveChildren(DependencyGraph graph,
                                       Dependency parent,
                                       AspectNodeMetadata parentNode,
                                       Map<String, AspectNodeMetadata> byLabel,
                                       Map<String, Dependency> depByLabel,
                                       Set<String> visited) {
        for (AspectNodeDep edge : parentNode.getDirectDeps()) {
            String depLabel = edge.getLabel();
            Dependency childDep = depByLabel.get(depLabel);
            if (childDep == null) {
                continue;
            }
            graph.addChildWithParent(childDep, parent);
            logger.debug("Aspect: {} → {} (transitive)", parentNode.getLabel(), depLabel);

            // Guard: only recurse into a child's subtree once
            if (visited.add(depLabel)) {
                AspectNodeMetadata childNode = byLabel.get(depLabel);
                if (childNode != null) {
                    addTransitiveChildren(graph, childDep, childNode, byLabel, depByLabel, visited);
                }
            }
        }
    }

    private Map<String, AspectNodeMetadata> indexByLabel(List<AspectNodeMetadata> nodes) {
        Map<String, AspectNodeMetadata> map = new HashMap<>();
        for (AspectNodeMetadata node : nodes) {
            if (node.getLabel() != null) {
                map.put(node.getLabel(), node);
            }
        }
        return map;
    }

    private Map<String, Dependency> buildDependencyMap(List<AspectNodeMetadata> nodes) {
        Map<String, Dependency> map = new HashMap<>();
        for (AspectNodeMetadata node : nodes) {
            toDependency(node).ifPresent(dep -> map.put(node.getLabel(), dep));
        }
        return map;
    }

    private Optional<Dependency> toDependency(AspectNodeMetadata node) {
        try {
            if (node.hasMavenCoordinates()) {
                return Optional.ofNullable(mavenDependency(node.getMavenCoordinates()));
            }
            if (node.hasCrateCoordinates()) {
                ExternalId id = externalIdFactory.createNameVersionExternalId(CRATES_IO_FORGE, node.getCrateName(), node.getCrateVersion());
                return Optional.of(new Dependency(node.getCrateName(), node.getCrateVersion(), id));
            }
            if (node.hasPypiCoordinates()) {
                ExternalId id = externalIdFactory.createNameVersionExternalId(PYPI_FORGE, node.getPypiName(), node.getPypiVersion());
                return Optional.of(new Dependency(node.getPypiName(), node.getPypiVersion(), id));
            }
        } catch (Exception e) {
            logger.warn("Aspect: could not create dependency for '{}': {}", node.getLabel(), e.getMessage());
        }
        return Optional.empty();
    }

    private Dependency mavenDependency(String coordinates) {
        // coordinates = "group:artifact:version"
        String[] parts = coordinates.split(":");
        if (parts.length < 3) {
            logger.warn("Aspect: malformed maven_coordinates '{}' — expected g:a:v", coordinates);
            return null;
        }
        String group = parts[0];
        String artifact = parts[1];
        String version = parts[parts.length - 1];
        ExternalId id = externalIdFactory.createMavenExternalId(group, artifact, version);
        return new Dependency(artifact, version, id);
    }

}
