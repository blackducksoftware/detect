package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
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
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    // Dependency tree output file naming constants
    private static final String DEPENDENCY_TREE_FILE_PREFIX = "dependency-tree-";
    private static final String COMPILE_SCOPE_SUFFIX = "compile";
    private static final String TEST_SCOPE_SUFFIX = "test";
    private static final String TREE_FILE_EXTENSION = ".txt";
    private static final String ERROR_FILE_SUFFIX = "-ERROR";

    // Maven scope constants
    private static final String MAVEN_SCOPE_COMPILE = "compile";
    private static final String MAVEN_SCOPE_TEST = "test";

    // Fallback values for error conditions
    private static final String UNKNOWN_GAV = "unknown:unknown:unknown";
    private static final String UNKNOWN_NAME = "unknown";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final FileFinder fileFinder;
    private final ExternalIdFactory externalIdFactory;

    // Cached POM file location from applicable() phase
    private File pomFile;

    /**
     * Constructs a Maven Resolver Detectable.
     *
     * @param environment The detectable environment providing context for detection
     * @param fileFinder Utility for locating files in the project directory
     * @param externalIdFactory Factory for creating Maven external identifiers for code locations
     */
    public MavenResolverDetectable(
            DetectableEnvironment environment,
            FileFinder fileFinder,
            ExternalIdFactory externalIdFactory
    ) {
        super(environment);
        this.fileFinder = fileFinder;
        this.externalIdFactory = externalIdFactory;
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
            try (PrintStream printStream = new PrintStream(dependencyTreeCompileFile)) {
                collectResultCompile.getRoot().accept(new DependencyGraphDumper(printStream::println));
            }
            logger.info("Compile dependency tree saved to: {}", dependencyTreeCompileFile.getAbsolutePath());

            File dependencyTreeTestFile = null;
            if (includeTestScope && collectResultTest != null) {
                dependencyTreeTestFile = new File(extractionEnvironment.getOutputDirectory(),
                    DEPENDENCY_TREE_FILE_PREFIX + TEST_SCOPE_SUFFIX + TREE_FILE_EXTENSION);
                try (PrintStream printStream = new PrintStream(dependencyTreeTestFile)) {
                    collectResultTest.getRoot().accept(new DependencyGraphDumper(printStream::println));
                }
                logger.info("Test dependency tree saved to: {}", dependencyTreeTestFile.getAbsolutePath());
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

            // Create CodeLocations for the root project (both compile and test scopes)
            // CodeLocation ties together the dependency graph, external ID, and source path
            List<CodeLocation> codeLocations = new ArrayList<>();
            try {
                ExternalId rootExternalId = externalIdFactory.createMavenExternalId(
                    mavenProject.getCoordinates().getGroupId(),
                    mavenProject.getCoordinates().getArtifactId(),
                    mavenProject.getCoordinates().getVersion()
                );
                File rootSourcePath = pomFile.getParentFile();
                codeLocations.add(new CodeLocation(dependencyGraphCompile, rootExternalId, rootSourcePath));
                if (dependencyGraphTest != null) {
                    // Create a separate codelocation for test-scope dependencies
                    codeLocations.add(new CodeLocation(dependencyGraphTest, rootExternalId, rootSourcePath));
                }
            } catch (Exception e) {
                // If external ID creation fails, fall back to code locations without external ID
                logger.debug("Failed to create root external id for code location: {}", e.getMessage());
                codeLocations.add(new CodeLocation(dependencyGraphCompile));
                if (dependencyGraphTest != null) {
                    codeLocations.add(new CodeLocation(dependencyGraphTest));
                }
            }

            // PHASE 5: Process multi-module Maven projects recursively
            // Each module gets its own code locations for compile and test scopes
            if (mavenProject.getModules() != null && !mavenProject.getModules().isEmpty()) {
                File rootDir = pomFile.getParentFile();
                Set<String> visitedModulePomPaths = new HashSet<>();

                for (String module : mavenProject.getModules()) {
                    processModuleRecursive(
                        module,
                        rootDir,
                        projectBuilder,
                        dependencyResolver,
                        localRepoPath,
                        extractionEnvironment.getOutputDirectory(),
                        includeTestScope,
                        externalIdFactory,
                        codeLocations,
                        visitedModulePomPaths,
                        mavenGraphParser,
                        mavenGraphTransformer
                    );
                }
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
     * Recursively processes Maven modules declared in a multi-module project.
     *
     * <p>This method handles the complete workflow for each Maven module:
     * <ol>
     *   <li><strong>Path Resolution:</strong> Resolves the module's pom.xml path from the module entry,
     *       handling both directory references and explicit pom.xml files.</li>
     *   <li><strong>Canonicalization:</strong> Normalizes file paths to prevent processing the same module
     *       multiple times due to symlinks or relative path differences.</li>
     *   <li><strong>Cycle Detection:</strong> Tracks visited module paths to prevent infinite recursion
     *       in circular module dependencies.</li>
     *   <li><strong>Build Effective Model:</strong> Constructs the module's effective POM, inheriting
     *       from parent POMs and importing BOMs as needed.</li>
     *   <li><strong>Resolve Dependencies:</strong> Resolves compile and test scope dependencies for the module
     *       using Eclipse Aether.</li>
     *   <li><strong>Write Debug Trees:</strong> Outputs dependency trees for debugging, with unique filenames
     *       based on GAV and path hash to prevent collisions.</li>
     *   <li><strong>Transform and Emit:</strong> Converts dependency graphs and creates CodeLocations for
     *       Black Duck analysis.</li>
     *   <li><strong>Recurse:</strong> Processes any nested modules declared by this module.</li>
     * </ol>
     *
     * <p><strong>Error Handling:</strong> If any phase fails for a module, an error marker file is written
     * for debugging, but processing continues with the next module to maximize extraction coverage.
     *
     * <p><strong>State Mutation:</strong> This method mutates two shared collections:
     * <ul>
     *   <li>{@code codeLocations} - Accumulates all discovered code locations across modules</li>
     *   <li>{@code visitedModulePomPaths} - Tracks visited modules to prevent cycles</li>
     * </ul>
     *
     * @param moduleEntry The module identifier from the parent POM's {@code <modules>} section (e.g., "submodule" or "path/to/module")
     * @param parentDir The parent directory containing the module (typically the parent POM's directory)
     * @param projectBuilder Builder for constructing effective Maven project models
     * @param dependencyResolver Resolver for Maven dependencies using Eclipse Aether
     * @param localRepoPath Path to the local Maven repository for artifact caching
     * @param outputDir Directory for writing dependency tree and error files
     * @param includeTestScope Whether to resolve and include test-scoped dependencies
     * @param externalIdFactory Factory for creating Maven external identifiers
     * @param codeLocations Accumulator list for all discovered code locations (mutated by this method)
     * @param visitedModulePomPaths Set of already-processed module paths for cycle detection (mutated by this method)
     * @param mavenGraphParser Parser for converting Aether CollectResult to internal parse result
     * @param mavenGraphTransformer Transformer for converting parse results to dependency graphs
     */
    private void processModuleRecursive(
        String moduleEntry,
        File parentDir,
        ProjectBuilder projectBuilder,
        MavenDependencyResolver dependencyResolver,
        Path localRepoPath,
        File outputDir,
        boolean includeTestScope,
        ExternalIdFactory externalIdFactory,
        List<CodeLocation> codeLocations,
        Set<String> visitedModulePomPaths,
        MavenGraphParser mavenGraphParser,
        MavenGraphTransformer mavenGraphTransformer
    ) {
        try {
            // Early exit for null or empty module entries
            if (moduleEntry == null || moduleEntry.trim().isEmpty()) {
                return;
            }
            String modulePathStr = moduleEntry.trim();

            // STEP 1: Resolve module path to its pom.xml file
            // Maven module entries can be either:
            // - A directory name (we append pom.xml)
            // - A path to a pom.xml file directly
            File modulePom = new File(parentDir, modulePathStr);
            if (modulePom.isDirectory()) {
                modulePom = new File(modulePom, POM_XML_FILENAME);
            }
            // Fallback: try appending pom.xml if direct path doesn't exist
            if (!modulePom.exists()) {
                File alternative = new File(parentDir, modulePathStr + File.separator + POM_XML_FILENAME);
                if (alternative.exists()) {
                    modulePom = alternative;
                }
            }

            // STEP 2: Canonicalize the file path to normalize it
            // This prevents processing the same module twice due to symlinks or ../.. references
            File canonicalModulePom = modulePom;
            try {
                canonicalModulePom = modulePom.getCanonicalFile();
            } catch (Exception e) {
                logger.debug("Could not canonicalize module pom path {}, continuing with absolute path.", modulePom.getAbsolutePath());
            }

            // Validate that the resolved POM file exists and is readable
            if (!canonicalModulePom.exists() || !canonicalModulePom.isFile()) {
                logger.warn("Module POM not found for module '{}' at expected path '{}'. Skipping module.", modulePathStr, canonicalModulePom.getAbsolutePath());
                return;
            }

            // STEP 3: Check for cycles - have we already processed this module?
            // Use canonical path as the key to prevent duplicate processing
            String modulePomPathKey;
            try {
                modulePomPathKey = canonicalModulePom.getCanonicalPath();
            } catch (Exception e) {
                modulePomPathKey = canonicalModulePom.getAbsolutePath();
            }

            if (visitedModulePomPaths.contains(modulePomPathKey)) {
                logger.debug("Skipping already-visited module POM: {}", modulePomPathKey);
                return;
            }
            visitedModulePomPaths.add(modulePomPathKey);

            logger.info("Processing module POM: {}", canonicalModulePom.getAbsolutePath());

            // STEP 4: Build the effective Maven project model for this module
            MavenProject moduleProject;
            try {
                moduleProject = projectBuilder.buildProject(canonicalModulePom);
                logger.info("Module '{}' dependencies: {}", modulePomPathKey,
                        moduleProject.getDependencies() == null ? "[]" :
                                moduleProject.getDependencies().stream()
                                        .map(d -> d.getCoordinates().getArtifactId() + ":" + d.getCoordinates().getVersion())
                                        .map(s -> "\"" + s + "\"")
                                        .collect(java.util.stream.Collectors.joining(",", "[", "]"))
                );
            } catch (Exception e) {
                logger.warn("Failed to build effective project for module '{}': {}", modulePomPathKey, e.getMessage());
                // Save an error marker file to aid debugging
                writeErrorTreeFile(outputDir, MAVEN_SCOPE_COMPILE, "BUILD-PROJECT-ERROR", modulePomPathKey, e.getMessage());
                return;
            }

            // STEP 5: Resolve dependencies for the module (compile scope)
            CollectResult moduleCollectCompile;
            try {
                moduleCollectCompile = dependencyResolver.resolveDependencies(
                    canonicalModulePom, moduleProject, localRepoPath.toFile(), MAVEN_SCOPE_COMPILE);
            } catch (org.eclipse.aether.collection.DependencyCollectionException e) {
                logger.warn("Compile dependency resolution failed for module '{}': {}", modulePomPathKey, e.getMessage());
                writeErrorTreeFile(outputDir, MAVEN_SCOPE_COMPILE, gavOf(moduleProject), modulePomPathKey, e.getMessage());
                return;
            }

            // STEP 6: Resolve test scope dependencies (if enabled)
            CollectResult moduleCollectTest = null;
            if (includeTestScope) {
                try {
                    moduleCollectTest = dependencyResolver.resolveDependencies(
                        canonicalModulePom, moduleProject, localRepoPath.toFile(), MAVEN_SCOPE_TEST);
                } catch (org.eclipse.aether.collection.DependencyCollectionException e) {
                    logger.warn("Test dependency resolution failed for module '{}': {}", modulePomPathKey, e.getMessage());
                    writeErrorTreeFile(outputDir, MAVEN_SCOPE_TEST, gavOf(moduleProject), modulePomPathKey, e.getMessage());
                    // Continue processing - test scope failure is not critical
                }
            }

            // STEP 7: Write dependency trees to files for debugging
            // Use hash of path to ensure unique filenames for modules with same GAV
            try {
                String gav = gavOf(moduleProject);
                String hash = Integer.toHexString(modulePomPathKey.hashCode());

                File compileOut = new File(outputDir,
                    DEPENDENCY_TREE_FILE_PREFIX + COMPILE_SCOPE_SUFFIX + "-" + safeName(gav) + "-" + hash + TREE_FILE_EXTENSION);
                try (PrintStream ps = new PrintStream(compileOut)) {
                    moduleCollectCompile.getRoot().accept(new DependencyGraphDumper(ps::println));
                }
                logger.info("Module compile dependency tree saved to: {}", compileOut.getAbsolutePath());

                if (includeTestScope && moduleCollectTest != null) {
                    File testOut = new File(outputDir,
                        DEPENDENCY_TREE_FILE_PREFIX + TEST_SCOPE_SUFFIX + "-" + safeName(gav) + "-" + hash + TREE_FILE_EXTENSION);
                    try (PrintStream ps = new PrintStream(testOut)) {
                        moduleCollectTest.getRoot().accept(new DependencyGraphDumper(ps::println));
                    }
                    logger.info("Module test dependency tree saved to: {}", testOut.getAbsolutePath());
                }
            } catch (Exception e) {
                logger.debug("Failed to write module dependency tree files for '{}': {}", modulePomPathKey, e.getMessage());
            }

            // STEP 8: Transform Aether graphs to Black Duck dependency graphs
            MavenParseResult moduleParseCompile = mavenGraphParser.parse(moduleCollectCompile);
            DependencyGraph moduleGraphCompile = new MavenGraphTransformer(externalIdFactory).transform(moduleParseCompile);

            DependencyGraph moduleGraphTest = null;
            if (includeTestScope && moduleCollectTest != null) {
                MavenParseResult moduleParseTest = mavenGraphParser.parse(moduleCollectTest);
                moduleGraphTest = new MavenGraphTransformer(externalIdFactory).transform(moduleParseTest);
            }

            // STEP 9: Create CodeLocations for this module and add to the accumulator
            try {
                ExternalId moduleExternalId = externalIdFactory.createMavenExternalId(
                    moduleProject.getCoordinates().getGroupId(),
                    moduleProject.getCoordinates().getArtifactId(),
                    moduleProject.getCoordinates().getVersion()
                );
                File moduleSourcePath = canonicalModulePom.getParentFile();
                codeLocations.add(new CodeLocation(moduleGraphCompile, moduleExternalId, moduleSourcePath));
                if (moduleGraphTest != null) {
                    codeLocations.add(new CodeLocation(moduleGraphTest, moduleExternalId, moduleSourcePath));
                }
            } catch (Exception e) {
                // Fallback: create code location without external ID if creation fails
                logger.debug("Failed to create module external id for '{}': {}", modulePomPathKey, e.getMessage());
                codeLocations.add(new CodeLocation(moduleGraphCompile));
                if (moduleGraphTest != null) {
                    codeLocations.add(new CodeLocation(moduleGraphTest));
                }
            }

            // STEP 10: Recursively process any nested modules declared by this module
            List<String> nestedModules = moduleProject.getModules();
            if (nestedModules != null && !nestedModules.isEmpty()) {
                File childParentDir = canonicalModulePom.getParentFile();
                for (String childModule : nestedModules) {
                    processModuleRecursive(
                        childModule,
                        childParentDir,
                        projectBuilder,
                        dependencyResolver,
                        localRepoPath,
                        outputDir,
                        includeTestScope,
                        externalIdFactory,
                        codeLocations,
                        visitedModulePomPaths,
                        mavenGraphParser,
                        mavenGraphTransformer
                    );
                }
            }
        } catch (Exception e) {
            // Catch-all for unexpected errors - log and continue processing other modules
            logger.warn("Failed processing module '{}' due to: {}", moduleEntry, e.getMessage());
        }
    }

    /**
     * Formats a Maven project's coordinates as a GAV (GroupId:ArtifactId:Version) string.
     *
     * <p>This method safely extracts coordinates even if some fields are null or the
     * coordinates object itself is malformed.
     *
     * @param project The Maven project to extract coordinates from
     * @return GAV string in the format "groupId:artifactId:version", or "unknown:unknown:unknown" if extraction fails
     */
    private String gavOf(MavenProject project) {
        try {
            return project.getCoordinates().getGroupId() + ":" +
                   project.getCoordinates().getArtifactId() + ":" +
                   project.getCoordinates().getVersion();
        } catch (Exception e) {
            return UNKNOWN_GAV;
        }
    }

    /**
     * Sanitizes a string for safe use in filenames by replacing problematic characters.
     *
     * <p>Replaces forward slashes, backslashes, and colons with underscores to ensure
     * the resulting string can be used in file paths on all operating systems.
     *
     * @param s The string to sanitize
     * @return Sanitized string safe for use in filenames, or "unknown" if input is null
     */
    private String safeName(String s) {
        if (s == null) return UNKNOWN_NAME;
        // Replace characters that are problematic in filenames across different operating systems
        return s.replace('/', '_').replace('\\', '_').replace(':', '_');
    }

    /**
     * Writes an error marker file when dependency resolution fails for a module.
     *
     * <p>These error files help with debugging by capturing the scope, module coordinates,
     * file path, and failure reason. The filename includes a hash to prevent collisions
     * when multiple modules have similar GAV coordinates.
     *
     * <p>This is a best-effort operation - if writing the error file itself fails,
     * the exception is silently ignored to prevent cascading failures.
     *
     * @param outputDir Directory where the error file should be written
     * @param scope The Maven scope being resolved when the error occurred (e.g., "compile", "test")
     * @param gavOrTag GAV coordinates or error tag identifying the module
     * @param modulePathKey The canonical or absolute path to the module's POM file
     * @param message The error message describing why resolution failed
     */
    private void writeErrorTreeFile(File outputDir, String scope, String gavOrTag, String modulePathKey, String message) {
        try {
            String hash = Integer.toHexString(modulePathKey == null ? 0 : modulePathKey.hashCode());
            String base = DEPENDENCY_TREE_FILE_PREFIX + scope + "-" + safeName(gavOrTag) + "-" + hash + ERROR_FILE_SUFFIX + TREE_FILE_EXTENSION;
            File out = new File(outputDir, base);
            try (PrintStream ps = new PrintStream(out)) {
                ps.println("Dependency collection failed for scope '" + scope + "'.");
                ps.println("Module: " + gavOrTag);
                ps.println("Path: " + modulePathKey);
                ps.println("Reason: " + message);
            }
            logger.info("Module {} dependency tree error file saved to: {}", scope, out.getAbsolutePath());
        } catch (Exception ignored) {
            // Best-effort only - don't let error file writing failure cause additional problems
        }
    }
}
