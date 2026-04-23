package com.blackduck.integration.detectable.detectables.maven.resolver.pom;

import com.blackduck.integration.detectable.detectables.maven.resolver.model.*;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.pomxml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles merging of parent and child Maven project models.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Merge properties (child overrides parent)</li>
 *   <li>Merge repositories (child overrides parent by ID)</li>
 *   <li>Merge dependency management (child overrides parent by G:A)</li>
 *   <li>Merge dependencies (child overrides parent by G:A)</li>
 * </ul>
 *
 * <p>This class is stateless and thread-safe.
 */
class ModelMerger {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    PartialMavenProject finalizeEffectiveModel(String path, PartialMavenProject partialModel, PartialMavenProject parentModel, Map<String, PartialMavenProject> pomCache) {
        if (parentModel != null) {
            mergeParentIntoChild(partialModel, parentModel);
        }
        pomCache.put(path, partialModel);
        return partialModel;
    }

    private void mergeParentIntoChild(PartialMavenProject child, PartialMavenProject parent) {
        logger.info("Merging models: child='{}' into parent='{}'", child.getCoordinates().getArtifactId(), parent.getCoordinates().getArtifactId());
        logger.info("Child properties before merge: {}", child.getProperties().size());
//            child.getProperties().forEach((k, v) -> logger.info("  - Child Prop: {} = {}", k, v));

        mergeProperties(child, parent);
        mergeRepositories(child, parent);
        mergeDependencyManagement(child, parent);
        mergeDependencies(child, parent);
    }

    private void mergeProperties(PartialMavenProject child, PartialMavenProject parent) {
        Map<String, String> merged = new HashMap<>();
        addAllIfNotNull(merged, parent.getProperties());
        addAllIfNotNull(merged, child.getProperties());
        child.setProperties(merged);
        logger.info("Merged properties count: {}", merged.size());
//            merged.forEach((k, v) -> logger.info("  - Merged Prop: {} = {}", k, v));
    }

    private void mergeRepositories(PartialMavenProject child, PartialMavenProject parent) {
        Map<String, JavaRepository> repoMap = new HashMap<>();
        if (parent.getRepositories() != null) {
            parent.getRepositories().forEach(repo -> repoMap.put(repo.getId(), repo));
        }
        if (child.getRepositories() != null) {
            child.getRepositories().forEach(repo -> repoMap.put(repo.getId(), repo));
        }
        child.setRepositories(new ArrayList<>(repoMap.values()));
    }

    private void mergeDependencyManagement(PartialMavenProject child, PartialMavenProject parent) {
        Map<String, PomXmlDependency> depMgmtMap = new LinkedHashMap<>();
        addDependenciesToMap(depMgmtMap, parent.getDependencyManagement());
        addDependenciesToMap(depMgmtMap, child.getDependencyManagement());
        child.setDependencyManagement(new ArrayList<>(depMgmtMap.values()));
    }

    private void mergeDependencies(PartialMavenProject child, PartialMavenProject parent) {
        Map<String, PomXmlDependency> dependenciesMap = new LinkedHashMap<>();
        addDependenciesToMap(dependenciesMap, parent.getDependencies());
        addDependenciesToMap(dependenciesMap, child.getDependencies());
        child.setDependencies(new ArrayList<>(dependenciesMap.values()));
    }

    private <K, V> void addAllIfNotNull(Map<K, V> target, Map<K, V> source) {
        if (source != null) {
            target.putAll(source);
        }
    }

    private void addDependenciesToMap(Map<String, PomXmlDependency> map, java.util.List<PomXmlDependency> dependencies) {
        if (dependencies != null) {
            dependencies.forEach(dep -> map.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep));
        }
    }
}
