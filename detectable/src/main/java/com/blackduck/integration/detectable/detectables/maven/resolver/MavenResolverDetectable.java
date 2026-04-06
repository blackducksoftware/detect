package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaCoordinates;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaRepository;
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
import java.util.*;

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

            // PHASE 4.5: Download artifact JARs and check for shaded dependencies if enabled
            if (mavenResolverOptions.isIncludeShadedDependenciesV2Enabled()) {
                logger.info("DETECT SHADED DEPENDENCIES FEATURE: ENABLED");
                logger.info("Starting JAR download phase for Maven dependencies...");

                // Collect all Aether dependencies from both compile and test scopes
                List<org.eclipse.aether.graph.Dependency> aetherDependencies = new ArrayList<org.eclipse.aether.graph.Dependency>();
                Set<String> seenGavs = new HashSet<>();

                if (collectResultCompile != null && collectResultCompile.getRoot() != null) {
                    extractAetherDependenciesFromNode(collectResultCompile.getRoot(), aetherDependencies, seenGavs);
                    logger.debug("Extracted {} dependencies from compile scope", aetherDependencies.size());
                }

                int compileCount = aetherDependencies.size();
                if (includeTestScope && collectResultTest != null && collectResultTest.getRoot() != null) {
                    extractAetherDependenciesFromNode(collectResultTest.getRoot(), aetherDependencies, seenGavs);
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
                        shadedDependencyScanner.scanJarsForShadedDependencies(
                                locatedOrDownloadedArtifactJars,
                                collectResultCompile,
                                collectResultTest,
                                projectBuilder);

                logger.info("Shaded dependency inspection phase completed. Found {} JARs with shaded dependencies.",
                        shadedDependenciesMap.size());

                // PHASE 4.7: Add shaded dependencies to dependency graphs by resolving their sub-trees and attaching them under the correct parent artifact
                // ========================================================================================
                // This phase performs the following steps for each shaded dependency discovered in Phase 4.6:
                //   1. Download the POM file for the shaded dependency from Maven repositories
                //   2. Build a MavenProject model (resolving parent POMs, BOMs, property inheritance)
                //   3. Resolve the full Aether dependency tree for the shaded dependency
                //   4. Convert the Aether tree to an isolated BDIO DependencyGraph
                //   5. Graft (attach) the isolated graph onto the main graph under the parent JAR
                //
                // The result is that vulnerabilities in transitive dependencies of shaded JARs
                // are correctly attributed with an accurate remediation path in the BDIO output.
                // ========================================================================================
                if (!shadedDependenciesMap.isEmpty()) {
                    logger.info("Resolving sub-trees and grafting shaded dependencies into main dependency graphs...");

                    // We pass the root POM's repositories so the downloader knows about any custom repos defined in the root
                    List<JavaRepository> rootRepositories = mavenProject.getRepositories() != null ? mavenProject.getRepositories() : new ArrayList<>();

                    // Process compile scope dependency graph
                    int addedToCompile = addShadedDependenciesToGraph(
                            dependencyGraphCompile,
                            shadedDependenciesMap,
                            projectBuilder,
                            dependencyResolver,
                            mavenGraphParser,
                            mavenGraphTransformer,
                            downloadDir.toFile(),
                            localRepoPath.toFile(),
                            rootRepositories
                    );
                    logger.info("Grafted {} sub-graph nodes into compile dependency graph.", addedToCompile);

                    // Process test scope dependency graph if enabled
                    if (includeTestScope && dependencyGraphTest != null) {
                        int addedToTest = addShadedDependenciesToGraph(
                                dependencyGraphTest,
                                shadedDependenciesMap,
                                projectBuilder,
                                dependencyResolver,
                                mavenGraphParser,
                                mavenGraphTransformer,
                                downloadDir.toFile(),
                                localRepoPath.toFile(),
                                rootRepositories
                        );
                        logger.info("Grafted {} sub-graph nodes into test dependency graph.", addedToTest);
                    }

                    logger.info("Successfully integrated shaded dependency sub-trees.");

                    // Optional: Print the updated graphs for debugging
                    logger.info("================= Updated Compile Dependency Graph with Shaded Dependencies =================");
                    debugPrintBdioGraph(dependencyGraphCompile, dependencyGraphCompile.getRootDependencies(),0);
                    logger.info("==============================================================================================");
                } else {
                    logger.info("No shaded dependencies found to add to dependency graphs.");
                }

                logger.info("Shaded JAR inspection phase completed, continuing with code location generation...");
            } else {
                logger.info("DETECT SHADED DEPENDENCIES FEATURE: DISABLED");

                Path customRepositoryPath = mavenResolverOptions.getJarRepositoryPath();
                if (customRepositoryPath != null) {
                    logger.warn("Configuration Issue Detected:");
                    logger.warn("  detect.maven.jar.repository.path is set to: {}", customRepositoryPath);
                    logger.warn("  BUT detect.maven.include.shaded.dependenciesv2 is DISABLED (false)");
                    logger.warn("  The custom .m2 repository path will be IGNORED.");
                    logger.warn("  To use the custom repository, enable the flag to include shaded dependencies for JAR downloads:");
                    logger.warn("  --detect.maven.include.shaded.dependenciesv2=true");
                } else {
                    logger.debug("Skipping JAR downloads (feature not enabled via detect.maven.include.shaded.dependenciesv2 property)");
                }
            }

            // PHASE 5: Process multi-module Maven projects recursively
            // Each module gets its own code locations for compile and test scopes
            if (mavenProject.getModules() != null && !mavenProject.getModules().isEmpty()) {
                File rootDir = pomFile.getParentFile();

                // Build processing context with all dependencies including shaded dependency support
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
                    .mavenResolverOptions(mavenResolverOptions)
                    .externalIdFactory(externalIdFactory)
                    .downloadDir(downloadDir)
                    .rootRepositories(mavenProject.getRepositories())
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
     * Adds discovered shaded dependencies to the dependency graph under their respective parent artifacts.
     *
     * <p>Converts DiscoveredDependency objects (containing GAV strings) into DependencyGraph nodes
     * and adds them as direct children of the JAR that actually shaded them. This ensures the
     * vulnerability remediation path is accurate in the final BDIO output.
     *
     * @param graph The dependency graph to add shaded dependencies to
     * @param shadedDepsMap Map of parent artifact to list of shaded dependencies found in it
     * @return The number of shaded dependencies successfully added to the graph
     */
    /**

     * Resolves the full Aether dependency tree for each discovered shaded dependency

     * and grafts the resulting sub-graph onto the parent JAR in the main BDIO graph.

     */

    private int addShadedDependenciesToGraph(
            DependencyGraph mainGraph,
            Map<Artifact, List<DiscoveredDependency>> shadedDepsMap,
            ProjectBuilder projectBuilder,
            MavenDependencyResolver dependencyResolver,
            MavenGraphParser graphParser,
            MavenGraphTransformer graphTransformer,
            File downloadDir,
            File localRepoPath,
            List<JavaRepository> rootRepositories) {

        int graftedNodeCount = 0;

        // Iterate through each parent artifact that contains shaded dependencies
        for (Map.Entry<Artifact, List<DiscoveredDependency>> entry : shadedDepsMap.entrySet()) {
            Artifact parentArtifact = entry.getKey();
            List<DiscoveredDependency> shadedDeps = entry.getValue();

            logger.debug("Processing {} shaded dependencies for parent artifact: {}:{}:{}",
                    shadedDeps.size(),
                    parentArtifact.getGroupId(),
                    parentArtifact.getArtifactId(),
                    parentArtifact.getVersion());

            // Look up the EXISTING parent node in the main graph by ExternalId
            // This is critical: we must use the actual node from the graph, not a new Dependency object,
            // otherwise addChildWithParent won't find the parent and shaded deps will be added at root level
            ExternalId parentExternalId = externalIdFactory.createMavenExternalId(
                    parentArtifact.getGroupId(), parentArtifact.getArtifactId(), parentArtifact.getVersion());
            Dependency parentDependency = mainGraph.getDependency(parentExternalId);

            // If the parent artifact is not found in the graph, skip processing its shaded dependencies
            // This can happen if the parent artifact was excluded or filtered out during dependency resolution
            if (parentDependency == null) {
                logger.warn("Parent artifact {}:{}:{} not found in dependency graph. Skipping shaded dependencies for this artifact.",
                        parentArtifact.getGroupId(), parentArtifact.getArtifactId(), parentArtifact.getVersion());
                continue;
            }

            // Process each shaded dependency found inside this parent JAR
            for (DiscoveredDependency shadedDep : shadedDeps) {
                try {
                    // Validate the identifier is present
                    String identifier = shadedDep.getIdentifier();
                    if (identifier == null || identifier.isEmpty()) {
                        logger.warn("Skipping shaded dependency with null/empty identifier in parent: {}",
                                parentArtifact.getArtifactId());
                        continue;
                    }

                    // Parse GAV coordinates from identifier (format: groupId:artifactId:version)
                    String[] gavParts = identifier.split(":");
                    if (gavParts.length < 3) {
                        logger.warn("Skipping shaded dependency with invalid GAV format: {}", identifier);
                        continue;
                    }

                    String groupId = gavParts[0];
                    String artifactId = gavParts[1];
                    String version = gavParts[2];

                    // Skip dependencies with incomplete or unknown coordinates
                    if (groupId.isEmpty() || artifactId.isEmpty() || version.isEmpty() || "UNKNOWN".equals(version)) {
                        logger.debug("Skipping shaded dependency with incomplete coordinates: {}", identifier);
                        continue;
                    }

                    logger.debug("Resolving sub-tree for shaded dependency: {}", identifier);

                    // ==========================================
                    // STEP 1: Download the POM for the shaded dependency
                    // ==========================================
                    // NOTE (Edge Case): If the POM is not in Maven Central, the download will fail.
                    // This frequently occurs in air-gapped environments or when artifacts are hosted on
                    // custom Nexus/Artifactory mirrors that are NOT declared in the root POM's <repositories> block.
                    // Future enhancement: Add a fallback to attach the shaded dependency as a flat, single node
                    // if the POM download fails, ensuring we don't lose the vulnerability finding entirely.
                    JavaCoordinates coords = new JavaCoordinates(groupId, artifactId, version, "pom");
                    MavenDownloader downloader = new MavenDownloader(rootRepositories, downloadDir.toPath());
                    File shadedPomFile = downloader.downloadPom(coords);

                    // Handle POM download failure with fallback behavior
                    if (shadedPomFile == null || !shadedPomFile.exists()) {
                        logger.warn("Could not download POM for shaded dependency {}. Sub-tree resolution aborted.", identifier);
                        // Fallback: Attach just the single node to prevent data loss
                        // This ensures the vulnerability is still reported, even without transitive dependencies
                        ExternalId fallbackId = externalIdFactory.createMavenExternalId(groupId, artifactId, version);
                        mainGraph.addChildWithParent(new Dependency(artifactId, version, fallbackId), parentDependency);
                        graftedNodeCount++;
                        logger.info("Added fallback single node for shaded dependency: {} (parent: {})",
                                identifier, parentArtifact.getArtifactId());
                        continue;
                    }

                    logger.debug("Successfully downloaded POM for shaded dependency: {}", identifier);

                    // ==========================================
                    // STEP 2: Build the MavenProject model
                    // ==========================================
                    // This resolves properties, BOMs, and Parent POMs to get the complete dependency list
                    com.blackduck.integration.detectable.detectables.maven.resolver.MavenProject shadedProject =
                            projectBuilder.buildProject(shadedPomFile);
                    logger.debug("Built MavenProject for shaded dependency: {} with {} direct dependencies",
                            identifier, shadedProject.getDependencies().size());

                    // ==========================================
                    // STEP 3: Resolve the Aether dependency tree
                    // ==========================================
                    // This performs full transitive resolution using Eclipse Aether
                    CollectResult collectResult = dependencyResolver.resolveDependencies(
                            shadedPomFile, shadedProject, localRepoPath, MAVEN_SCOPE_COMPILE);
                    logger.debug("Resolved Aether tree for shaded dependency: {}", identifier);

                    //TODO : FIND SHADED DEPENDENCIES OF THESE SHADED DEPENDENCIES
                    //Aether only resolves unshaded dependencies, leaving shaded deps as gaps
                    //in the tree which may result in false negatives for vulnerabilities.

                    // ==========================================
                    // STEP 4: Convert to isolated BDIO DependencyGraph
                    // ==========================================
                    MavenParseResult parseResult = graphParser.parse(collectResult);
                    DependencyGraph isolatedShadedGraph = graphTransformer.transform(parseResult);
                    logger.debug("Transformed shaded dependency {} to BDIO graph with {} root dependencies",
                            identifier, isolatedShadedGraph.getRootDependencies().size());

                    // ==========================================
                    // STEP 5: Graft the shaded dependency and its sub-tree onto the main graph
                    // ==========================================
                    // IMPORTANT: We must first add the shaded dependency ITSELF (ss) as a child of the parent (X),
                    // then graft ss's transitive dependencies under ss.
                    //
                    // Correct structure:   X -> ss -> ss-transitive-1
                    // Wrong structure:     X -> ss-transitive-1 (missing ss node!)
                    //
                    // The isolatedShadedGraph.getRootDependencies() returns ss's dependencies, NOT ss itself.
                    // So we need to explicitly create and add the ss node first.

                    // Create the shaded dependency node (ss) and add it under the parent (X)
                    ExternalId shadedDepExternalId = externalIdFactory.createMavenExternalId(groupId, artifactId, version);
                    Dependency shadedDependencyNode = new Dependency(artifactId, version, shadedDepExternalId);
                    mainGraph.addChildWithParent(shadedDependencyNode, parentDependency);
                    graftedNodeCount++;
                    logger.debug("Added shaded dependency node {} under parent {}", identifier, parentArtifact.getArtifactId());

                    // Now graft ss's transitive dependencies under ss (not under X)
                    // The isolatedShadedGraph's root dependencies are ss's direct dependencies
                    //
                    // TODO: [STEP 4 - Graph Intersection]
                    // Perform a logical-to-physical intersection filter before grafting.
                    // The current sub-graph represents an "ideal" Aether tree, but the actual fat JAR
                    // may have physically excluded certain transitives via the Maven Shade Plugin's
                    // '<excludes>' configuration. To prevent false positives, we must cross-reference
                    // the nodes in 'isolatedShadedGraph' against the physical 'shadedDepsMap' and
                    // only graft nodes that are physically present in the binary.
                    int nodesAdded = graftSubGraph(isolatedShadedGraph, mainGraph, shadedDependencyNode);
                    graftedNodeCount += nodesAdded;
                    logger.debug("Grafted {} transitive nodes from shaded dependency {} under {}",
                            nodesAdded, identifier, identifier);

                } catch (Exception e) {
                    logger.error("Failed to resolve and graft sub-tree for shaded dependency: {} - Error: {}",
                            shadedDep.getIdentifier(), e.getMessage());
                    logger.debug("Stack trace for shaded dependency resolution failure:", e);
                }
            }
        }

        logger.debug("Total grafted node count for this graph: {}", graftedNodeCount);
        return graftedNodeCount;
    }

    /**
     * Grafts an isolated sub-graph onto a destination graph under a specified parent node.
     *
     * <p>This method performs the following operations:
     * <ol>
     *   <li>Takes the root dependencies of the source (isolated) graph</li>
     *   <li>Attaches each root as a child of the specified parent in the destination graph</li>
     *   <li>Recursively copies all child relationships from the source to the destination</li>
     * </ol>
     *
     * <p><strong>Example:</strong>
     * <pre>
     * Source Graph (isolated shaded dependency):
     *   root-dep-A
     *     └── child-dep-B
     *           └── child-dep-C
     *
     * Destination Graph (main BDIO graph):
     *   parent-jar (the JAR containing shaded deps)
     *
     * Result after grafting:
     *   parent-jar
     *     └── root-dep-A
     *           └── child-dep-B
     *                 └── child-dep-C
     * </pre>
     *
     * @param sourceGraph The isolated sub-graph to graft (source)
     * @param destGraph The main dependency graph to graft onto (destination)
     * @param destParent The parent node in the destination graph under which to attach source roots
     * @return The total number of nodes added to the destination graph
     */
    private int graftSubGraph(DependencyGraph sourceGraph, DependencyGraph destGraph, Dependency destParent) {
        int count = 0;

        // Step A: Get the root dependencies of the isolated graph
        // These are the top-level dependencies that will be attached to destParent
        Set<Dependency> sourceRoots = sourceGraph.getRootDependencies();
        if (sourceRoots == null || sourceRoots.isEmpty()) {
            logger.debug("Source graph has no root dependencies to graft");
            return count;
        }

        logger.debug("Grafting {} root dependencies under parent: {}", sourceRoots.size(), destParent.getName());

        // Initialize visited set for cycle detection during graph traversal
        Set<ExternalId> visitedNodes = new HashSet<>();

        // Step B: Attach each source root as a child of the destination parent
        for (Dependency sourceRoot : sourceRoots) {
            destGraph.addChildWithParent(sourceRoot, destParent);
            count++;

            // Step C: Recursively copy all child relationships from source to destination
            // Pass visitedNodes to detect and handle cycles gracefully
            count += copyGraphEdges(sourceGraph, destGraph, sourceRoot, visitedNodes);
        }

        return count;
    }

    /**
     * Recursively copies parent-child relationships from a source graph to a destination graph.
     *
     * <p>This method traverses the source graph starting from a given parent node and
     * replicates all child relationships in the destination graph. The traversal is
     * depth-first, ensuring that the entire sub-tree is copied.
     *
     * <p><strong>Cycle Detection:</strong> This method tracks visited nodes to detect cycles.
     * If a cycle is detected (a node that has already been visited in the current traversal path),
     * the method logs a warning and skips that branch to prevent infinite recursion.
     * While Maven's dependency resolution naturally prevents cycles, malformed POMs or
     * edge cases in shaded JARs could theoretically introduce them.
     *
     * @param sourceGraph The source graph to copy relationships from
     * @param destGraph The destination graph to copy relationships to
     * @param currentParent The current parent node being processed
     * @param visitedNodes Set of already visited node ExternalIds for cycle detection
     * @return The number of child nodes copied
     */
    private int copyGraphEdges(DependencyGraph sourceGraph, DependencyGraph destGraph, Dependency currentParent, Set<ExternalId> visitedNodes) {
        int count = 0;

        // Cycle detection: Check if we've already visited this node in the current traversal
        ExternalId currentId = currentParent.getExternalId();
        if (currentId != null && visitedNodes.contains(currentId)) {
            // Cycle detected - log warning and skip this branch to prevent infinite recursion
            logger.warn("Cycle detected in dependency graph at node: {}:{}:{}. Skipping to prevent infinite recursion.",
                    currentId.getGroup(), currentId.getName(), currentId.getVersion());
            return count;
        }

        // Mark current node as visited for cycle detection
        if (currentId != null) {
            visitedNodes.add(currentId);
        }

        try {
            // Get all children of the current parent in the source graph
            Set<Dependency> children = sourceGraph.getChildrenForParent(currentParent);

            // Base case: no children to process
            if (children == null || children.isEmpty()) {
                return count;
            }

            // Copy each child relationship and recurse
            for (Dependency child : children) {
                // Check for cycle before processing child
                ExternalId childId = child.getExternalId();
                if (childId != null && visitedNodes.contains(childId)) {
                    logger.warn("Cycle detected: {} already visited in current path. Skipping child to prevent infinite loop.",
                            childId.getGroup() + ":" + childId.getName() + ":" + childId.getVersion());
                    continue;
                }

                // Add the parent-child relationship to the destination graph
                destGraph.addChildWithParent(child, currentParent);
                count++;
                logger.trace("Copied edge: {} -> {}", currentParent.getName(), child.getName());

                // Recurse down the tree to copy grandchildren, great-grandchildren, etc.
                count += copyGraphEdges(sourceGraph, destGraph, child, visitedNodes);
            }
        } finally {
            // Remove current node from visited set when backtracking
            // This allows the same node to appear in different branches (diamond dependency pattern)
            if (currentId != null) {
                visitedNodes.remove(currentId);
            }
        }

        return count;

    }

    /**
     * Recursively extracts all Aether dependencies from a dependency node tree.
     * This traverses the Aether dependency graph and collects all unique dependencies.
     * Uses a Set for O(1) duplicate detection instead of linear search.
     *
     * @param node The root dependency node to traverse
     * @param aetherDependencies The list to collect Aether dependencies into
     * @param seenGavs Set of already-seen GAV keys for O(1) deduplication
     */
    private void extractAetherDependenciesFromNode(DependencyNode node,
                                                    List<org.eclipse.aether.graph.Dependency> aetherDependencies,
                                                    Set<String> seenGavs) {
        if (node == null) {
            return;
        }

        // Add current node's dependency if it exists
        org.eclipse.aether.graph.Dependency dependency = node.getDependency();
        if (dependency != null && dependency.getArtifact() != null) {
            // O(1) duplicate check using Set
            String gavKey = dependency.getArtifact().getGroupId() + ":"
                          + dependency.getArtifact().getArtifactId() + ":"
                          + dependency.getArtifact().getVersion();
            if (seenGavs.add(gavKey)) {  // add() returns false if already present
                aetherDependencies.add(dependency);
            }
        }

        // Recursively process children
        if (node.getChildren() != null) {
            for (DependencyNode child : node.getChildren()) {
                extractAetherDependenciesFromNode(child, aetherDependencies, seenGavs);
            }
        }
    }

    private void debugPrintBdioGraph(DependencyGraph graph, Set<Dependency> dependencies, int depth) {
        debugPrintBdioGraph(graph, dependencies, depth, new HashSet<>());
    }

    private void debugPrintBdioGraph(DependencyGraph graph, Set<Dependency> dependencies, int depth, Set<ExternalId> visited) {
        if (dependencies == null || dependencies.isEmpty()) { return; }

        String indent = String.join("", Collections.nCopies(depth * 2, " "));
        for (Dependency dep : dependencies) {
            ExternalId id = dep.getExternalId();

            // Guard: null ExternalId cannot be tracked — log and skip to avoid NPE and untracked cycles
            if (id == null) {
                logger.warn("{}|-- [skipped: dependency '{}' has null ExternalId]", indent, dep.getName());
                continue;
            }

            // Cycle guard: check before logging so a cyclic node is never printed as if it has children
            if (visited.contains(id)) {
                logger.info("{}|-- {}:{}:{} (already visited — cycle/diamond detected, skipping children)",
                        indent, id.getGroup(), dep.getName(), dep.getVersion());
                continue;
            }

            String gav = id.getGroup() + ":" + dep.getName() + ":" + dep.getVersion();
            logger.info("{}|-- {}", indent, gav);

            visited.add(id);
            Set<Dependency> children = graph.getChildrenForParent(dep);
            debugPrintBdioGraph(graph, children, depth + 1, visited);
            visited.remove(id);
        }
    }
}
