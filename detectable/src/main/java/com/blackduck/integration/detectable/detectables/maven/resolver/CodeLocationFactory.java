package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Factory for creating CodeLocation objects with proper error handling.
 *
 * <p>This factory encapsulates the logic for creating CodeLocations with external IDs,
 * providing fallback behavior when external ID creation fails.
 *
 * <p><strong>Fallback Strategy:</strong> If external ID creation fails, a CodeLocation
 * is still created without an external ID to ensure extraction continues.
 */
class CodeLocationFactory {

    private static final Logger logger = LoggerFactory.getLogger(CodeLocationFactory.class);

    private final ExternalIdFactory externalIdFactory;

    /**
     * Constructs a CodeLocationFactory.
     *
     * @param externalIdFactory Factory for creating Maven external identifiers
     */
    public CodeLocationFactory(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    /**
     * Creates a CodeLocation from a dependency graph and Maven project coordinates.
     *
     * <p>Attempts to create a Maven external ID from the project coordinates. If creation
     * fails, logs a debug message and creates a CodeLocation without an external ID.
     *
     * @param dependencyGraph The dependency graph for this code location
     * @param project The Maven project containing coordinates and source information
     * @param sourcePath The source directory for this code location
     * @return A CodeLocation with external ID if possible, otherwise without
     */
    public CodeLocation createCodeLocation(DependencyGraph dependencyGraph, MavenProject project, File sourcePath) {
        try {
            ExternalId externalId = externalIdFactory.createMavenExternalId(
                project.getCoordinates().getGroupId(),
                project.getCoordinates().getArtifactId(),
                project.getCoordinates().getVersion()
            );
            return new CodeLocation(dependencyGraph, externalId, sourcePath);
        } catch (Exception e) {
            logger.debug("Failed to create external id for '{}:{}:{}': {}",
                safeGet(() -> project.getCoordinates().getGroupId()),
                safeGet(() -> project.getCoordinates().getArtifactId()),
                safeGet(() -> project.getCoordinates().getVersion()),
                e.getMessage());
            return new CodeLocation(dependencyGraph);
        }
    }

    /**
     * Creates a CodeLocation from a dependency graph with explicit coordinates.
     *
     * <p>This overload is useful when you have individual coordinate components
     * rather than a full MavenProject object.
     *
     * @param dependencyGraph The dependency graph for this code location
     * @param groupId Maven group ID
     * @param artifactId Maven artifact ID
     * @param version Maven version
     * @param sourcePath The source directory for this code location
     * @return A CodeLocation with external ID if possible, otherwise without
     */
    public CodeLocation createCodeLocation(DependencyGraph dependencyGraph,
                                          String groupId,
                                          String artifactId,
                                          String version,
                                          File sourcePath) {
        try {
            ExternalId externalId = externalIdFactory.createMavenExternalId(groupId, artifactId, version);
            return new CodeLocation(dependencyGraph, externalId, sourcePath);
        } catch (Exception e) {
            logger.debug("Failed to create external id for '{}:{}:{}': {}", groupId, artifactId, version, e.getMessage());
            return new CodeLocation(dependencyGraph);
        }
    }

    /**
     * Safely extracts a value, returning null if any exception occurs.
     *
     * @param supplier Function that provides the value
     * @return The extracted value or null if extraction fails
     */
    private String safeGet(java.util.function.Supplier<String> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return null;
        }
    }
}

