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

    private PartialMavenProject finalizeEffectiveModel(String path, PartialMavenProject partialModel, PartialMavenProject parentModel) {
        if (parentModel != null) {
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

    private MavenProject toCompleteMavenProject(String pomFile, PartialMavenProject project) {
        // 1. Resolve properties in dependencies
        resolveDependencyProperties(project);

        // 2. Apply dependency management
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
        // A simple string replacer, a more robust solution would use a library like Apache Commons Text
        for (PomXmlDependency dep : project.getDependencies()) {
            dep.setGroupId(resolveProperties(dep.getGroupId(), project.getProperties()));
            dep.setArtifactId(resolveProperties(dep.getArtifactId(), project.getProperties()));
            dep.setVersion(resolveProperties(dep.getVersion(), project.getProperties()));
        }
        for (PomXmlDependency dep : project.getDependencyManagement()) {
            dep.setGroupId(resolveProperties(dep.getGroupId(), project.getProperties()));
            dep.setArtifactId(resolveProperties(dep.getArtifactId(), project.getProperties()));
            dep.setVersion(resolveProperties(dep.getVersion(), project.getProperties()));
        }
    }

    private String resolveProperties(String value, Map<String, String> properties) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            value = value.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return value;
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
