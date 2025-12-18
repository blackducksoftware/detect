package com.blackduck.integration.detectable.detectables.maven.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
        // Use canonical path to ensure consistent cache keys and avoid duplicate processing
        File canonicalPom = pomFile;
        try {
            canonicalPom = pomFile.getCanonicalFile();
        } catch (IOException e) {
            logger.debug("Could not canonicalize pom file path {}, falling back to provided file: {}", pomFile, e.getMessage());
        }

        PartialMavenProject partialMavenProject = internalBuildProject(canonicalPom, new HashSet<>());
        return toCompleteMavenProject(canonicalPom.getCanonicalPath(), partialMavenProject);
    }

    private PartialMavenProject internalBuildProject(File pomFile, Set<String> identifiedParents) throws Exception {
        // Canonicalize the file to normalize path segments like '..' and symlinks so cache keys match
        File canonicalFile = pomFile;
        try {
            canonicalFile = pomFile.getCanonicalFile();
        } catch (IOException e) {
            logger.debug("Could not canonicalize pom file path {}, continuing with absolute path: {}", pomFile, e.getMessage());
            // fallback to provided file
        }

        String pomFilePath = canonicalFile.getCanonicalPath();
        logger.debug("Building project file \"{}\"", pomFilePath);

        if (pomCache.containsKey(pomFilePath)) {
            logger.debug("Retrieved project file \"{}\" from cache", pomFilePath);
            return pomCache.get(pomFilePath);
        }

        byte[] content = Files.readAllBytes(canonicalFile.toPath());
        PartialMavenProject pomFileInfo = pomParser.parsePomFile(pomFilePath, content, propertiesResolverProvider);

        if (pomFileInfo.getParentPomInfo() != null && pomFileInfo.getParentPomInfo().getCoordinates() != null) {
            JavaCoordinates parentCoords = pomFileInfo.getParentPomInfo().getCoordinates();
            if (parentCoords.getGroupId() != null && parentCoords.getArtifactId() != null && parentCoords.getVersion() != null) {
                String parentKey = buildGavKey(parentCoords);
                if (identifiedParents.contains(parentKey)) {
                    logger.debug("Cycle detected for parent key '{}', treating file \"{}\" as direct super pom descendant.", parentKey, pomFilePath);
                    return finalizeEffectiveModel(pomFilePath, pomFileInfo, null);
                }
                identifiedParents.add(parentKey);

                // First attempt: check expected local parent path (relativePath or default ../pom.xml)
                String expectedParentPath = pomFileInfo.getParentPomInfo().getExpectedPath();
                if (expectedParentPath != null && !expectedParentPath.isEmpty()) {
                    File expectedParentFile = new File(expectedParentPath);
                    try {
                        expectedParentFile = expectedParentFile.getCanonicalFile();
                    } catch (IOException e) {
                        logger.debug("Could not canonicalize expected parent path {}, using raw path: {}", expectedParentPath, e.getMessage());
                    }
                    if (expectedParentFile.exists() && expectedParentFile.isFile()) {
                        logger.info("Found local parent POM at expected path: {}", expectedParentFile.getAbsolutePath());
                        PartialMavenProject effectiveParentPom = internalBuildProject(expectedParentFile, identifiedParents);
                        propertiesResolverProvider.setParentProperties(effectiveParentPom.getProperties());
                        PartialMavenProject interpolatedPomInfo = pomParser.parsePomFile(pomFilePath, content, propertiesResolverProvider);
                        return finalizeEffectiveModel(pomFilePath, interpolatedPomInfo, effectiveParentPom);
                    } else {
                        logger.debug("No local parent POM found at expected path: {}", expectedParentPath);
                    }
                }

                MavenDownloader mavenDownloader = new MavenDownloader(pomFileInfo.getRepositories(), downloadDir);
                File parentPomFile = mavenDownloader.downloadPom(parentCoords);

                if (parentPomFile != null) {
                    // Use canonical file when recursing
                    File canonicalParent = parentPomFile;
                    try {
                        canonicalParent = parentPomFile.getCanonicalFile();
                    } catch (IOException e) {
                        logger.debug("Could not canonicalize downloaded parent pom path {}, proceeding with returned file: {}", parentPomFile, e.getMessage());
                    }

                    logger.debug("Building effective pom for parent pom \"{}\" ...", canonicalParent.getAbsolutePath());
                    PartialMavenProject effectiveParentPom = internalBuildProject(canonicalParent, identifiedParents);
                    logger.debug("Built effective pom for parent pom \"{}\".", canonicalParent.getAbsolutePath());
                    logger.info("Parent ({}) properties found: {}", effectiveParentPom.getCoordinates().getArtifactId(), effectiveParentPom.getProperties().size());
//                    effectiveParentPom.getProperties().forEach((k, v) -> logger.info("  - Parent Prop: {} = {}", k, v));


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

    private String buildGavKey(JavaCoordinates coords) {
        return coords.getGroupId() + ":" + coords.getArtifactId() + ":" + coords.getVersion();
    }

    private void resolveDependencyProperties(PartialMavenProject project) {
        // Create a complete map of properties for resolution.
        // Build a combined properties map that includes parent properties so
        // BOM coordinates that use parent/root properties (e.g. ${spring-cloud-dependencies.version})
        // can be resolved during the first pass.
        Map<String, String> allProps = new HashMap<>();
        try {
            Map<String, String> parentProps = propertiesResolverProvider.getParentProperties();
            if (parentProps != null) {
                allProps.putAll(parentProps);
            }
        } catch (Exception e) {
            logger.debug("Failed to include parent properties in resolution map: {}", e.getMessage());
        }
        if (project.getProperties() != null) {
            allProps.putAll(project.getProperties());
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
                    if (isEmpty(dep.getVersion()) && !isEmpty(managed.getVersion())) {
                        dep.setVersion(managed.getVersion());
                    }
                    if (isEmpty(dep.getScope()) && !isEmpty(managed.getScope())) {
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

    private PartialMavenProject processBoms(String pomFilePath, PartialMavenProject partialModel) throws Exception {
        // NOTE: This method was previously defined earlier; we're providing an updated implementation
        // that resolves BOM coordinates against combined properties before attempting downloads.
        List<PomXmlDependency> bomImports = partialModel.getDependencyManagement().stream()
            .filter(dep -> "import".equals(dep.getScope()))
            .collect(Collectors.toList());

        if (bomImports.isEmpty()) {
            return partialModel;
        }

        logger.info("Found {} BOM imports to process in {}", bomImports.size(), pomFilePath);

        for (PomXmlDependency bom : bomImports) {
            // Build a combined properties map including parent properties to resolve BOM coordinates
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

            String resolvedGroupId = resolveProperties(bom.getGroupId(), combinedProps);
            String resolvedArtifactId = resolveProperties(bom.getArtifactId(), combinedProps);
            String resolvedVersion = resolveProperties(bom.getVersion(), combinedProps);

            JavaCoordinates bomCoords = new JavaCoordinates(resolvedGroupId, resolvedArtifactId, resolvedVersion, "pom");
            MavenDownloader mavenDownloader = new MavenDownloader(partialModel.getRepositories(), downloadDir);
            File bomPomFile = mavenDownloader.downloadPom(bomCoords);

            if (bomPomFile != null) {
                logger.debug("Building BOM project: {}", bomPomFile.getAbsolutePath());
                PartialMavenProject bomProject = internalBuildProject(bomPomFile, new HashSet<>());

                // Resolve BOM dependencyManagement entries in BOM context before merging
                try {
                    Map<String, String> bomProps = new HashMap<>();
                    if (bomProject.getProperties() != null) {
                        bomProps.putAll(bomProject.getProperties());
                    }
                    // Substitute properties for all BOM-managed entries so ${project.version} etc. become literals
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
                logger.warn("Could not download BOM POM for coordinates: {}:{}:{}", resolvedGroupId, resolvedArtifactId, resolvedVersion);
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
        // Process any imported BOMs so their properties and dependencyManagement entries
        // are merged into the partial model before the second property resolution pass.
        project = processBoms(pomFile, project);

        // 3. Second pass of property resolution to handle properties from imported BOMs
        logger.info("Starting second pass of property resolution for '{}' after processing BOMs...", project.getCoordinates().getArtifactId());
        resolveDependencyProperties(project);

        logger.info("Preparing to complete MavenProject. Final properties for resolution in '{}': {}", project.getCoordinates().getArtifactId(), project.getProperties().size());
//        project.getProperties().forEach((k, v) -> logger.info("  - Final Prop: {} = {}", k, v));

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

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
