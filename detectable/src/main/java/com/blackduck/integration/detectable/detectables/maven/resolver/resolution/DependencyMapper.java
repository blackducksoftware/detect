package com.blackduck.integration.detectable.detectables.maven.resolver.resolution;

import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaDependency;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps JavaDependency objects (from POM parsing) to Aether Dependency objects.
 *
 * <p>Handles version resolution (applying managed versions from BOM/dependencyManagement
 * when declared versions are missing or unresolved), exclusion mapping, and artifact
 * type/classifier handling.
 *
 * <p>This class eliminates code duplication between mapping direct dependencies and
 * managed dependencies by providing a unified mapping implementation.
 */
public class DependencyMapper {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String, String> managedVersionByGa;

    /**
     * Constructs a DependencyMapper with managed versions from dependencyManagement.
     *
     * @param managedDependencies List of managed dependencies (from BOM or dependencyManagement)
     */
    public DependencyMapper(List<JavaDependency> managedDependencies) {
        // Build a map of managed versions keyed by group:artifact
        this.managedVersionByGa = managedDependencies.stream()
            .collect(Collectors.toMap(
                d -> d.getCoordinates().getGroupId() + ":" + d.getCoordinates().getArtifactId(),
                d -> d.getCoordinates().getVersion(),
                (a, b) -> a // If duplicate keys, keep first
            ));
    }

    /**
     * Maps project dependencies to Aether Dependency objects.
     *
     * <p>Applies managed versions when declared versions are missing or unresolved.
     * Skips dependencies with unresolved versions.
     *
     * @param dependencies List of project dependencies
     * @return List of Aether Dependency objects (filtered, non-null)
     */
    public List<Dependency> mapDependencies(List<JavaDependency> dependencies) {
        return dependencies.stream()
            .map(this::mapSingleDependency)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Maps managed dependencies to Aether Dependency objects.
     *
     * <p>Filters out dependencies with unresolved versions.
     *
     * @param managedDependencies List of managed dependencies
     * @return List of Aether Dependency objects (filtered, non-null)
     */
    public List<Dependency> mapManagedDependencies(List<JavaDependency> managedDependencies) {
        return managedDependencies.stream()
            .filter(this::isVersionResolved)
            .map(this::mapSingleDependencyWithoutVersionResolution)
            .collect(Collectors.toList());
    }

    /**
     * Maps a single JavaDependency to an Aether Dependency.
     *
     * <p>Applies version resolution: prefers declared version if resolved,
     * otherwise tries to use managed version from BOM/dependencyManagement.
     *
     * @param dep The JavaDependency to map
     * @return Aether Dependency, or null if version cannot be resolved
     */
    private Dependency mapSingleDependency(JavaDependency dep) {
        String groupId = dep.getCoordinates().getGroupId();
        String artifactId = dep.getCoordinates().getArtifactId();
        String declaredVersion = dep.getCoordinates().getVersion();

        // Determine effective version: prefer declared if resolved; otherwise try managed (BOM)
        String effectiveVersion = resolveVersion(groupId, artifactId, declaredVersion);

        // If still unresolved, skip this dependency
        if (effectiveVersion == null || effectiveVersion.isEmpty() || effectiveVersion.contains("${")) {
            logger.info("Skipping dependency with unresolved version: {}:{}:{}", groupId, artifactId, declaredVersion);
            return null;
        }

        logger.info("Mapping dependency to Aether Artifact: {}:{}:{}:{}:{} (scope={})",
            groupId, artifactId,
            dep.getClassifier() == null ? "" : dep.getClassifier(),
            dep.getType(), effectiveVersion, dep.getScope());

        return buildDependency(dep, effectiveVersion);
    }

    /**
     * Maps a single JavaDependency to an Aether Dependency without version resolution.
     *
     * <p>Used for managed dependencies which already have resolved versions.
     *
     * @param dep The JavaDependency to map
     * @return Aether Dependency
     */
    private Dependency mapSingleDependencyWithoutVersionResolution(JavaDependency dep) {
        String version = dep.getCoordinates().getVersion();
        return buildDependency(dep, version);
    }

    /**
     * Resolves the effective version for a dependency.
     *
     * <p>Resolution strategy:
     * <ol>
     *   <li>Use declared version if it's resolved (not null, not empty, no ${...} placeholders)</li>
     *   <li>If declared is unresolved, try to find managed version from dependencyManagement</li>
     *   <li>If managed version is also unresolved, return the original declared version</li>
     * </ol>
     *
     * @param groupId Group ID
     * @param artifactId Artifact ID
     * @param declaredVersion Declared version (may be null or contain ${...})
     * @return Effective version to use, or null/unresolved if none found
     */
    private String resolveVersion(String groupId, String artifactId, String declaredVersion) {
        String effectiveVersion = declaredVersion;
        boolean declaredUnresolved = (effectiveVersion == null || effectiveVersion.isEmpty() || effectiveVersion.contains("${"));

        if (declaredUnresolved) {
            String key = groupId + ":" + artifactId;
            String managedVersion = managedVersionByGa.get(key);
            if (managedVersion != null && !managedVersion.isEmpty() && !managedVersion.contains("${")) {
                effectiveVersion = managedVersion;
            }
        }

        return effectiveVersion;
    }

    /**
     * Builds an Aether Dependency from a JavaDependency and resolved version.
     *
     * @param dep The JavaDependency
     * @param version The resolved version
     * @return Aether Dependency with exclusions
     */
    private Dependency buildDependency(JavaDependency dep, String version) {
        String groupId = dep.getCoordinates().getGroupId();
        String artifactId = dep.getCoordinates().getArtifactId();
        String type = dep.getType();
        String classifier = dep.getClassifier();

        // Default type to "jar" if not specified
        if (type == null || type.trim().isEmpty()) {
            type = "jar";
        }

        // Build artifact with or without classifier
        org.eclipse.aether.artifact.Artifact artifact;
        if (classifier != null && !classifier.trim().isEmpty()) {
            artifact = new DefaultArtifact(groupId, artifactId, classifier, type, version);
        } else {
            artifact = new DefaultArtifact(groupId, artifactId, type, version);
        }

        // Map exclusions
        List<Exclusion> aetherExclusions = mapExclusions(dep);

        // Create Dependency with or without exclusions
        if (aetherExclusions.isEmpty()) {
            return new Dependency(artifact, dep.getScope());
        } else {
            return new Dependency(artifact, dep.getScope(), false, aetherExclusions);
        }
    }

    /**
     * Maps JavaDependency exclusions to Aether Exclusion objects.
     *
     * @param dep The JavaDependency containing exclusions
     * @return List of Aether Exclusions
     */
    private List<Exclusion> mapExclusions(JavaDependency dep) {
        List<Exclusion> aetherExclusions = new ArrayList<>();

        if (dep.getExclusions() != null) {
            dep.getExclusions().forEach(ex -> {
                String exclGroup = (ex.getGroupId() == null || ex.getGroupId().trim().isEmpty()) ? "*" : ex.getGroupId();
                String exclArtifact = (ex.getArtifactId() == null || ex.getArtifactId().trim().isEmpty()) ? "*" : ex.getArtifactId();
                // Use wildcard for classifier and extension
                aetherExclusions.add(new Exclusion(exclGroup, exclArtifact, "*", "*"));
            });
        }

        return aetherExclusions;
    }

    /**
     * Checks if a dependency has a resolved version.
     *
     * @param dependency The dependency to check
     * @return true if version is resolved (not null, not empty, no ${...} placeholders)
     */
    private boolean isVersionResolved(JavaDependency dependency) {
        String version = dependency.getCoordinates().getVersion();
        if (version == null || version.isEmpty() || version.contains("${")) {
            logger.info("Skipping dependency with unresolved version: {}:{}:{}",
                dependency.getCoordinates().getGroupId(),
                dependency.getCoordinates().getArtifactId(),
                version);
            return false;
        }
        return true;
    }
}
