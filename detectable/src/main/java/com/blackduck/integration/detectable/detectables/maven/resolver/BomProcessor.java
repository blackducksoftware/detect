package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.detectable.detectables.maven.resolver.model.*;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles BOM (Bill of Materials) import processing for Maven projects.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Iteratively discover and process scope=import entries in dependency management</li>
 *   <li>Download BOM POMs and merge their properties and managed dependencies</li>
 *   <li>Handle nested BOMs (BOMs that import other BOMs) with cycle detection</li>
 *   <li>Remove processed BOM entries from the final dependency management list</li>
 * </ul>
 *
 * <p>This class is not thread-safe due to its dependency on ProjectBuilder's mutable state.
 */
class BomProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Maximum number of BOM processing iterations to prevent infinite loops from pathological POMs.
     * This is a safety limit - in practice, most projects have far fewer nested BOMs.
     */
    private static final int MAX_BOM_ITERATIONS = 50;

    /**
     * Scope value used for BOM (Bill of Materials) imports in Maven dependency management.
     */
    private static final String SCOPE_IMPORT = "import";

    private final Path downloadDir;
    private final ProjectBuilder projectBuilder;
    private final PropertiesResolverProvider propertiesResolverProvider;

    BomProcessor(Path downloadDir, ProjectBuilder projectBuilder, PropertiesResolverProvider propertiesResolverProvider) {
        this.downloadDir = downloadDir;
        this.projectBuilder = projectBuilder;
        this.propertiesResolverProvider = propertiesResolverProvider;
    }

    PartialMavenProject processBoms(String pomFilePath, PartialMavenProject partialModel, Set<String> visitedBoms) throws Exception {
        int iteration = 0;

        while (iteration < MAX_BOM_ITERATIONS) {
            // Build combined properties for resolving BOM coordinates
            final Map<String, String> currentProps = buildCombinedProperties(partialModel);

            // Find unprocessed BOMs: scope=import AND not already visited
            List<PomXmlDependency> unprocessedBoms = partialModel.getDependencyManagement().stream()
                .filter(dep -> SCOPE_IMPORT.equals(dep.getScope()))
                .filter(dep -> {
                    String bomKey = buildBomKey(dep, currentProps);
                    return !visitedBoms.contains(bomKey);
                })
                .collect(Collectors.toList());

            if (unprocessedBoms.isEmpty()) {
                logger.debug("No more unprocessed BOMs found after {} iteration(s)", iteration);
                break;
            }

            logger.info("BOM processing iteration {}: found {} unprocessed BOM(s) in {}",
                iteration + 1, unprocessedBoms.size(), pomFilePath);

            // Process each unprocessed BOM
            for (PomXmlDependency bom : unprocessedBoms) {
                // Rebuild combined props in case they changed from previous BOM merges
                final Map<String, String> combinedProps = buildCombinedProperties(partialModel);

                String resolvedGroupId = resolveProperties(bom.getGroupId(), combinedProps);
                String resolvedArtifactId = resolveProperties(bom.getArtifactId(), combinedProps);
                String resolvedVersion = resolveProperties(bom.getVersion(), combinedProps);

                String bomKey = resolvedGroupId + ":" + resolvedArtifactId + ":" + resolvedVersion;

                // Mark as visited BEFORE processing to prevent cycles
                visitedBoms.add(bomKey);
                logger.debug("Processing BOM: {} (iteration {})", bomKey, iteration + 1);

                JavaCoordinates bomCoords = new JavaCoordinates(resolvedGroupId, resolvedArtifactId, resolvedVersion, "pom");
                MavenDownloader mavenDownloader = new MavenDownloader(partialModel.getRepositories(), downloadDir);
                File bomPomFile = mavenDownloader.downloadPom(bomCoords);

                if (bomPomFile != null) {
                    logger.debug("Building BOM project: {}", bomPomFile.getAbsolutePath());
                    PartialMavenProject bomProject = projectBuilder.internalBuildProject(bomPomFile, new HashSet<>());

                    // Resolve BOM dependencyManagement entries in BOM context before merging
                    resolveBomManagedEntries(bomProject);

                    // Merge properties from BOM. Existing properties take precedence.
                    Map<String, String> mergedProperties = new HashMap<>();
                    if (bomProject.getProperties() != null) {
                        mergedProperties.putAll(bomProject.getProperties());
                    }
                    mergedProperties.putAll(partialModel.getProperties()); // Original properties override BOM's
                    partialModel.setProperties(mergedProperties);

                    // Merge dependency management from BOM. Existing entries take precedence.
                    // This may introduce NEW scope=import entries (nested BOMs) which will be
                    // picked up in the next iteration of the while loop.
                    Map<String, PomXmlDependency> depMgmtMap = new HashMap<>();
                    if (bomProject.getDependencyManagement() != null) {
                        bomProject.getDependencyManagement().forEach(dep ->
                            depMgmtMap.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep));
                    }
                    // Original entries override BOM's entries
                    partialModel.getDependencyManagement().forEach(dep ->
                        depMgmtMap.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep));
                    partialModel.setDependencyManagement(new ArrayList<>(depMgmtMap.values()));

                    logger.debug("Merged BOM {}: {} properties, {} managed dependencies",
                        bomKey,
                        bomProject.getProperties() != null ? bomProject.getProperties().size() : 0,
                        bomProject.getDependencyManagement() != null ? bomProject.getDependencyManagement().size() : 0);
                } else {
                    logger.warn("Could not download BOM POM for coordinates: {}", bomKey);
                }

                // Remove THIS specific processed BOM from the dependency management list
                // We match by the RESOLVED coordinates to ensure correct removal
                final String finalGroupId = resolvedGroupId;
                final String finalArtifactId = resolvedArtifactId;
                partialModel.getDependencyManagement().removeIf(dep ->
                    finalGroupId.equals(resolveProperties(dep.getGroupId(), combinedProps)) &&
                    finalArtifactId.equals(resolveProperties(dep.getArtifactId(), combinedProps)) &&
                    SCOPE_IMPORT.equals(dep.getScope()));
            }

            iteration++;
        }

        if (iteration >= MAX_BOM_ITERATIONS) {
            logger.warn("Reached maximum BOM processing iterations ({}). Some nested BOMs may not be fully processed. " +
                "This could indicate a circular BOM import or an unusually deep BOM hierarchy.", MAX_BOM_ITERATIONS);
        }

        // Final cleanup: remove any remaining scope=import entries that couldn't be processed
        int remainingImports = (int) partialModel.getDependencyManagement().stream()
            .filter(dep -> SCOPE_IMPORT.equals(dep.getScope()))
            .count();
        if (remainingImports > 0) {
            logger.warn("Removing {} unprocessed BOM import(s) from dependency management", remainingImports);
        }
        partialModel.getDependencyManagement().removeIf(dep -> SCOPE_IMPORT.equals(dep.getScope()));

        logger.info("BOM processing complete: processed {} unique BOM(s) in {} iteration(s)",
            visitedBoms.size(), iteration);

        return partialModel;
    }

    /**
     * Builds a combined properties map including parent properties and current model properties.
     * Used for resolving property placeholders in BOM coordinates.
     */
    Map<String, String> buildCombinedProperties(PartialMavenProject partialModel) {
        Map<String, String> combinedProps = new HashMap<>();
        try {
            Map<String, String> parentProps = propertiesResolverProvider.getParentProperties();
            if (parentProps != null) {
                combinedProps.putAll(parentProps);
            }
        } catch (Exception e) {
            logger.debug("Failed to include parent properties while resolving BOM coords: {}", e.getMessage());
        }
        if (partialModel.getProperties() != null) {
            combinedProps.putAll(partialModel.getProperties());
        }
        return combinedProps;
    }

    /**
     * Builds a unique key for a BOM dependency using resolved coordinates.
     * Format: groupId:artifactId:version
     */
    private String buildBomKey(PomXmlDependency bom, Map<String, String> properties) {
        String groupId = resolveProperties(bom.getGroupId(), properties);
        String artifactId = resolveProperties(bom.getArtifactId(), properties);
        String version = resolveProperties(bom.getVersion(), properties);
        return groupId + ":" + artifactId + ":" + version;
    }

    /**
     * Resolves property placeholders in all dependency management entries of a BOM project.
     * This ensures ${project.version} and similar placeholders become literal values before merging.
     */
    private void resolveBomManagedEntries(PartialMavenProject bomProject) {
        try {
            Map<String, String> bomProps = new HashMap<>();
            if (bomProject.getProperties() != null) {
                bomProps.putAll(bomProject.getProperties());
            }
            // Substitute properties for all BOM-managed entries
            if (bomProject.getDependencyManagement() != null) {
                for (PomXmlDependency depMgmt : bomProject.getDependencyManagement()) {
                    depMgmt.setGroupId(resolveProperties(depMgmt.getGroupId(), bomProps));
                    depMgmt.setArtifactId(resolveProperties(depMgmt.getArtifactId(), bomProps));
                    depMgmt.setVersion(resolveProperties(depMgmt.getVersion(), bomProps));
                    depMgmt.setScope(resolveProperties(depMgmt.getScope(), bomProps));
                    depMgmt.setType(resolveProperties(depMgmt.getType(), bomProps));
                    depMgmt.setClassifier(resolveProperties(depMgmt.getClassifier(), bomProps));
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to resolve BOM managed entries in BOM context: {}", e.getMessage());
        }
    }

    String resolveProperties(String value, Map<String, String> properties) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        StringSubstitutor sub = new StringSubstitutor(properties);
        return sub.replace(value);
    }
}
