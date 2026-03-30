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
            List<PomXmlDependency> unprocessedBoms = findUnprocessedBoms(partialModel, visitedBoms);

            if (unprocessedBoms.isEmpty()) {
                logger.debug("No more unprocessed BOMs found after {} iteration(s)", iteration);
                break;
            }

            logger.info("BOM processing iteration {}: found {} unprocessed BOM(s) in {}",
                iteration + 1, unprocessedBoms.size(), pomFilePath);

            processUnprocessedBoms(unprocessedBoms, partialModel, visitedBoms, iteration);
            iteration++;
        }

        logMaxIterationWarningIfNeeded(iteration);
        cleanupRemainingImports(partialModel);

        logger.info("BOM processing complete: processed {} unique BOM(s) in {} iteration(s)",
            visitedBoms.size(), iteration);

        return partialModel;
    }

    /**
     * Finds all unprocessed BOMs (scope=import entries not yet visited).
     */
    private List<PomXmlDependency> findUnprocessedBoms(PartialMavenProject partialModel, Set<String> visitedBoms) {
        final Map<String, String> currentProps = buildCombinedProperties(partialModel);

        return partialModel.getDependencyManagement().stream()
            .filter(dep -> SCOPE_IMPORT.equals(dep.getScope()))
            .filter(dep -> !visitedBoms.contains(buildBomKey(dep, currentProps)))
            .collect(Collectors.toList());
    }

    /**
     * Processes all unprocessed BOMs in the current iteration.
     */
    private void processUnprocessedBoms(
        List<PomXmlDependency> unprocessedBoms,
        PartialMavenProject partialModel,
        Set<String> visitedBoms,
        int iteration
    ) throws Exception {
        for (PomXmlDependency bom : unprocessedBoms) {
            processSingleBom(bom, partialModel, visitedBoms, iteration);
        }
    }

    /**
     * Processes a single BOM: resolves coordinates, downloads, merges, and removes from depMgmt.
     */
    private void processSingleBom(
        PomXmlDependency bom,
        PartialMavenProject partialModel,
        Set<String> visitedBoms,
        int iteration
    ) throws Exception {
        final Map<String, String> combinedProps = buildCombinedProperties(partialModel);
        ResolvedBomCoordinates resolved = resolveAndMarkBomAsVisited(bom, combinedProps, visitedBoms, iteration);

        PartialMavenProject bomProject = downloadAndBuildBomProject(resolved.coordinates, partialModel);
        if (bomProject != null) {
            mergeBomIntoModel(bomProject, partialModel, resolved.key);
        }

        removeProcessedBomFromDepMgmt(partialModel, resolved, combinedProps);
    }

    /**
     * Resolves BOM coordinates and marks the BOM as visited.
     */
    private ResolvedBomCoordinates resolveAndMarkBomAsVisited(
        PomXmlDependency bom,
        Map<String, String> combinedProps,
        Set<String> visitedBoms,
        int iteration
    ) {
        String resolvedGroupId = resolveProperties(bom.getGroupId(), combinedProps);
        String resolvedArtifactId = resolveProperties(bom.getArtifactId(), combinedProps);
        String resolvedVersion = resolveProperties(bom.getVersion(), combinedProps);
        String bomKey = resolvedGroupId + ":" + resolvedArtifactId + ":" + resolvedVersion;

        visitedBoms.add(bomKey);
        logger.debug("Processing BOM: {} (iteration {})", bomKey, iteration + 1);

        JavaCoordinates coords = new JavaCoordinates(resolvedGroupId, resolvedArtifactId, resolvedVersion, "pom");
        return new ResolvedBomCoordinates(coords, bomKey, resolvedGroupId, resolvedArtifactId);
    }

    /**
     * Downloads and builds the BOM project. Returns null if download fails.
     */
    private PartialMavenProject downloadAndBuildBomProject(
        JavaCoordinates bomCoords,
        PartialMavenProject partialModel
    ) throws Exception {
        MavenDownloader mavenDownloader = new MavenDownloader(partialModel.getRepositories(), downloadDir);
        File bomPomFile = mavenDownloader.downloadPom(bomCoords);

        if (bomPomFile == null) {
            logger.warn("Could not download BOM POM for coordinates: {}:{}:{}",
                bomCoords.getGroupId(), bomCoords.getArtifactId(), bomCoords.getVersion());
            return null;
        }

        logger.debug("Building BOM project: {}", bomPomFile.getAbsolutePath());
        PartialMavenProject bomProject = projectBuilder.internalBuildProject(bomPomFile, new HashSet<>());
        resolveBomManagedEntries(bomProject);
        return bomProject;
    }

    /**
     * Merges properties and dependency management from BOM into the partial model.
     */
    private void mergeBomIntoModel(PartialMavenProject bomProject, PartialMavenProject partialModel, String bomKey) {
        mergeProperties(bomProject, partialModel);
        mergeDependencyManagement(bomProject, partialModel);

        logger.debug("Merged BOM {}: {} properties, {} managed dependencies",
            bomKey,
            bomProject.getProperties() != null ? bomProject.getProperties().size() : 0,
            bomProject.getDependencyManagement() != null ? bomProject.getDependencyManagement().size() : 0);
    }

    /**
     * Merges properties from BOM into partial model. Original properties take precedence.
     */
    private void mergeProperties(PartialMavenProject bomProject, PartialMavenProject partialModel) {
        Map<String, String> mergedProperties = new HashMap<>();
        if (bomProject.getProperties() != null) {
            mergedProperties.putAll(bomProject.getProperties());
        }
        mergedProperties.putAll(partialModel.getProperties());
        partialModel.setProperties(mergedProperties);
    }

    /**
     * Merges dependency management from BOM into partial model. Original entries take precedence.
     */
    private void mergeDependencyManagement(PartialMavenProject bomProject, PartialMavenProject partialModel) {
        Map<String, PomXmlDependency> depMgmtMap = new HashMap<>();
        if (bomProject.getDependencyManagement() != null) {
            bomProject.getDependencyManagement().forEach(dep ->
                depMgmtMap.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep));
        }
        partialModel.getDependencyManagement().forEach(dep ->
            depMgmtMap.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep));
        partialModel.setDependencyManagement(new ArrayList<>(depMgmtMap.values()));
    }

    /**
     * Removes the processed BOM from the dependency management list.
     */
    private void removeProcessedBomFromDepMgmt(
        PartialMavenProject partialModel,
        ResolvedBomCoordinates resolved,
        Map<String, String> combinedProps
    ) {
        partialModel.getDependencyManagement().removeIf(dep ->
            resolved.groupId.equals(resolveProperties(dep.getGroupId(), combinedProps)) &&
            resolved.artifactId.equals(resolveProperties(dep.getArtifactId(), combinedProps)) &&
            SCOPE_IMPORT.equals(dep.getScope()));
    }

    /**
     * Logs a warning if maximum iterations were reached.
     */
    private void logMaxIterationWarningIfNeeded(int iteration) {
        if (iteration >= MAX_BOM_ITERATIONS) {
            logger.warn("Reached maximum BOM processing iterations ({}). Some nested BOMs may not be fully processed. " +
                "This could indicate a circular BOM import or an unusually deep BOM hierarchy.", MAX_BOM_ITERATIONS);
        }
    }

    /**
     * Removes any remaining scope=import entries that couldn't be processed.
     */
    private void cleanupRemainingImports(PartialMavenProject partialModel) {
        int remainingImports = (int) partialModel.getDependencyManagement().stream()
            .filter(dep -> SCOPE_IMPORT.equals(dep.getScope()))
            .count();
        if (remainingImports > 0) {
            logger.warn("Removing {} unprocessed BOM import(s) from dependency management", remainingImports);
        }
        partialModel.getDependencyManagement().removeIf(dep -> SCOPE_IMPORT.equals(dep.getScope()));
    }

    /**
     * Holds resolved BOM coordinates and derived values.
     */
    private static class ResolvedBomCoordinates {
        final JavaCoordinates coordinates;
        final String key;
        final String groupId;
        final String artifactId;

        ResolvedBomCoordinates(JavaCoordinates coordinates, String key, String groupId, String artifactId) {
            this.coordinates = coordinates;
            this.key = key;
            this.groupId = groupId;
            this.artifactId = artifactId;
        }
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
