package com.blackduck.integration.detectable.detectables.maven.resolver.module;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.detectables.maven.resolver.graph.MavenParseResult;
import com.blackduck.integration.detectable.detectables.maven.resolver.pom.MavenProject;
import org.eclipse.aether.collection.CollectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Processor for handling Maven multi-module projects recursively.
 *
 * <p>This class encapsulates the logic for processing Maven modules, including:
 * <ul>
 *   <li>Path resolution and canonicalization</li>
 *   <li>Cycle detection to prevent infinite recursion</li>
 *   <li>Building effective POMs for each module</li>
 *   <li>Resolving dependencies for compile and test scopes</li>
 *   <li>Generating dependency trees and code locations</li>
 *   <li>Recursive processing of nested modules</li>
 * </ul>
 *
 * <p><strong>Design Pattern:</strong> Strategy pattern - encapsulates the module processing algorithm,
 * allowing it to be used independently or as part of a larger workflow.
 *
 * <p><strong>Error Handling:</strong> This processor is designed to be resilient. If processing fails
 * for a specific module, it logs the error, writes an error marker file, and continues processing
 * other modules to maximize extraction coverage.
 */
public class MavenModuleProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MavenModuleProcessor.class);
    private static final String POM_XML_FILENAME = "pom.xml";

    /**
     * Processes all modules declared in a Maven project in parallel.
     *
     * <p>Sibling modules at every level are submitted to {@code executor} concurrently.
     * Nested module recursion stays within each future (depth-first per branch), so the
     * module tree is traversed correctly while siblings run in parallel.
     *
     * <p>All thread-safety prerequisites must be satisfied before calling this with a
     * multi-threaded executor (visitedModulePomPaths, codeLocations, pomCache, etc.).
     *
     * @param modules   sibling modules to process
     * @param parentDir parent directory containing the modules
     * @param context   shared processing context (all mutable state is thread-safe)
     * @param executor  executor for parallel submission; use a single-thread executor to
     *                  disable parallelism without changing logic
     */
    public void processModules(List<String> modules, File parentDir, MavenModuleProcessingContext context, Executor executor) {
        if (modules == null || modules.isEmpty()) {
            return;
        }

        List<CompletableFuture<Void>> futures = modules.stream()
            .map(module -> CompletableFuture.runAsync(
                () -> processModuleRecursive(module, parentDir, context, executor),
                executor))
            .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Recursively processes a single Maven module.
     *
     * <p>This method handles the complete workflow for a Maven module:
     * <ol>
     *   <li>Path resolution and validation</li>
     *   <li>Canonicalization and cycle detection</li>
     *   <li>Effective POM building</li>
     *   <li>Dependency resolution (compile and test scopes)</li>
     *   <li>Dependency tree file writing</li>
     *   <li>Graph transformation and CodeLocation creation</li>
     *   <li>Recursive processing of nested modules</li>
     * </ol>
     *
     * @param moduleEntry The module identifier (e.g., "submodule" or "path/to/module")
     * @param parentDir The parent directory containing this module
     * @param context Processing context with all dependencies and shared state
     */
    private void processModuleRecursive(String moduleEntry, File parentDir, MavenModuleProcessingContext context, Executor executor) {
        try {
            // STEP 1: Early exit for null or empty module entries
            if (moduleEntry == null || moduleEntry.trim().isEmpty()) {
                return;
            }
            String modulePathStr = moduleEntry.trim();

            // STEP 2: Resolve module path to its pom.xml file
            File modulePom = resolveModulePomFile(modulePathStr, parentDir);
            if (modulePom == null) {
                return;
            }

            // STEP 3: Canonicalize and check for cycles
            File canonicalModulePom = canonicalizeFile(modulePom);
            String modulePomPathKey = getCanonicalPathSafe(canonicalModulePom);

            // ConcurrentHashMap key-set: add() returns false if already present — atomic check-and-add.
            if (!context.getVisitedModulePomPaths().add(modulePomPathKey)) {
                logger.debug("Skipping already-visited module POM: {}", modulePomPathKey);
                return;
            }

            logger.info("Processing module POM: {}", canonicalModulePom.getAbsolutePath());

            // STEP 4: Build the effective Maven project model for this module
            MavenProject moduleProject = buildModuleProject(canonicalModulePom, modulePomPathKey, context);
            if (moduleProject == null) {
                return;
            }

            // STEP 5: Resolve compile and test scope dependencies concurrently.
            CollectResult moduleCollectCompile;
            CollectResult moduleCollectTest;
            if (context.isIncludeTestScope()) {
                CompletableFuture<CollectResult> compileFuture = CompletableFuture.supplyAsync(
                    () -> resolveCompileDependencies(canonicalModulePom, moduleProject, modulePomPathKey, context), executor);
                CompletableFuture<CollectResult> testFuture = CompletableFuture.supplyAsync(
                    () -> resolveTestDependencies(canonicalModulePom, moduleProject, modulePomPathKey, context), executor);
                moduleCollectCompile = compileFuture.join();
                moduleCollectTest = testFuture.join();
            } else {
                moduleCollectCompile = resolveCompileDependencies(canonicalModulePom, moduleProject, modulePomPathKey, context);
                moduleCollectTest = null;
            }

            if (moduleCollectCompile == null) {
                return;
            }

            // STEP 6: Write dependency trees to files for debugging (async)
            writeDependencyTreeFiles(moduleProject, moduleCollectCompile, moduleCollectTest, modulePomPathKey, context);

            // STEP 7: Transform graphs and create CodeLocations
            createCodeLocationsForModule(
                moduleProject, moduleCollectCompile, moduleCollectTest, canonicalModulePom, context);

            // STEP 8: Recursively process nested modules (siblings parallelized via the same executor)
            processNestedModules(moduleProject, canonicalModulePom, context, executor);

        } catch (Exception e) {
            logger.warn("Failed processing module '{}' due to: {}", moduleEntry, e.getMessage());
        }
    }

    /**
     * Resolves a module entry to its pom.xml file.
     *
     * <p>Maven module entries can be:
     * <ul>
     *   <li>A directory name (we append pom.xml)</li>
     *   <li>A path to a pom.xml file directly</li>
     * </ul>
     *
     * @param modulePathStr The module path string
     * @param parentDir The parent directory
     * @return The resolved POM file, or null if not found
     */
    private File resolveModulePomFile(String modulePathStr, File parentDir) {
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

        // Canonicalize for validation
        File canonicalPom = canonicalizeFile(modulePom);

        if (!canonicalPom.exists() || !canonicalPom.isFile()) {
            logger.warn("Module POM not found for module '{}' at expected path '{}'. Skipping module.",
                modulePathStr, canonicalPom.getAbsolutePath());
            return null;
        }

        return modulePom;
    }

    /**
     * Canonicalizes a file path to normalize it.
     *
     * <p>This prevents processing the same module twice due to symlinks or relative path references.
     *
     * @param file The file to canonicalize
     * @return The canonical file, or the original if canonicalization fails
     */
    private File canonicalizeFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (Exception e) {
            logger.debug("Could not canonicalize path {}, continuing with absolute path.", file.getAbsolutePath());
            return file;
        }
    }

    /**
     * Safely gets the canonical path of a file, falling back to absolute path.
     *
     * @param file The file
     * @return The canonical path if available, otherwise absolute path
     */
    private String getCanonicalPathSafe(File file) {
        try {
            return file.getCanonicalPath();
        } catch (Exception e) {
            return file.getAbsolutePath();
        }
    }

    /**
     * Builds the effective Maven project model for a module.
     *
     * @param modulePom The module's POM file
     * @param modulePomPathKey The module's path key (for logging)
     * @param context Processing context
     * @return The MavenProject, or null if building fails
     */
    private MavenProject buildModuleProject(File modulePom, String modulePomPathKey, MavenModuleProcessingContext context) {
        try {
            MavenProject moduleProject = context.getProjectBuilder().buildProject(modulePom);
            logger.info("Module '{}' dependencies: {}", modulePomPathKey,
                moduleProject.getDependencies() == null ? "[]" :
                    moduleProject.getDependencies().stream()
                        .map(d -> d.getCoordinates().getArtifactId() + ":" + d.getCoordinates().getVersion())
                        .map(s -> "\"" + s + "\"")
                        .collect(java.util.stream.Collectors.joining(",", "[", "]"))
            );
            return moduleProject;
        } catch (Exception e) {
            logger.warn("Failed to build effective project for module '{}': {}", modulePomPathKey, e.getMessage());
            context.getTreeWriter().writeErrorTreeFile(
                context.getOutputDir(),
                context.getCompileScope(),
                "BUILD-PROJECT-ERROR",
                modulePomPathKey,
                e.getMessage()
            );
            return null;
        }
    }

    /**
     * Resolves compile-scope dependencies for a module.
     *
     * @param modulePom The module's POM file
     * @param moduleProject The module's project model
     * @param modulePomPathKey The module's path key (for logging)
     * @param context Processing context
     * @return The CollectResult, or null if resolution fails
     */
    private CollectResult resolveCompileDependencies(
        File modulePom, MavenProject moduleProject, String modulePomPathKey, MavenModuleProcessingContext context
    ) {
        try {
            return context.getDependencyResolver().resolveDependencies(
                modulePom, moduleProject, context.getLocalRepoPath().toFile(), context.getCompileScope());
        } catch (org.eclipse.aether.collection.DependencyCollectionException e) {
            logger.warn("Compile dependency resolution failed for module '{}': {}", modulePomPathKey, e.getMessage());
            context.getTreeWriter().writeErrorTreeFile(
                context.getOutputDir(),
                context.getCompileScope(),
                context.getCoordinateFormatter().formatGAV(moduleProject),
                modulePomPathKey,
                e.getMessage()
            );
            return null;
        }
    }

    /**
     * Resolves test-scope dependencies for a module (if enabled).
     *
     * @param modulePom The module's POM file
     * @param moduleProject The module's project model
     * @param modulePomPathKey The module's path key (for logging)
     * @param context Processing context
     * @return The CollectResult, or null if resolution fails or test scope is disabled
     */
    private CollectResult resolveTestDependencies(
        File modulePom, MavenProject moduleProject, String modulePomPathKey, MavenModuleProcessingContext context
    ) {
        if (!context.isIncludeTestScope()) {
            return null;
        }

        try {
            return context.getDependencyResolver().resolveDependencies(
                modulePom, moduleProject, context.getLocalRepoPath().toFile(), context.getTestScope());
        } catch (org.eclipse.aether.collection.DependencyCollectionException e) {
            logger.warn("Test dependency resolution failed for module '{}': {}", modulePomPathKey, e.getMessage());
            context.getTreeWriter().writeErrorTreeFile(
                context.getOutputDir(),
                context.getTestScope(),
                context.getCoordinateFormatter().formatGAV(moduleProject),
                modulePomPathKey,
                e.getMessage()
            );
            // Test scope failure is not critical - return null but continue processing
            return null;
        }
    }

    /**
     * Writes dependency tree files for a module.
     *
     * @param moduleProject The module's project model
     * @param compileResult The compile-scope CollectResult
     * @param testResult The test-scope CollectResult (may be null)
     * @param modulePomPathKey The module's path key
     * @param context Processing context
     */
    private void writeDependencyTreeFiles(
        MavenProject moduleProject,
        CollectResult compileResult,
        CollectResult testResult,
        String modulePomPathKey,
        MavenModuleProcessingContext context
    ) {
        // Write tree files asynchronously (#4 optimization) — they are debug artifacts and
        // must not block the critical dependency-resolution path.
        // Futures are collected in the context and joined by the caller before extract() returns.
        try {
            String gav = context.getCoordinateFormatter().formatGAV(moduleProject);
            String hash = Integer.toHexString(modulePomPathKey.hashCode());

            String compileFileName = context.getTreeWriter().buildDependencyTreeFileName("compile", gav, hash);
            File compileOut = new File(context.getOutputDir(), compileFileName);
            context.getTreeWriteFutures().add(CompletableFuture.runAsync(() -> {
                try { context.getTreeWriter().writeDependencyTree(compileResult, compileOut, context.getCompileScope()); }
                catch (Exception e) { logger.debug("Failed writing compile tree for '{}': {}", modulePomPathKey, e.getMessage()); }
            }));

            if (testResult != null) {
                String testFileName = context.getTreeWriter().buildDependencyTreeFileName("test", gav, hash);
                File testOut = new File(context.getOutputDir(), testFileName);
                context.getTreeWriteFutures().add(CompletableFuture.runAsync(() -> {
                    try { context.getTreeWriter().writeDependencyTree(testResult, testOut, context.getTestScope()); }
                    catch (Exception e) { logger.debug("Failed writing test tree for '{}': {}", modulePomPathKey, e.getMessage()); }
                }));
            }
        } catch (Exception e) {
            logger.debug("Failed to schedule module dependency tree writes for '{}': {}", modulePomPathKey, e.getMessage());
        }
    }

    /**
     * Creates CodeLocations for a module and adds them to the accumulator.
     *
     * @param moduleProject The module's project model
     * @param compileResult The compile-scope CollectResult
     * @param testResult The test-scope CollectResult (may be null)
     * @param modulePom The module's POM file
     * @param context Processing context
     */
    private void createCodeLocationsForModule(
        MavenProject moduleProject,
        CollectResult compileResult,
        CollectResult testResult,
        File modulePom,
        MavenModuleProcessingContext context
    ) {
        // Transform compile graph
        MavenParseResult compileParseResult = context.getGraphParser().parse(compileResult);
        DependencyGraph compileGraph = context.getGraphTransformer().transform(compileParseResult);

        // Transform test graph if available
        DependencyGraph testGraph = null;
        if (testResult != null) {
            MavenParseResult testParseResult = context.getGraphParser().parse(testResult);
            testGraph = context.getGraphTransformer().transform(testParseResult);
        }

        // Shaded dependency detection for this module
        if (context.isIncludeShadedDependencies() && context.getShadedDependencyScanner() != null) {
            context.getShadedDependencyScanner().processShading(
                compileResult, testResult,
                compileGraph, testGraph,
                context.getLocalRepoPath(), context.getDownloadDir(),
                moduleProject.getRepositories() != null ? moduleProject.getRepositories() : Collections.emptyList(),
                context.getShadedSubTreeCache(),
                context.getMavenResolverOptions()
            );
        }

        // Create and add CodeLocations
        File moduleSourcePath = modulePom.getParentFile();

        context.getCodeLocations().add(
            context.getCodeLocationFactory().createCodeLocation(compileGraph, moduleProject, moduleSourcePath)
        );

        if (testGraph != null) {
            context.getCodeLocations().add(
                context.getCodeLocationFactory().createCodeLocation(testGraph, moduleProject, moduleSourcePath)
            );
        }
    }

    /**
     * Recursively processes nested modules declared by a module.
     *
     * @param moduleProject The parent module's project model
     * @param modulePom The parent module's POM file
     * @param context Processing context
     */
    private void processNestedModules(MavenProject moduleProject, File modulePom, MavenModuleProcessingContext context, Executor executor) {
        List<String> nestedModules = moduleProject.getModules();
        if (nestedModules == null || nestedModules.isEmpty()) {
            return;
        }

        File childParentDir = modulePom.getParentFile();
        // Recurse — siblings at this nested level are also parallelized via the same executor.
        processModules(nestedModules, childParentDir, context, executor);
    }
}

