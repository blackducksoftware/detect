package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.detectables.maven.resolver.result.MavenParseResult;
import org.eclipse.aether.collection.CollectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

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
class MavenModuleProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MavenModuleProcessor.class);
    private static final String POM_XML_FILENAME = "pom.xml";

    /**
     * Processes all modules declared in a Maven project.
     *
     * <p>This is the main entry point for module processing. It iterates through the list
     * of modules and processes each one recursively.
     *
     * @param modules List of module identifiers from the parent POM's {@code <modules>} section
     * @param parentDir The parent directory containing the modules
     * @param context Processing context containing all dependencies and shared state
     */
    public void processModules(List<String> modules, File parentDir, MavenModuleProcessingContext context) {
        if (modules == null || modules.isEmpty()) {
            return;
        }

        for (String module : modules) {
            processModuleRecursive(module, parentDir, context);
        }
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
    private void processModuleRecursive(String moduleEntry, File parentDir, MavenModuleProcessingContext context) {
        try {
            // STEP 1: Early exit for null or empty module entries
            if (moduleEntry == null || moduleEntry.trim().isEmpty()) {
                return;
            }
            String modulePathStr = moduleEntry.trim();

            // STEP 2: Resolve module path to its pom.xml file
            File modulePom = resolveModulePomFile(modulePathStr, parentDir);
            if (modulePom == null) {
                return; // Warning already logged in resolveModulePomFile
            }

            // STEP 3: Canonicalize and check for cycles
            File canonicalModulePom = canonicalizeFile(modulePom);
            String modulePomPathKey = getCanonicalPathSafe(canonicalModulePom);

            if (context.getVisitedModulePomPaths().contains(modulePomPathKey)) {
                logger.debug("Skipping already-visited module POM: {}", modulePomPathKey);
                return;
            }
            context.getVisitedModulePomPaths().add(modulePomPathKey);

            logger.info("Processing module POM: {}", canonicalModulePom.getAbsolutePath());

            // STEP 4: Build the effective Maven project model for this module
            MavenProject moduleProject = buildModuleProject(canonicalModulePom, modulePomPathKey, context);
            if (moduleProject == null) {
                return; // Error already logged and error file written
            }

            // STEP 5: Resolve dependencies for both compile and test scopes
            CollectResult moduleCollectCompile = resolveCompileDependencies(
                canonicalModulePom, moduleProject, modulePomPathKey, context);
            if (moduleCollectCompile == null) {
                return; // Error already logged and error file written
            }

            CollectResult moduleCollectTest = resolveTestDependencies(
                canonicalModulePom, moduleProject, modulePomPathKey, context);
            // Test scope failure is not critical - continue processing

            // STEP 6: Write dependency trees to files for debugging
            writeDependencyTreeFiles(moduleProject, moduleCollectCompile, moduleCollectTest, modulePomPathKey, context);

            // STEP 7: Transform graphs and create CodeLocations
            createCodeLocationsForModule(
                moduleProject, moduleCollectCompile, moduleCollectTest, canonicalModulePom, context);

            // STEP 8: Recursively process nested modules
            processNestedModules(moduleProject, canonicalModulePom, context);

        } catch (Exception e) {
            // Catch-all for unexpected errors - log and continue processing other modules
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
        try {
            String gav = context.getCoordinateFormatter().formatGAV(moduleProject);
            String hash = Integer.toHexString(modulePomPathKey.hashCode());

            // Write compile tree
            String compileFileName = context.getTreeWriter().buildDependencyTreeFileName("compile", gav, hash);
            File compileOut = new File(context.getOutputDir(), compileFileName);
            context.getTreeWriter().writeDependencyTree(compileResult, compileOut, context.getCompileScope());

            // Write test tree if available
            if (testResult != null) {
                String testFileName = context.getTreeWriter().buildDependencyTreeFileName("test", gav, hash);
                File testOut = new File(context.getOutputDir(), testFileName);
                context.getTreeWriter().writeDependencyTree(testResult, testOut, context.getTestScope());
            }
        } catch (Exception e) {
            logger.debug("Failed to write module dependency tree files for '{}': {}", modulePomPathKey, e.getMessage());
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
    private void processNestedModules(MavenProject moduleProject, File modulePom, MavenModuleProcessingContext context) {
        List<String> nestedModules = moduleProject.getModules();
        if (nestedModules == null || nestedModules.isEmpty()) {
            return;
        }

        File childParentDir = modulePom.getParentFile();
        for (String childModule : nestedModules) {
            processModuleRecursive(childModule, childParentDir, context);
        }
    }
}

