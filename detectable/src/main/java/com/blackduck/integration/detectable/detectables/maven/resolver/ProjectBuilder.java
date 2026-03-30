package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.detectable.detectables.maven.resolver.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ProjectBuilder is responsible for constructing a complete {@link MavenProject} from a POM file.
 * It parses POMs into {@link PartialMavenProject}, resolves properties (including those inherited
 * from parent POMs), processes imported BOMs (Bill of Materials), merges parent/child models,
 * applies dependency management, and converts POM dependency entries into {@link JavaDependency}
 * objects suitable for downstream processing.
 *
 * Responsibilities:
 * - Parse and interpolate POM files using {@link PomParser} and {@link PropertiesResolverProvider}.
 * - Download parent and BOM POMs via {@link MavenDownloader} when local parent POMs are not available.
 * - Merge properties, repositories, dependency management, and dependencies from parent/BOM models
 *   (delegated to {@link ModelMerger}).
 * - Process imported BOMs (delegated to {@link BomProcessor}).
 * - Apply dependency management to resolve missing versions/scopes and produce the final dependency
 *   list (delegated to {@link DependencyConverter}).
 *
 * The implementation caches partially-built projects to avoid duplicate work and detects parent
 * cycles to prevent infinite recursion. Instances are not thread-safe due to internal mutable state
 * (cache and resolver provider).
 *
 * Usage example:
 *   ProjectBuilder builder = new ProjectBuilder(downloadDir);
 *   MavenProject project = builder.buildProject(pomFile);
 */
public class ProjectBuilder {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final PomParser pomParser;
    private final PropertiesResolverProvider propertiesResolverProvider;
    private final Map<String, PartialMavenProject> pomCache = new HashMap<>();
    private final Path downloadDir;

    private final BomProcessor bomProcessor;
    private final ModelMerger modelMerger;
    private final DependencyConverter dependencyConverter;

    public ProjectBuilder(Path downloadDir) {
        this.pomParser = new PomParser();
        this.propertiesResolverProvider = new PropertiesResolverProvider(null, System::getenv);
        this.downloadDir = downloadDir;

        this.bomProcessor = new BomProcessor(downloadDir, this, propertiesResolverProvider);
        this.modelMerger = new ModelMerger();
        this.dependencyConverter = new DependencyConverter(propertiesResolverProvider);
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

    PartialMavenProject internalBuildProject(File pomFile, Set<String> identifiedParents) throws Exception {
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
                    return modelMerger.finalizeEffectiveModel(pomFilePath, pomFileInfo, null, pomCache);
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
                        return modelMerger.finalizeEffectiveModel(pomFilePath, interpolatedPomInfo, effectiveParentPom, pomCache);
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
                    return modelMerger.finalizeEffectiveModel(pomFilePath, interpolatedPomInfo, effectiveParentPom, pomCache);
                } else {
                    logger.warn("Could not retrieve parent pom for file: \"{}\"", pomFilePath);
                }
            }
        }

        return modelMerger.finalizeEffectiveModel(pomFilePath, pomFileInfo, null, pomCache);
    }

    private String buildGavKey(JavaCoordinates coords) {
        return coords.getGroupId() + ":" + coords.getArtifactId() + ":" + coords.getVersion();
    }

    private MavenProject toCompleteMavenProject(String pomFile, PartialMavenProject project) throws Exception {
        // 1. First pass of property resolution to resolve BOM coordinates
        logger.info("Starting first pass of property resolution for '{}'...", project.getCoordinates().getArtifactId());
        dependencyConverter.resolveDependencyProperties(project);

        // 2. Process BOMs (including nested BOMs), which may add new properties and managed dependencies.
        // The visitedBoms set tracks processed BOMs to prevent infinite cycles.
        // Process any imported BOMs so their properties and dependencyManagement entries
        // are merged into the partial model before the second property resolution pass.
        Set<String> visitedBoms = new HashSet<>();
        project = bomProcessor.processBoms(pomFile, project, visitedBoms);

        // 3. Second pass of property resolution to handle properties from imported BOMs
        logger.info("Starting second pass of property resolution for '{}' after processing BOMs...", project.getCoordinates().getArtifactId());
        dependencyConverter.resolveDependencyProperties(project);

        logger.info("Preparing to complete MavenProject. Final properties for resolution in '{}': {}", project.getCoordinates().getArtifactId(), project.getProperties().size());
//        project.getProperties().forEach((k, v) -> logger.info("  - Final Prop: {} = {}", k, v));

        // 4. Apply dependency management to the final set of dependencies
        List<JavaDependency> finalDependencies = dependencyConverter.applyDependencyManagement(project);

        List<JavaDependency> dependencyManagement = project.getDependencyManagement().stream()
            .map(dependencyConverter::convertPomXmlDependencyToJavaDependency)
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
}
