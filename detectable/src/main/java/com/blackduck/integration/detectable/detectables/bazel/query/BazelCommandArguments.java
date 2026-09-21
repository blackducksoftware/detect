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

    /**
     * JSON output format value for mod commands (e.g., mod graph --output json)
     */
    public static final String MOD_OUTPUT_JSON = "json";

    // ===== Repository Prefixes =====

    /**
     * Single @ prefix for repository references
     */
    public static final String REPO_PREFIX_SINGLE = "@";

    /**
     * Double @@ prefix for canonical repository references
     */
    public static final String REPO_PREFIX_CANONICAL = "@@";

    // ===== Canonical Repo-Name Suffixes (version-specific) =====

    /**
     * Canonical repo-name suffix used by Bazel 7.5+ (e.g. {@code @@protobuf~}).
     */
    public static final String REPO_CANONICAL_SUFFIX_TILDE = "~";

    /**
     * Canonical repo-name suffix used by Bazel 7.x (pre-7.5) and some 8.x builds (e.g. {@code @@protobuf+}).
     */
    public static final String REPO_CANONICAL_SUFFIX_PLUS = "+";

    /**
     * Regex matching any known trailing canonical suffix ({@code ~} or {@code +}) at end of a repo name.
     * Used as a best-effort strip when the exact suffix is unknown.
     */
    public static final String KNOWN_CANONICAL_SUFFIX_REGEX = "[+~]$";

    /**
     * Marker present in canonical names of module-extension sub-repos
     * (e.g. {@code rules_jvm_external++maven+guava}). These never appear as
     * {@code bazel mod graph} module keys and are excluded from BCR resolution.
     */
    public static final String MODULE_EXTENSION_MARKER = "++";

    /**
     * Separator between the repo-name part and the {@code //path:target} part of a Bazel label.
     */
    public static final String LABEL_PATH_SEPARATOR = "//";

    /**
     * Separator between the module name and version in a {@code bazel mod graph} module key
     * (e.g. the {@code @} in {@code protobuf@31.0}). Same character as {@link #REPO_PREFIX_SINGLE}
     * but semantically distinct — this one splits {@code name@version}, not a repo reference.
     */
    public static final String MODULE_KEY_SEPARATOR = "@";
}

