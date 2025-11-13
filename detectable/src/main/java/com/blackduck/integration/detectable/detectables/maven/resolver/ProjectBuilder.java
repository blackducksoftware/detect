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
            // Merge repositories
            List<JavaRepository> allRepositories = Stream.concat(partialModel.getRepositories().stream(), parentModel.getRepositories().stream())
                .distinct()
                .collect(Collectors.toList());
            partialModel.setRepositories(allRepositories);

            // Merge dependency management
            List<PomXmlDependency> allDepMgmt = Stream.concat(partialModel.getDependencyManagement().stream(), parentModel.getDependencyManagement().stream())
                .distinct()
                .collect(Collectors.toList());
            partialModel.setDependencyManagement(allDepMgmt);

            // Merge dependencies
            List<PomXmlDependency> allDeps = Stream.concat(partialModel.getDependencies().stream(), parentModel.getDependencies().stream())
                .distinct()
                .collect(Collectors.toList());
            partialModel.setDependencies(allDeps);
        }
        pomCache.put(path, partialModel);
        return partialModel;
    }

    private MavenProject toCompleteMavenProject(String pomFile, PartialMavenProject project) {
        // This is a simplified conversion. A real implementation would need to handle dependency management application.
        List<JavaDependency> dependencies = project.getDependencies().stream()
            .map(this::convertPomXmlDependencyToJavaDependency)
            .collect(Collectors.toList());

        List<JavaDependency> dependencyManagement = project.getDependencyManagement().stream()
            .map(this::convertPomXmlDependencyToJavaDependency)
            .collect(Collectors.toList());

        return new MavenProject(
            pomFile,
            project.getCoordinates(),
            project.getRepositories(),
            dependencies,
            dependencyManagement,
            project.getModules()
        );
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
