package com.blackduck.integration.detectable.detectables.maven.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.text.StringSubstitutor;

public class ProjectBuilder {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PomParser pomParser;
    private final PropertiesResolverProvider propertiesResolverProvider;
    private final Map<String, PartialMavenProject> pomCache = new HashMap<>();
    private final Path downloadDir;

    public ProjectBuilder(Path downloadDir) {
        this.pomParser = new PomParser();
        this.propertiesResolverProvider = new PropertiesResolverProvider(null, System::getenv);
        this.downloadDir = downloadDir;
    }

    public MavenProject buildProject(File pomFile) throws Exception {
        PartialMavenProject partialMavenProject = internalBuildProject(pomFile, new HashSet<>());
        return toCompleteMavenProject(pomFile.getAbsolutePath(), partialMavenProject);
    }

    private PartialMavenProject internalBuildProject(File pomFile, Set<String> identifiedParents) throws Exception {
        String pomFilePath = pomFile.getAbsolutePath();
        logger.debug("Building project file \"{}\"", pomFilePath);

        if (pomCache.containsKey(pomFilePath)) {
            logger.debug("Retrieved project file \"{}\" from cache", pomFilePath);
            return pomCache.get(pomFilePath);
        }

        byte[] content = Files.readAllBytes(pomFile.toPath());
        PartialMavenProject pomFileInfo = pomParser.parsePomFile(pomFilePath, content, propertiesResolverProvider);

        if (pomFileInfo.getParentPomInfo() != null && pomFileInfo.getParentPomInfo().getCoordinates() != null) {
            JavaCoordinates parentCoords = pomFileInfo.getParentPomInfo().getCoordinates();
            if (parentCoords.getGroupId() != null && parentCoords.getArtifactId() != null && parentCoords.getVersion() != null) {
                if (identifiedParents.contains(parentCoords.toString())) {
                    logger.debug("Cycle detected, treating file \"{}\" as direct super pom descendant.", pomFilePath);
                    return finalizeEffectiveModel(pomFilePath, pomFileInfo, null);
                }
                identifiedParents.add(parentCoords.toString());

                MavenDownloader mavenDownloader = new MavenDownloader(pomFileInfo.getRepositories(), downloadDir);
                File parentPomFile = mavenDownloader.downloadPom(parentCoords);

                if (parentPomFile != null) {
                    logger.debug("Building effective pom for parent pom \"{}\" ...", parentPomFile.getAbsolutePath());
                    PartialMavenProject effectiveParentPom = internalBuildProject(parentPomFile, identifiedParents);
                    logger.debug("Built effective pom for parent pom \"{}\".", parentPomFile.getAbsolutePath());
                    logger.info("Parent ({}) properties found: {}", effectiveParentPom.getCoordinates().getArtifactId(), effectiveParentPom.getProperties().size());
                    effectiveParentPom.getProperties().forEach((k, v) -> logger.info("  - Parent Prop: {} = {}", k, v));


                    propertiesResolverProvider.setParentProperties(effectiveParentPom.getProperties());
                    PartialMavenProject interpolatedPomInfo = pomParser.parsePomFile(pomFilePath, content, propertiesResolverProvider);
                    return finalizeEffectiveModel(pomFilePath, interpolatedPomInfo, effectiveParentPom);
                } else {
                    logger.warn("Could not retrieve parent pom for file: \"{}\"", pomFilePath);
                }
            }
        }

        return finalizeEffectiveModel(pomFilePath, pomFileInfo, null);
    }

    private PartialMavenProject processBoms(String pomFilePath, PartialMavenProject partialModel) throws Exception {
        List<PomXmlDependency> bomImports = partialModel.getDependencyManagement().stream()
            .filter(dep -> "import".equals(dep.getScope()))
            .collect(Collectors.toList());

        if (bomImports.isEmpty()) {
            return partialModel;
        }

        logger.info("Found {} BOM imports to process in {}", bomImports.size(), pomFilePath);

        for (PomXmlDependency bom : bomImports) {
            JavaCoordinates bomCoords = new JavaCoordinates(bom.getGroupId(), bom.getArtifactId(), bom.getVersion(), "pom");
            MavenDownloader mavenDownloader = new MavenDownloader(partialModel.getRepositories(), downloadDir);
            File bomPomFile = mavenDownloader.downloadPom(bomCoords);

            if (bomPomFile != null) {
                logger.debug("Building BOM project: {}", bomPomFile.getAbsolutePath());
                PartialMavenProject bomProject = internalBuildProject(bomPomFile, new HashSet<>());

                // Merge properties from BOM. Existing properties take precedence.
                Map<String, String> mergedProperties = new HashMap<>();
                if (bomProject.getProperties() != null) {
                    mergedProperties.putAll(bomProject.getProperties());
                }
                mergedProperties.putAll(partialModel.getProperties()); // Original properties override BOM's
                partialModel.setProperties(mergedProperties);

                // Merge dependency management from BOM. Existing entries take precedence.
                Map<String, PomXmlDependency> depMgmtMap = new HashMap<>();
                if (bomProject.getDependencyManagement() != null) {
                    bomProject.getDependencyManagement().forEach(dep -> depMgmtMap.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep));
                }
                partialModel.getDependencyManagement().forEach(dep -> depMgmtMap.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep)); // Original entries override BOM's
                partialModel.setDependencyManagement(new ArrayList<>(depMgmtMap.values()));
            } else {
                logger.warn("Could not download BOM POM for coordinates: {}", bomCoords);
            }
        }

        // Remove the 'import' scoped dependencies themselves from the list
        partialModel.getDependencyManagement().removeIf(dep -> "import".equals(dep.getScope()));

        return partialModel;
    }

    private PartialMavenProject finalizeEffectiveModel(String path, PartialMavenProject partialModel, PartialMavenProject parentModel) {
        if (parentModel != null) {
            logger.info("Merging models: child='{}' into parent='{}'", partialModel.getCoordinates().getArtifactId(), parentModel.getCoordinates().getArtifactId());
            logger.info("Child properties before merge: {}", partialModel.getProperties().size());
            partialModel.getProperties().forEach((k, v) -> logger.info("  - Child Prop: {} = {}", k, v));

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
            mergedProperties.forEach((k, v) -> logger.info("  - Merged Prop: {} = {}", k, v));

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
            Map<String, PomXmlDependency> depsMap = new HashMap<>();
            if (parentModel.getDependencies() != null) {
                parentModel.getDependencies().forEach(dep -> depsMap.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep));
            }
            if (partialModel.getDependencies() != null) {
                partialModel.getDependencies().forEach(dep -> depsMap.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep));
            }
            partialModel.setDependencies(new ArrayList<>(depsMap.values()));
        }
        pomCache.put(path, partialModel);
        return partialModel;
    }

    private MavenProject toCompleteMavenProject(String pomFile, PartialMavenProject project) throws Exception {
        // 1. First pass of property resolution to resolve BOM coordinates
        logger.info("Starting first pass of property resolution for '{}'...", project.getCoordinates().getArtifactId());
        resolveDependencyProperties(project);

        // 2. Process BOMs, which may add new properties and managed dependencies
        project = processBoms(pomFile, project);

        // 3. Second pass of property resolution to handle properties from imported BOMs
        logger.info("Starting second pass of property resolution for '{}' after processing BOMs...", project.getCoordinates().getArtifactId());
        resolveDependencyProperties(project);

        logger.info("Preparing to complete MavenProject. Final properties for resolution in '{}': {}", project.getCoordinates().getArtifactId(), project.getProperties().size());
        project.getProperties().forEach((k, v) -> logger.info("  - Final Prop: {} = {}", k, v));

        // 4. Apply dependency management to the final set of dependencies
        List<JavaDependency> finalDependencies = applyDependencyManagement(project);

        List<JavaDependency> dependencyManagement = project.getDependencyManagement().stream()
            .map(this::convertPomXmlDependencyToJavaDependency)
            .collect(Collectors.toList());

        return new MavenProject(
            pomFile,
            project.getCoordinates(),
            project.getRepositories(),
            finalDependencies,
            dependencyManagement,
            project.getModules()
        );
    }

    private void resolveDependencyProperties(PartialMavenProject project) {
        // Create a complete map of properties for resolution, including the project's own coordinates.
        Map<String, String> allProps = new HashMap<>(project.getProperties());
        JavaCoordinates coords = project.getCoordinates();
        if (coords.getGroupId() != null) {
            allProps.put("project.groupId", coords.getGroupId());
            allProps.put("pom.groupId", coords.getGroupId());
        }
        if (coords.getArtifactId() != null) {
            allProps.put("project.artifactId", coords.getArtifactId());
            allProps.put("pom.artifactId", coords.getArtifactId());
        }
        if (coords.getVersion() != null) {
            allProps.put("project.version", coords.getVersion());
            allProps.put("pom.version", coords.getVersion());
        }

        // Resolve properties for dependencies
        for (PomXmlDependency dep : project.getDependencies()) {
            dep.setGroupId(resolveProperties(dep.getGroupId(), allProps));
            dep.setArtifactId(resolveProperties(dep.getArtifactId(), allProps));
            dep.setVersion(resolveProperties(dep.getVersion(), allProps));
        }
        // Resolve properties for dependency management
        for (PomXmlDependency dep : project.getDependencyManagement()) {
            dep.setGroupId(resolveProperties(dep.getGroupId(), allProps));
            dep.setArtifactId(resolveProperties(dep.getArtifactId(), allProps));
            dep.setVersion(resolveProperties(dep.getVersion(), allProps));
        }
    }

    private String resolveProperties(String value, Map<String, String> properties) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        // Use StringSubstitutor for more robust property replacement.
        StringSubstitutor sub = new StringSubstitutor(properties);
        return sub.replace(value);
    }

    private List<JavaDependency> applyDependencyManagement(PartialMavenProject project) {
        Map<String, PomXmlDependency> managedDependencies = project.getDependencyManagement().stream()
            .collect(Collectors.toMap(dep -> dep.getGroupId() + ":" + dep.getArtifactId(), dep -> dep));

        return project.getDependencies().stream()
            .map(dep -> {
                PomXmlDependency managed = managedDependencies.get(dep.getGroupId() + ":" + dep.getArtifactId());
                if (managed != null) {
                    if (dep.getVersion() == null && managed.getVersion() != null) {
                        dep.setVersion(managed.getVersion());
                    }
                    if (dep.getScope() == null && managed.getScope() != null) {
                        dep.setScope(managed.getScope());
                    }
                }
                return convertPomXmlDependencyToJavaDependency(dep);
            })
            .collect(Collectors.toList());
    }

    private JavaDependency convertPomXmlDependencyToJavaDependency(PomXmlDependency pomDep) {
        List<JavaDependencyExclusion> exclusions = new ArrayList<>();
        if (pomDep.getExclusions() != null) {
            exclusions = pomDep.getExclusions().stream()
                .map(exclusion -> new JavaDependencyExclusion(exclusion.getGroupId(), exclusion.getArtifactId()))
                .collect(Collectors.toList());
        }

        JavaCoordinates coordinates = new JavaCoordinates(pomDep.getGroupId(), pomDep.getArtifactId(), pomDep.getVersion(), pomDep.getType());

        return new JavaDependency(
            coordinates,
            pomDep.getScope(),
            pomDep.getType(),
            pomDep.getClassifier(),
            exclusions
        );
    }
}
