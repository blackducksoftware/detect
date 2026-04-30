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
import com.blackduck.integration.detectable.detectables.maven.resolver.graph.MavenGraphParser;
import com.blackduck.integration.detectable.detectables.maven.resolver.graph.MavenGraphTransformer;
import com.blackduck.integration.detectable.detectables.maven.resolver.graph.MavenParseResult;
import com.blackduck.integration.detectable.detectables.maven.resolver.module.MavenModuleProcessor;
import com.blackduck.integration.detectable.detectables.maven.resolver.module.MavenModuleProcessingContext;
import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenMirrorConfig;
import com.blackduck.integration.detectable.detectables.maven.resolver.output.CodeLocationFactory;
import com.blackduck.integration.detectable.detectables.maven.resolver.output.DependencyTreeFileWriter;
import com.blackduck.integration.detectable.detectables.maven.resolver.output.MavenCoordinateFormatter;
import com.blackduck.integration.detectable.detectables.maven.resolver.pom.MavenProject;
import com.blackduck.integration.detectable.detectables.maven.resolver.pom.ProjectBuilder;
import com.blackduck.integration.detectable.detectables.maven.resolver.resolution.MavenDependencyResolver;
import com.blackduck.integration.util.NameVersion;
import org.eclipse.aether.collection.CollectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

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
     * @param mavenResolverOptions Configuration options for Maven resolution
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
        this.mavenResolverOptions = mavenResolverOptions;

        // Initialize helper classes
        this.treeWriter = new DependencyTreeFileWriter();
        this.codeLocationFactory = new CodeLocationFactory(externalIdFactory);
        this.coordinateFormatter = new MavenCoordinateFormatter();
        this.moduleProcessor = new MavenModuleProcessor();
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
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        try {
            // PHASE 1: Build the effective POM model
            // Create a download directory for parent POMs and BOM imports that need to be fetched from remote repositories
            Path downloadDir = extractionEnvironment.getOutputDirectory().toPath().resolve(DOWNLOADS_DIR_NAME);
            Files.createDirectories(downloadDir);
            ProjectBuilder projectBuilder = new ProjectBuilder(downloadDir,
                mavenResolverOptions != null ? mavenResolverOptions.getMirrorConfigurations() : Collections.<MavenMirrorConfig>emptyList());
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
            // Create the resolver with proxy, mirror, and diagnostics configurations.
            // NOTE: The same ProjectBuilder instance is passed into MavenModuleProcessingContext
            // below, so its internal pomCache (parent POM chain) is shared across all modules —
            // no module will re-download a parent POM that was already fetched for the root.
            MavenDependencyResolver dependencyResolver;
            boolean diagnosticsEnabled = mavenResolverOptions != null && mavenResolverOptions.isDiagnosticsEnabled();
            if (mavenResolverOptions != null && (mavenResolverOptions.hasProxyConfiguration() || mavenResolverOptions.hasMirrorConfiguration())) {
                if (mavenResolverOptions.hasProxyConfiguration()) {
                    logger.info("Creating Maven dependency resolver with proxy configuration");
                }
                if (mavenResolverOptions.hasMirrorConfiguration()) {
                    logger.info("Creating Maven dependency resolver with {} mirror(s)",
                        mavenResolverOptions.getMirrorConfigurations().size());
                }
                dependencyResolver = new MavenDependencyResolver(
                    mavenResolverOptions.getProxyConfig(),
                    mavenResolverOptions.getMirrorConfigurations(),
                    diagnosticsEnabled
                );
            } else {
                dependencyResolver = new MavenDependencyResolver(null, Collections.emptyList(), diagnosticsEnabled);
            }
            Path localRepoPath = extractionEnvironment.getOutputDirectory().toPath().resolve(LOCAL_REPO_DIR_NAME);

            // Get external repositories from configuration
            List<String> externalRepositories = mavenResolverOptions != null ? mavenResolverOptions.getExternalRepositories() : Collections.emptyList();
            if (!externalRepositories.isEmpty()) {
                logger.info("Using {} external repository URL(s) from configuration: {}",
                    externalRepositories.size(), String.join(", ", externalRepositories));
            }

            // Read test scope configuration from options (configurable via detect.maven.include.test.scope)
            boolean includeTestScope = mavenResolverOptions != null ? mavenResolverOptions.getIncludeTestScope() : true;

            // Perform compile and test dependency collection concurrently.
            // They are completely independent operations on immutable data and can safely run
            // in parallel. The MavenProxyConfigurator uses ThreadLocal storage so concurrent
            // calls do not interfere with each other's proxy save/restore cycle.
            final MavenDependencyResolver resolverRef = dependencyResolver;
            final List<String> externalReposRef = externalRepositories;
            final Path localRepoRef = localRepoPath;

            CompletableFuture<CollectResult> compileFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return resolverRef.resolveDependencies(
                        pomFile, mavenProject, localRepoRef.toFile(), MAVEN_SCOPE_COMPILE, externalReposRef);
                } catch (Exception e) {
                    throw new RuntimeException("Compile-scope dependency collection failed: " + e.getMessage(), e);
                }
            });

            CompletableFuture<CollectResult> testFuture = includeTestScope
                ? CompletableFuture.supplyAsync(() -> {
                    try {
                        return resolverRef.resolveDependencies(
                            pomFile, mavenProject, localRepoRef.toFile(), MAVEN_SCOPE_TEST, externalReposRef);
                    } catch (Exception e) {
                        throw new RuntimeException("Test-scope dependency collection failed: " + e.getMessage(), e);
                    }
                })
                : CompletableFuture.completedFuture(null);

            CollectResult collectResultCompile = compileFuture.join();
            CollectResult collectResultTest = testFuture.join();

            // PHASE 3: Write root dependency trees asynchronously (#4 optimization).
            // These are debug artifacts — fire-and-forget, collected and joined before extract() returns.
            List<CompletableFuture<Void>> treeWriteFutures = new ArrayList<>();

            final CollectResult finalCompileResult = collectResultCompile;
            final File compileTreeFile = new File(extractionEnvironment.getOutputDirectory(),
                DEPENDENCY_TREE_FILE_PREFIX + COMPILE_SCOPE_SUFFIX + TREE_FILE_EXTENSION);
            treeWriteFutures.add(CompletableFuture.runAsync(() -> {
                try { treeWriter.writeDependencyTree(finalCompileResult, compileTreeFile, MAVEN_SCOPE_COMPILE); }
                catch (Exception e) { logger.debug("Failed writing compile tree file: {}", e.getMessage()); }
            }));

            if (includeTestScope && collectResultTest != null) {
                final CollectResult finalTestResult = collectResultTest;
                final File testTreeFile = new File(extractionEnvironment.getOutputDirectory(),
                    DEPENDENCY_TREE_FILE_PREFIX + TEST_SCOPE_SUFFIX + TREE_FILE_EXTENSION);
                treeWriteFutures.add(CompletableFuture.runAsync(() -> {
                    try { treeWriter.writeDependencyTree(finalTestResult, testTreeFile, MAVEN_SCOPE_TEST); }
                    catch (Exception e) { logger.debug("Failed writing test tree file: {}", e.getMessage()); }
                }));
            }

            // PHASE 4: Transform Aether dependency graphs to Black Duck's internal DependencyGraph format
            MavenGraphParser mavenGraphParser = new MavenGraphParser();

            MavenParseResult parseResultCompile = mavenGraphParser.parse(collectResultCompile);
            MavenGraphTransformer mavenGraphTransformer = new MavenGraphTransformer(externalIdFactory);
            DependencyGraph dependencyGraphCompile = mavenGraphTransformer.transform(parseResultCompile);

            DependencyGraph dependencyGraphTest = null;
            if (includeTestScope && collectResultTest != null) {
                MavenParseResult parseResultTest = mavenGraphParser.parse(collectResultTest);
                dependencyGraphTest = mavenGraphTransformer.transform(parseResultTest);
            }

            // PHASE 4.5: Shaded dependency detection
            // ConcurrentHashMap so the cache is safe if shaded scanner is later parallelized.
            Map<String, DependencyGraph> shadedSubTreeCache = new ConcurrentHashMap<>();
            ShadedDependencyScanner shadedDependencyScanner = null;
            if (mavenResolverOptions != null && mavenResolverOptions.isIncludeShadedDependenciesEnabled()) {
                logger.info("Shaded dependency detection is enabled. Initializing scanner...");
                shadedDependencyScanner = new ShadedDependencyScanner(
                    externalIdFactory, projectBuilder, dependencyResolver,
                    mavenGraphParser, mavenGraphTransformer,
                    mavenResolverOptions.getProxyConfig()
                );
                shadedDependencyScanner.processShading(
                    collectResultCompile, collectResultTest,
                    dependencyGraphCompile, dependencyGraphTest,
                    localRepoPath, downloadDir,
                    mavenProject.getRepositories(),
                    shadedSubTreeCache, mavenResolverOptions
                );
            }

            // Create CodeLocations for the root project (both compile and test scopes).
            // synchronizedList — safe for concurrent add() from module-processing threads.
            List<CodeLocation> codeLocations = Collections.synchronizedList(new ArrayList<>());
            File rootSourcePath = pomFile.getParentFile();

            codeLocations.add(codeLocationFactory.createCodeLocation(
                dependencyGraphCompile, mavenProject, rootSourcePath));

            if (dependencyGraphTest != null) {
                // Create a separate codelocation for test-scope dependencies
                codeLocations.add(codeLocationFactory.createCodeLocation(
                    dependencyGraphTest, mavenProject, rootSourcePath));
            }

            // PHASE 5: Process multi-module Maven projects recursively in parallel.
            // Each sibling module is submitted to a dedicated ForkJoinPool so the JVM's
            // common pool is not exhausted. ForkJoinPool uses work-stealing, which prevents
            // deadlock when nested-module futures are submitted from within a running future.
            if (mavenProject.getModules() != null && !mavenProject.getModules().isEmpty()) {
                File rootDir = pomFile.getParentFile();

                int moduleThreads = mavenResolverOptions != null
                    ? mavenResolverOptions.getModuleThreadCount()
                    : Runtime.getRuntime().availableProcessors();
                logger.info("Processing {} top-level module(s) with {} thread(s).",
                    mavenProject.getModules().size(), moduleThreads);

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
                    .shadedDependencyScanner(shadedDependencyScanner)
                    .shadedSubTreeCache(shadedSubTreeCache)
                    .includeShadedDependencies(mavenResolverOptions != null && mavenResolverOptions.isIncludeShadedDependenciesEnabled())
                    .mavenResolverOptions(mavenResolverOptions)
                    .downloadDir(downloadDir)
                    .build();

                // NOTE on pomCache race: two threads may concurrently build the same parent POM
                // (e.g., a shared spring-boot-parent) if neither has cached it yet. The second
                // result simply overwrites the first with an identical value — no correctness
                // issue, just occasional redundant network work. Acceptable for this release.
                ForkJoinPool moduleExecutor = new ForkJoinPool(moduleThreads);
                try {
                    moduleProcessor.processModules(mavenProject.getModules(), rootDir, context, moduleExecutor);
                } finally {
                    moduleExecutor.shutdown();
                }

                treeWriteFutures.addAll(context.getTreeWriteFutures());
            }

            // Join all async tree-write futures before returning — guarantees debug files are
            // flushed to disk even though they were written off the critical path.
            CompletableFuture.allOf(treeWriteFutures.toArray(new CompletableFuture[0])).join();

            // PHASE 6: Return all collected code locations (root + all modules)
            String projectName = mavenProject.getCoordinates().getGroupId() + ":" + mavenProject.getCoordinates().getArtifactId();
            NameVersion nameVersion = new NameVersion(projectName, mavenProject.getCoordinates().getVersion());

            return new Extraction.Builder().success(codeLocations).nameVersion(nameVersion).build();

        } catch (Exception e) {
            logger.error("Failed to resolve dependencies for pom.xml: {}", e.getMessage());
            return new Extraction.Builder().exception(e).build();
        }
    }
}
