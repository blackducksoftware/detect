package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.bdio.graph.DependencyGraph;
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
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.detectable.detectables.maven.resolver.result.MavenParseResult;
import com.blackduck.integration.util.NameVersion;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
                logger.info("╔════════════════════════════════════════════════════════════╗");
                logger.info("║       ARTIFACT JAR DOWNLOAD FEATURE: ENABLED               ║");
                logger.info("╚════════════════════════════════════════════════════════════╝");
                logger.info("Starting JAR download phase for Maven dependencies...");

                // Collect all dependencies from both compile and test scopes
                List<Dependency> allDependencies = new ArrayList<>();

                // Extract dependencies from the collect results
                if (collectResultCompile != null && collectResultCompile.getRoot() != null) {
                    extractDependenciesFromNode(collectResultCompile.getRoot(), allDependencies);
                    logger.debug("Extracted {} dependencies from compile scope", allDependencies.size());
                }

                int compileCount = allDependencies.size();
                if (includeTestScope && collectResultTest != null && collectResultTest.getRoot() != null) {
                    extractDependenciesFromNode(collectResultTest.getRoot(), allDependencies);
                    logger.debug("Extracted {} additional dependencies from test scope", allDependencies.size() - compileCount);
                }

                logger.info("Total unique dependencies collected for JAR download: {}", allDependencies.size());

                // Set up repository paths for JAR checking and downloading
                Path userHome = java.nio.file.Paths.get(System.getProperty("user.home"));
                Path defaultM2Repository = userHome.resolve(".m2").resolve("repository");

                Path customRepositoryPath = mavenResolverOptions.getJarRepositoryPath();

                if (customRepositoryPath != null) {
                    logger.info("Custom JAR repository configured: {}", customRepositoryPath);
                    logger.info("Default Maven repository (.m2): {}", defaultM2Repository);
                    logger.info("Lookup order: 1) Custom repository → 2) .m2/repository → 3) Maven Central");
                } else {
                    logger.info("Using default Maven repository: {}", defaultM2Repository);
                    logger.info("Lookup order: 1) .m2/repository → 2) Maven Central");
                }

                // Extract POM-declared repositories for Tier-2 resolution
                // These repositories come from the effective POM after inheritance and import resolution
                List<com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaRepository> pomRepositories =
                    mavenProject.getRepositories();

                if (pomRepositories != null && !pomRepositories.isEmpty()) {
                    logger.info("Found {} POM-declared repositories for Tier-2 resolution:", pomRepositories.size());
                    for (int i = 0; i < pomRepositories.size(); i++) {
                        com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaRepository repo = pomRepositories.get(i);
                        logger.info("  {}. {} - {}", i + 1, repo.getId(), repo.getUrl());
                    }
                } else {
                    logger.info("No POM-declared repositories found - will use 2-tier resolution (local + Maven Central)");
                }

                // Create enhanced artifact downloader with full configuration including POM repositories
                ArtifactDownloaderV2 artifactDownloader = new ArtifactDownloaderV2(
                    customRepositoryPath,
                    defaultM2Repository,
                    pomRepositories != null ? pomRepositories : new ArrayList<>(),  // Pass POM repositories for Tier-2
                    mavenResolverOptions
                );

                logger.info("Starting artifact downloads with 3-tier resolution strategy...");
                artifactDownloader.downloadArtifacts(allDependencies);

                logger.info("JAR download phase completed, continuing with code location generation...");
            } else {
                logger.info("╔════════════════════════════════════════════════════════════╗");
                logger.info("║       ARTIFACT JAR DOWNLOAD FEATURE: DISABLED              ║");
                logger.info("╚════════════════════════════════════════════════════════════╝");

                // Check for configuration issue: custom path provided but feature disabled
                Path customRepositoryPath = mavenResolverOptions.getJarRepositoryPath();
                if (customRepositoryPath != null) {
                    logger.warn("⚠ Configuration Issue Detected:");
                    logger.warn("  detect.maven.jar.repository.path is set to: {}", customRepositoryPath);
                    logger.warn("  BUT detect.maven.download.artifact.jars is DISABLED (false)");
                    logger.warn("  The custom JAR repository path will be IGNORED.");
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
     * Recursively extracts all dependencies from a dependency node tree.
     * This traverses the Aether dependency graph and collects all unique dependencies.
     *
     * @param node The root dependency node to traverse
     * @param dependencies The list to collect dependencies into
     */
    private void extractDependenciesFromNode(org.eclipse.aether.graph.DependencyNode node, List<Dependency> dependencies) {
        if (node == null) {
            return;
        }

        // Add current node's dependency if it exists
        Dependency dependency = node.getDependency();
        if (dependency != null && dependency.getArtifact() != null) {
            // Avoid duplicates by checking if already added
            boolean alreadyExists = dependencies.stream()
                .anyMatch(d -> isSameArtifact(d.getArtifact(), dependency.getArtifact()));

            if (!alreadyExists) {
                dependencies.add(dependency);
            }
        }

        // Recursively process children
        if (node.getChildren() != null) {
            for (org.eclipse.aether.graph.DependencyNode child : node.getChildren()) {
                extractDependenciesFromNode(child, dependencies);
            }
        }
    }

    /**
     * Checks if two artifacts represent the same artifact (same coordinates).
     *
     * @param a1 First artifact
     * @param a2 Second artifact
     * @return true if they have the same groupId, artifactId, and version
     */
    private boolean isSameArtifact(org.eclipse.aether.artifact.Artifact a1, org.eclipse.aether.artifact.Artifact a2) {
        return a1.getGroupId().equals(a2.getGroupId()) &&
               a1.getArtifactId().equals(a2.getArtifactId()) &&
               a1.getVersion().equals(a2.getVersion());
    }
}
