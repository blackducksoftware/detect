package com.blackduck.integration.detectable.detectables.bazel.query;

/**
 * Constants for Bazel command arguments, flags, and functions.
 * Centralizes all Bazel command-line syntax in one place.
 */
public final class BazelCommandArguments {

    // Private constructor to prevent instantiation
    private BazelCommandArguments() {
        throw new IllegalStateException("Utility class - do not instantiate");
    }

    // ===== Commands =====

    /**
     * Bazel query command - queries the build graph
     */
    public static final String QUERY = "query";

    /**
     * Bazel cquery command - configured query for analyzing build configurations
     */
    public static final String CQUERY = "cquery";

    /**
     * Bazel mod command - module-related commands
     */
    public static final String MOD = "mod";

    // ===== Common Flags =====

    /**
     * Flag to exclude implicit dependencies from query results
     */
    public static final String NO_IMPLICIT_DEPS = "--noimplicit_deps";

    /**
     * Output format flag
     */
    public static final String OUTPUT_FLAG = "--output";

    // ===== Query Functions =====

    /**
     * kind() function - filters rules by type
     */
    public static final String KIND_FUNCTION = "kind";

    /**
     * deps() function - returns dependencies of a target
     */
    public static final String DEPS_FUNCTION = "deps";

    /**
     * filter() function - filters labels by pattern
     */
    public static final String FILTER_FUNCTION = "filter";

    // ===== Mod Subcommands =====

    /**
     * show_repo subcommand - shows repository information
     */
    public static final String MOD_SHOW_REPO = "show_repo";

    /**
     * graph subcommand - shows module dependency graph
     */
    public static final String MOD_GRAPH = "graph";

    // ===== Repository Prefixes =====

    /**
     * Single @ prefix for repository references
     */
    public static final String REPO_PREFIX_SINGLE = "@";

    /**
     * Double @@ prefix for canonical repository references
     */
    public static final String REPO_PREFIX_CANONICAL = "@@";
}

