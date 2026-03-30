package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.detectable.detectables.maven.resolver.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
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
            logger.info("Merging models: child='{}' into parent='{}'", partialModel.getCoordinates().getArtifactId(), parentModel.getCoordinates().getArtifactId());
            logger.info("Child properties before merge: {}", partialModel.getProperties().size());
//            partialModel.getProperties().forEach((k, v) -> logger.info("  - Child Prop: {} = {}", k, v));

            // Merge properties: Child's properties override parent's.
            Map<String, String> mergedProperties = new HashMap<>();
            if (parentModel.getProperties() != null) {
                mergedProperties.putAll(parentModel.getProperties());
            }
            if (partialModel.getProperties() != null) {
                mergedProperties.putAll(partialModel.getProperties());
            }
            partialModel.setProperties(mergedProperties);
            logger.info("Merged properties count: {}", mergedProperties.size());
//            mergedProperties.forEach((k, v) -> logger.info("  - Merged Prop: {} = {}", k, v));

            // Merge repositories: Child overrides parent by ID
            Map<String, JavaRepository> repoMap = new HashMap<>();
            if (parentModel.getRepositories() != null) {
                parentModel.getRepositories().forEach(repo -> repoMap.put(repo.getId(), repo));
            }
            if (partialModel.getRepositories() != null) {
                partialModel.getRepositories().forEach(repo -> repoMap.put(repo.getId(), repo));
            }
            partialModel.setRepositories(new ArrayList<>(repoMap.values()));

            // Merge dependency management: Child overrides parent by G:A
            Map<String, PomXmlDependency> depMgmtMap = new HashMap<>();
            if (parentModel.getDependencyManagement() != null) {
                parentModel.getDependencyManagement().forEach(dep -> depMgmtMap.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep));
            }
            if (partialModel.getDependencyManagement() != null) {
                partialModel.getDependencyManagement().forEach(dep -> depMgmtMap.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep));
            }
            partialModel.setDependencyManagement(new ArrayList<>(depMgmtMap.values()));

            // Merge dependencies: Child overrides parent by G:A
            Map<String, PomXmlDependency> dependenciesMap = new HashMap<>();
            if (parentModel.getDependencies() != null) {
                parentModel.getDependencies().forEach(dep -> dependenciesMap.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep));
            }
            if (partialModel.getDependencies() != null) {
                partialModel.getDependencies().forEach(dep -> dependenciesMap.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep));
            }
            partialModel.setDependencies(new ArrayList<>(dependenciesMap.values()));
        }
        pomCache.put(path, partialModel);
        return partialModel;
    }
}
