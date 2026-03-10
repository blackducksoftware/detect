package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.FileNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model.DiscoveredDependency;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.detectable.detectables.maven.resolver.result.MavenParseResult;
import com.blackduck.integration.util.NameVersion;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.DependencyNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maven Resolver Detectable - Detects and resolves Maven project dependencies.
 *
 * <p>This detectable handles Maven projects by:
 * <ul>
 *   <li>Building effective POM models (including parent POM inheritance and BOM imports)</li>
 *   <li>Resolving dependencies using Eclipse Aether for both compile and test scopes</li>
 *   <li>Processing multi-module Maven projects recursively</li>
 *   <li>Generating dependency graphs and code locations for Black Duck analysis</li>
 * </ul>
 *
 * <p><strong>Detection Criteria:</strong> Presence of a pom.xml file in the project directory.
 *
 * <p><strong>Dependency Resolution:</strong> Uses Eclipse Aether to resolve transitive dependencies,
 * respecting Maven's dependency management, exclusions, and scope inheritance rules.
 *
 * <p><strong>Multi-Module Support:</strong> Recursively processes Maven modules, preventing cycles
 * and creating separate code locations for each module.
 *
 * @see ProjectBuilder for POM model construction
 * @see MavenDependencyResolver for Aether-based resolution
 */
@DetectableInfo(
        name = "Maven Resolver",
        language = "Java",
        forge = "Maven Central",
        accuracy = DetectableAccuracyType.HIGH,
        requirementsMarkdown = "File: pom.xml"
)
public class MavenResolverDetectable extends Detectable {

    // File and directory name constants
    private static final String POM_XML_FILENAME = "pom.xml";
    private static final String DOWNLOADS_DIR_NAME = "downloads";
    private static final String LOCAL_REPO_DIR_NAME = "local-repo";

    // Dependency tree output file naming constants (for root project files)
    private static final String DEPENDENCY_TREE_FILE_PREFIX = "dependency-tree-";
    private static final String COMPILE_SCOPE_SUFFIX = "compile";
    private static final String TEST_SCOPE_SUFFIX = "test";
    private static final String TREE_FILE_EXTENSION = ".txt";

    // Maven scope constants
    private static final String MAVEN_SCOPE_COMPILE = "compile";
    private static final String MAVEN_SCOPE_TEST = "test";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final FileFinder fileFinder;
    private final ExternalIdFactory externalIdFactory;
    private final MavenResolverOptions mavenResolverOptions;

    // Helper classes for code organization and reusability
    private final DependencyTreeFileWriter treeWriter;
    private final CodeLocationFactory codeLocationFactory;
    private final MavenCoordinateFormatter coordinateFormatter;
    private final MavenModuleProcessor moduleProcessor;

    // Cached POM file location from applicable() phase
    private File pomFile;

    /**
     * Constructs a Maven Resolver Detectable.
     *
     * @param environment The detectable environment providing context for detection
     * @param fileFinder Utility for locating files in the project directory
     * @param externalIdFactory Factory for creating Maven external identifiers for code locations
     * @param mavenResolverOptions options including artifact download settings
     */
    public MavenResolverDetectable(
            DetectableEnvironment environment,
            FileFinder fileFinder,
            ExternalIdFactory externalIdFactory,
            MavenResolverOptions mavenResolverOptions
    ) {
        super(environment);
        this.fileFinder = fileFinder;
        this.externalIdFactory = externalIdFactory;
        // Initialize helper classes
        this.treeWriter = new DependencyTreeFileWriter();
        this.codeLocationFactory = new CodeLocationFactory(externalIdFactory);
        this.coordinateFormatter = new MavenCoordinateFormatter();
        this.moduleProcessor = new MavenModuleProcessor();
        this.mavenResolverOptions = mavenResolverOptions;
    }

    /**
     * Determines if this detectable is applicable to the current project.
     *
     * <p>Checks for the presence of a pom.xml file in the project directory.
     * If found, the file reference is cached for later use in extraction.
     *
     * @return PassedDetectableResult if pom.xml exists, FileNotFoundDetectableResult otherwise
     */
    @Override
    public DetectableResult applicable() {
        pomFile = fileFinder.findFile(environment.getDirectory(), POM_XML_FILENAME);
        if (pomFile == null) {
            return new FileNotFoundDetectableResult(POM_XML_FILENAME);
        }
        return new PassedDetectableResult();
    }

    /**
     * Validates that the detected pom.xml file is extractable (exists and is readable).
     *
     * <p>This phase ensures the file still exists and has read permissions before
     * attempting the potentially expensive extraction process.
     *
     * @return PassedDetectableResult if pom.xml is readable, FileNotFoundDetectableResult otherwise
     */
    @Override
    public DetectableResult extractable() {
        // Ensure the pom file still exists and is readable before extraction.
        if (pomFile == null) {
            pomFile = fileFinder.findFile(environment.getDirectory(), POM_XML_FILENAME);
        }
        if (pomFile == null || !pomFile.exists() || !pomFile.isFile()) {
            return new FileNotFoundDetectableResult(POM_XML_FILENAME);
        }
        if (!pomFile.canRead()) {
            // Reuse FileNotFoundDetectableResult to indicate the prerequisite file cannot be accessed.
            return new FileNotFoundDetectableResult(POM_XML_FILENAME);
        }
        return new PassedDetectableResult();
    }

    /**
     * Extracts Maven dependency information from the detected pom.xml file.
     *
     * <p>This method orchestrates the complete Maven dependency extraction process:
     * <ol>
     *   <li><strong>Build Effective POM:</strong> Constructs the effective Maven project model by resolving
     *       parent POMs, importing BOMs, and applying property inheritance.</li>
     *   <li><strong>Resolve Dependencies:</strong> Uses Eclipse Aether to resolve both compile and test scope
     *       dependencies, respecting Maven's dependency management and exclusion rules.</li>
     *   <li><strong>Write Debug Trees:</strong> Outputs human-readable dependency trees for debugging purposes.</li>
     *   <li><strong>Transform to Graphs:</strong> Converts Aether's dependency structure to Black Duck's
     *       internal dependency graph format.</li>
     *   <li><strong>Process Modules:</strong> Recursively processes multi-module Maven projects, creating
     *       separate code locations for each module while preventing circular dependencies.</li>
     * </ol>
     *
     * <p><strong>Output Artifacts:</strong>
     * <ul>
     *   <li>Dependency tree files (compile and test scopes) for debugging</li>
     *   <li>Code locations for the root project and all modules</li>
     *   <li>Error marker files for any modules that fail to resolve</li>
     * </ul>
     *
     * @param extractionEnvironment Provides output directory and environment context for extraction
     * @return Extraction result containing code locations and project metadata, or an error if extraction fails
     */
    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment){
        try {
            // Initialize code locations list to collect all code locations (root + modules)
            List<CodeLocation> codeLocations = new ArrayList<>();

            // PHASE 1: Build the effective POM model
            // Create a download directory for parent POMs and BOM imports that need to be fetched from remote repositories
            Path downloadDir = extractionEnvironment.getOutputDirectory().toPath().resolve(DOWNLOADS_DIR_NAME);
            Files.createDirectories(downloadDir);
            ProjectBuilder projectBuilder = new ProjectBuilder(downloadDir);
            MavenProject mavenProject = projectBuilder.buildProject(pomFile);

            // Log the constructed MavenProject details for verification and debugging
            logger.info("Constructed MavenProject model for: {}", mavenProject.getPomFile());
            logger.info("  Coordinates: {}:{}:{}",
                mavenProject.getCoordinates().getGroupId(),
                mavenProject.getCoordinates().getArtifactId(),
                mavenProject.getCoordinates().getVersion());

            logger.info("  Dependencies found: {}", mavenProject.getDependencies().size());
            mavenProject.getDependencies().forEach(dep ->
                logger.info("    - Dependency: {}:{}:{} (Scope: {})",
                    dep.getCoordinates().getGroupId(),
                    dep.getCoordinates().getArtifactId(),
                    dep.getCoordinates().getVersion(),
                    dep.getScope())
            );

            logger.info("  Managed dependencies found: {}", mavenProject.getDependencyManagement().size());

            // PHASE 2: Resolve dependencies using Aether for both compile and test scopes
            MavenDependencyResolver dependencyResolver = new MavenDependencyResolver();
            Path localRepoPath = extractionEnvironment.getOutputDirectory().toPath().resolve(LOCAL_REPO_DIR_NAME);

            // TODO: expose a configuration flag `includeTestScope` later; for now we enable two-phase collection (compile + test)
            boolean includeTestScope = true; // TODO: make configurable

            // Perform compile-phase dependency collection
            CollectResult collectResultCompile = dependencyResolver.resolveDependencies(
                pomFile, mavenProject, localRepoPath.toFile(), MAVEN_SCOPE_COMPILE);

            // Perform test-phase collection (only if enabled)
            CollectResult collectResultTest = null;
            if (includeTestScope) {
                collectResultTest = dependencyResolver.resolveDependencies(
                    pomFile, mavenProject, localRepoPath.toFile(), MAVEN_SCOPE_TEST);
            }

            // PHASE 3: Write dependency trees to files for human inspection and debugging
            File dependencyTreeCompileFile = new File(extractionEnvironment.getOutputDirectory(),
                DEPENDENCY_TREE_FILE_PREFIX + COMPILE_SCOPE_SUFFIX + TREE_FILE_EXTENSION);
            treeWriter.writeDependencyTree(collectResultCompile, dependencyTreeCompileFile, MAVEN_SCOPE_COMPILE);

            if (includeTestScope && collectResultTest != null) {
                File dependencyTreeTestFile = new File(extractionEnvironment.getOutputDirectory(),
                    DEPENDENCY_TREE_FILE_PREFIX + TEST_SCOPE_SUFFIX + TREE_FILE_EXTENSION);
                treeWriter.writeDependencyTree(collectResultTest, dependencyTreeTestFile, MAVEN_SCOPE_TEST);
            }

            // PHASE 4: Transform Aether dependency graphs to Black Duck's internal DependencyGraph format
            MavenGraphParser mavenGraphParser = new MavenGraphParser();

            MavenParseResult parseResultCompile = mavenGraphParser.parse(collectResultCompile);
            MavenGraphTransformer mavenGraphTransformer = new MavenGraphTransformer(externalIdFactory);
            DependencyGraph dependencyGraphCompile = mavenGraphTransformer.transform(parseResultCompile);

            MavenParseResult parseResultTest = null;
            DependencyGraph dependencyGraphTest = null;
            if (includeTestScope && collectResultTest != null) {
                parseResultTest = mavenGraphParser.parse(collectResultTest);
                dependencyGraphTest = mavenGraphTransformer.transform(parseResultTest);
            }

            // Create root code locations for compile scope (and test scope if enabled)
            CodeLocation compileCodeLocation = codeLocationFactory.createCodeLocation(
                dependencyGraphCompile,
                mavenProject.getCoordinates(),
                MAVEN_SCOPE_COMPILE
            );
            codeLocations.add(compileCodeLocation);

            if (includeTestScope && dependencyGraphTest != null) {
                CodeLocation testCodeLocation = codeLocationFactory.createCodeLocation(
                    dependencyGraphTest,
                    mavenProject.getCoordinates(),
                    MAVEN_SCOPE_TEST
                );
                codeLocations.add(testCodeLocation);
            }

            // PHASE 4.5: Download artifact JARs if enabled
            if (mavenResolverOptions.isDownloadArtifactJarsEnabled()) {
                logger.info("║       ARTIFACT JAR DOWNLOAD FEATURE: ENABLED               ║");
                logger.info("Starting JAR download phase for Maven dependencies...");

                // Collect all Aether dependencies from both compile and test scopes
                List<org.eclipse.aether.graph.Dependency> aetherDependencies = new ArrayList<org.eclipse.aether.graph.Dependency>();

                if (collectResultCompile != null && collectResultCompile.getRoot() != null) {
                    extractAetherDependenciesFromNode(collectResultCompile.getRoot(), aetherDependencies);
                    logger.debug("Extracted {} dependencies from compile scope", aetherDependencies.size());
                }

                int compileCount = aetherDependencies.size();
                if (includeTestScope && collectResultTest != null && collectResultTest.getRoot() != null) {
                    extractAetherDependenciesFromNode(collectResultTest.getRoot(), aetherDependencies);
                    logger.debug("Extracted {} additional dependencies from test scope", aetherDependencies.size() - compileCount);
                }

                logger.info("Total unique dependencies collected for JAR download: {}", aetherDependencies.size());

                // Set up repository paths for JAR checking and downloading
                Path detectRunDirectory = extractionEnvironment.getOutputDirectory().toPath();
                Path defaultDownloadJarRepository = detectRunDirectory.resolve(DOWNLOADS_DIR_NAME);
                Files.createDirectories(defaultDownloadJarRepository);

                Path customRepositoryPath = mavenResolverOptions.getJarRepositoryPath();

                if (customRepositoryPath != null) {
                    logger.info("Custom .m2 repository location configured: {}", customRepositoryPath);
                    logger.info("  (Path will be resolved to .m2/repository root automatically)");
                    logger.info("Lookup order: 1) Custom .m2 -> 2) Home ~/.m2/repository -> 3) Download cache -> 4) POM repos -> 5) Maven Central");
                } else {
                    logger.info("No custom .m2 repository configured.");
                    logger.info("Lookup order: 1) Home ~/.m2/repository -> 2) Download cache -> 3) POM repos -> 4) Maven Central");
                }

                // Extract POM-declared repositories for Tier-2 resolution
                List<com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaRepository> pomRepositories =
                    mavenProject.getRepositories();

                if (pomRepositories != null && !pomRepositories.isEmpty()) {
                    logger.info("Found {} POM-declared repositories for Tier-2 resolution:", pomRepositories.size());
                    for (int idx = 0; idx < pomRepositories.size(); idx++) {
                        com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaRepository repo = pomRepositories.get(idx);
                        logger.info("  {}. {} - {}", idx + 1, repo.getId(), repo.getUrl());
                    }
                } else {
                    logger.info("No POM-declared repositories found - will use local + Maven Central resolution");
                }

                // Create artifact downloader — it internally resolves the custom path
                ArtifactDownloader artifactDownloader = new ArtifactDownloader(
                    customRepositoryPath,
                    defaultDownloadJarRepository,
                    pomRepositories != null ? pomRepositories : new ArrayList<>(),
                    mavenResolverOptions
                );

                // The path of downloaded or located JARs for all dependencies, keyed by their artifact coordinates
                Map<Artifact, Path> locatedOrDownloadedArtifactJars = Collections.emptyMap();

                logger.info("Starting artifact downloads...");
                locatedOrDownloadedArtifactJars = artifactDownloader.downloadArtifacts(aetherDependencies);

                // PHASE 4.6: Scan JARs for shaded dependencies
                logger.info("JAR download phase completed, continuing with shaded JAR inspection...");

                ShadedDependencyScanner shadedDependencyScanner = new ShadedDependencyScanner();

                // Scan all JARs and get discovered shaded dependencies
                Map<Artifact, List<DiscoveredDependency>> shadedDependenciesMap =
                        shadedDependencyScanner.scanJarsForShadedDependencies(locatedOrDownloadedArtifactJars, collectResultCompile,collectResultTest);

                logger.info("Shaded dependency inspection phase completed. Found {} JARs with shaded dependencies.",
                        shadedDependenciesMap.size());

                // PHASE 4.7: Add shaded dependencies to dependency graphs
                if (!shadedDependenciesMap.isEmpty()) {
                    logger.info("Adding shaded dependencies to dependency graphs...");

                    int addedToCompile = addShadedDependenciesToGraph(dependencyGraphCompile, shadedDependenciesMap);
                    logger.info("Added {} shaded dependencies to compile dependency graph.", addedToCompile);

                    if (includeTestScope && dependencyGraphTest != null) {
                        int addedToTest = addShadedDependenciesToGraph(dependencyGraphTest, shadedDependenciesMap);
                        logger.info("Added {} shaded dependencies to test dependency graph.", addedToTest);
                    }

                    logger.info("Successfully integrated shaded dependencies into dependency graphs.");
                } else {
                    logger.info("No shaded dependencies found to add to dependency graphs.");
                }

                logger.info("Shaded JAR inspection phase completed, continuing with code location generation...");
            } else {
                logger.info("ARTIFACT JAR DOWNLOAD FEATURE: DISABLED");

                Path customRepositoryPath = mavenResolverOptions.getJarRepositoryPath();
                if (customRepositoryPath != null) {
                    logger.warn("Configuration Issue Detected:");
                    logger.warn("  detect.maven.jar.repository.path is set to: {}", customRepositoryPath);
                    logger.warn("  BUT detect.maven.download.artifact.jars is DISABLED (false)");
                    logger.warn("  The custom .m2 repository path will be IGNORED.");
                    logger.warn("  To use the custom repository, enable JAR downloads:");
                    logger.warn("  --detect.maven.download.artifact.jars=true");
                } else {
                    logger.debug("Skipping JAR downloads (feature not enabled via detect.maven.download.artifact.jars property)");
                }
            }

            // PHASE 5: Process multi-module Maven projects recursively
            // Each module gets its own code locations for compile and test scopes
            if (mavenProject.getModules() != null && !mavenProject.getModules().isEmpty()) {
                File rootDir = pomFile.getParentFile();

                // Build processing context with all dependencies
                MavenModuleProcessingContext context = new MavenModuleProcessingContext.Builder()
                    .projectBuilder(projectBuilder)
                    .dependencyResolver(dependencyResolver)
                    .graphParser(mavenGraphParser)
                    .graphTransformer(mavenGraphTransformer)
                    .treeWriter(treeWriter)
                    .codeLocationFactory(codeLocationFactory)
                    .coordinateFormatter(coordinateFormatter)
                    .localRepoPath(localRepoPath)
                    .outputDir(extractionEnvironment.getOutputDirectory())
                    .includeTestScope(includeTestScope)
                    .codeLocations(codeLocations)
                    .compileScope(MAVEN_SCOPE_COMPILE)
                    .testScope(MAVEN_SCOPE_TEST)
                    .build();

                // Process all modules using the module processor
                moduleProcessor.processModules(mavenProject.getModules(), rootDir, context);
            }

            // PHASE 6: Return all collected code locations (root + all modules)
            String projectName = mavenProject.getCoordinates().getGroupId() + ":" + mavenProject.getCoordinates().getArtifactId();
            NameVersion nameVersion = new NameVersion(projectName, mavenProject.getCoordinates().getVersion());

            return new Extraction.Builder().success(codeLocations).nameVersion(nameVersion).build();

        } catch (Exception e) {
            logger.error("Failed to resolve dependencies for pom.xml: {}", e.getMessage());
            return new Extraction.Builder().exception(e).build();
        }
    }

    /**
     * Adds discovered shaded dependencies to the dependency graph as children of the root.
     *
     * <p>Converts DiscoveredDependency objects (containing GAV strings) into DependencyGraph nodes
     * and adds them as direct children of the root project node. This ensures shaded dependencies
     * appear in the final BDIO output for Black Duck analysis.
     *
     * @param graph The dependency graph to add shaded dependencies to
     * @param shadedDepsMap Map of parent artifact to list of shaded dependencies found in it
     * @return The number of shaded dependencies successfully added to the graph
     */
    private int addShadedDependenciesToGraph(
            DependencyGraph graph,
            Map<Artifact, List<DiscoveredDependency>> shadedDepsMap) {

        int addedCount = 0;

        for (Map.Entry<Artifact, List<DiscoveredDependency>> entry : shadedDepsMap.entrySet()) {
            Artifact parentArtifact = entry.getKey();
            List<DiscoveredDependency> shadedDeps = entry.getValue();

            String parentGav = parentArtifact.getGroupId() + ":" + parentArtifact.getArtifactId() + ":" + parentArtifact.getVersion();
            logger.debug("Processing {} shaded dependencies from parent: {}", shadedDeps.size(), parentGav);

            for (DiscoveredDependency shadedDep : shadedDeps) {
                try {
                    String identifier = shadedDep.getIdentifier();

                    if (identifier == null || identifier.isEmpty()) {
                        logger.warn("Skipping shaded dependency with null/empty identifier from parent: {}", parentGav);
                        continue;
                    }

                    // Parse GAV from identifier (format: "groupId:artifactId:version")
                    String[] gavParts = identifier.split(":");

                    if (gavParts.length < 3) {
                        logger.warn("Skipping shaded dependency with invalid GAV format: {} (expected groupId:artifactId:version)", identifier);
                        continue;
                    }

                    String groupId = gavParts[0];
                    String artifactId = gavParts[1];
                    String version = gavParts[2];

                    // Skip if any part is empty or marked as UNKNOWN
                    if (groupId.isEmpty() || artifactId.isEmpty() || version.isEmpty() || "UNKNOWN".equals(version)) {
                        logger.warn("Skipping shaded dependency with incomplete GAV: {}", identifier);
                        continue;
                    }

                    // Create ExternalId for the shaded dependency
                    ExternalId externalId = externalIdFactory.createMavenExternalId(groupId, artifactId, version);

                    // Create Dependency object (using BDIO Dependency)
                    Dependency dependency = new Dependency(artifactId, version, externalId);

                    // Add as child of root
                    graph.addChildToRoot(dependency);
                    addedCount++;

                    logger.debug("  Added shaded dependency to graph: {} (detected via: {})",
                        identifier, shadedDep.getDetectionSource());

                } catch (Exception e) {
                    logger.warn("Failed to add shaded dependency to graph: {} - Error: {}",
                        shadedDep.getIdentifier(), e.getMessage());
                }
            }
        }

        return addedCount;
    }

    /**
     * Recursively extracts all Aether dependencies from a dependency node tree.
     * This traverses the Aether dependency graph and collects all unique dependencies.
     *
     * @param node The root dependency node to traverse
     * @param aetherDependencies The list to collect Aether dependencies into
     */
    private void extractAetherDependenciesFromNode(DependencyNode node,
                                                    List<org.eclipse.aether.graph.Dependency> aetherDependencies) {
        if (node == null) {
            return;
        }

        // Add current node's dependency if it exists
        org.eclipse.aether.graph.Dependency dependency = node.getDependency();
        if (dependency != null && dependency.getArtifact() != null) {
            // Avoid duplicates by checking if already added
            boolean alreadyExists = false;
            for (org.eclipse.aether.graph.Dependency existing : aetherDependencies) {
                if (isSameArtifact(existing.getArtifact(), dependency.getArtifact())) {
                    alreadyExists = true;
                    break;
                }
            }

            if (!alreadyExists) {
                aetherDependencies.add(dependency);
            }
        }

        // Recursively process children
        if (node.getChildren() != null) {
            for (DependencyNode child : node.getChildren()) {
                extractAetherDependenciesFromNode(child, aetherDependencies);
            }
        }
    }

    /**
     * Checks if two Aether artifacts represent the same artifact (same coordinates).
     *
     * @param a1 First artifact
     * @param a2 Second artifact
     * @return true if they have the same groupId, artifactId, and version
     */
    private boolean isSameArtifact(Artifact a1, Artifact a2) {
        return a1.getGroupId().equals(a2.getGroupId()) &&
               a1.getArtifactId().equals(a2.getArtifactId()) &&
               a1.getVersion().equals(a2.getVersion());
    }
}
