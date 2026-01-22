package com.blackduck.integration.detectable.detectables.maven.resolver;

import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;

/**
 * Utility class for writing Maven dependency trees to files for debugging and inspection.
 *
 * <p>This class handles the file I/O for dependency tree output, creating both
 * successful resolution trees and error marker files for failed resolutions.
 *
 * <p><strong>File Naming Strategy:</strong>
 * <ul>
 *   <li>Success: {@code dependency-tree-{scope}-{gav}-{hash}.txt}</li>
 *   <li>Error: {@code dependency-tree-{scope}-{tag}-{hash}-ERROR.txt}</li>
 * </ul>
 *
 * <p>The hash suffix prevents filename collisions when multiple modules have similar GAV coordinates.
 */
class DependencyTreeFileWriter {

    private static final Logger logger = LoggerFactory.getLogger(DependencyTreeFileWriter.class);

    // File naming constants
    private static final String DEPENDENCY_TREE_FILE_PREFIX = "dependency-tree-";
    private static final String TREE_FILE_EXTENSION = ".txt";
    private static final String ERROR_FILE_SUFFIX = "-ERROR";

    /**
     * Writes a dependency tree to a file for a given scope.
     *
     * <p>The tree is written in Maven's standard text format using Aether's DependencyGraphDumper.
     *
     * @param collectResult The Aether collection result containing the dependency tree
     * @param outputFile The file to write the dependency tree to
     * @param scope The Maven scope (e.g., "compile", "test") for logging purposes
     * @throws Exception if file writing fails
     */
    public void writeDependencyTree(CollectResult collectResult, File outputFile, String scope) throws Exception {
        try (PrintStream printStream = new PrintStream(outputFile)) {
            collectResult.getRoot().accept(new DependencyGraphDumper(printStream::println));
        }
        logger.info("{} dependency tree saved to: {}", capitalize(scope), outputFile.getAbsolutePath());
    }

    /**
     * Writes an error marker file when dependency resolution fails.
     *
     * <p>Error files capture diagnostic information including the scope, module coordinates,
     * file path, and failure reason. This is a best-effort operation - if writing fails,
     * the exception is silently ignored to prevent cascading failures.
     *
     * @param outputDir Directory where the error file should be written
     * @param scope The Maven scope being resolved when the error occurred
     * @param gavOrTag GAV coordinates or error tag identifying the module
     * @param modulePathKey The canonical or absolute path to the module's POM file
     * @param message The error message describing why resolution failed
     */
    public void writeErrorTreeFile(File outputDir, String scope, String gavOrTag, String modulePathKey, String message) {
        try {
            String hash = Integer.toHexString(modulePathKey == null ? 0 : modulePathKey.hashCode());
            String fileName = buildErrorFileName(scope, gavOrTag, hash);
            File errorFile = new File(outputDir, fileName);

            try (PrintStream ps = new PrintStream(errorFile)) {
                ps.println("Dependency collection failed for scope '" + scope + "'.");
                ps.println("Module: " + gavOrTag);
                ps.println("Path: " + modulePathKey);
                ps.println("Reason: " + message);
            }

            logger.info("Module {} dependency tree error file saved to: {}", scope, errorFile.getAbsolutePath());
        } catch (Exception ignored) {
            // Best-effort only - don't let error file writing failure cause additional problems
        }
    }

    /**
     * Builds the filename for a dependency tree file.
     *
     * @param scope The Maven scope
     * @param gavOrTag The GAV coordinates or tag
     * @param hash Unique hash to prevent collisions
     * @return Complete filename string
     */
    public String buildDependencyTreeFileName(String scope, String gavOrTag, String hash) {
        return DEPENDENCY_TREE_FILE_PREFIX + scope + "-" + sanitizeForFilename(gavOrTag) + "-" + hash + TREE_FILE_EXTENSION;
    }

    /**
     * Builds the filename for an error marker file.
     *
     * @param scope The Maven scope
     * @param gavOrTag The GAV coordinates or tag
     * @param hash Unique hash to prevent collisions
     * @return Complete filename string
     */
    private String buildErrorFileName(String scope, String gavOrTag, String hash) {
        return DEPENDENCY_TREE_FILE_PREFIX + scope + "-" + sanitizeForFilename(gavOrTag) + "-" + hash + ERROR_FILE_SUFFIX + TREE_FILE_EXTENSION;
    }

    /**
     * Sanitizes a string for safe use in filenames.
     *
     * <p>Replaces forward slashes, backslashes, and colons with underscores.
     *
     * @param s The string to sanitize
     * @return Sanitized string, or "unknown" if input is null
     */
    private String sanitizeForFilename(String s) {
        if (s == null) {
            return "unknown";
        }
        return s.replace('/', '_').replace('\\', '_').replace(':', '_');
    }

    /**
     * Capitalizes the first letter of a string for logging.
     *
     * @param s The string to capitalize
     * @return Capitalized string
     */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

