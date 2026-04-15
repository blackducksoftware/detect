package com.blackduck.integration.detectable.detectables.bazel.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builder for constructing Bazel mod commands.
 * Provides a fluent API for building module-related commands.
 *
 * <p>Example usage:
 * <pre>
 * // Show repository info
 * List&lt;String&gt; args = ModCommandBuilder.create()
 *     .showRepo("my_repo", false)
 *     .build();
 *
 * // Get module graph
 * List&lt;String&gt; args = ModCommandBuilder.create()
 *     .graph()
 *     .build();
 * </pre>
 */
public class ModCommandBuilder {
    private final List<String> args;
    private String subcommand;
    private final List<String> subcommandArgs;

    /**
     * Private constructor - use create() factory method
     */
    private ModCommandBuilder() {
        this.args = new ArrayList<>();
        this.args.add(BazelCommandArguments.MOD);
        this.subcommandArgs = new ArrayList<>();
    }

    /**
     * Creates a new ModCommandBuilder instance.
     * @return New ModCommandBuilder
     */
    public static ModCommandBuilder create() {
        return new ModCommandBuilder();
    }

    /**
     * Adds the show_repo subcommand to display repository information.
     *
     * @param repoName Name of the repository (without @ prefix)
     * @param canonical If true, uses @@ prefix; if false, uses @ prefix
     * @return This builder for chaining
     */
    public ModCommandBuilder showRepo(String repoName, boolean canonical) {
        validateNotNull(repoName, "repoName");

        this.subcommand = BazelCommandArguments.MOD_SHOW_REPO;

        // Construct the repository reference with appropriate prefix
        String prefix = canonical ?
            BazelCommandArguments.REPO_PREFIX_CANONICAL :
            BazelCommandArguments.REPO_PREFIX_SINGLE;

        // Remove any existing @ prefixes from the repo name
        String cleanRepoName = repoName;
        while (cleanRepoName.startsWith("@")) {
            cleanRepoName = cleanRepoName.substring(1);
        }

        this.subcommandArgs.add(prefix + cleanRepoName);
        return this;
    }

    /**
     * Adds the show_repo subcommand with a pre-formatted repository reference.
     * Use this when you already have a properly formatted repo string (e.g., "@repo" or "@@repo").
     *
     * @param repoReference Complete repository reference including @ prefix
     * @return This builder for chaining
     */
    public ModCommandBuilder showRepoRaw(String repoReference) {
        validateNotNull(repoReference, "repoReference");

        this.subcommand = BazelCommandArguments.MOD_SHOW_REPO;
        this.subcommandArgs.add(repoReference);
        return this;
    }

    /**
     * Adds the show_repo subcommand with multiple pre-formatted repository references.
     * Bazel 6.x+ supports multiple repos in a single show_repo call:
     * {@code bazel mod show_repo @repo1 @repo2 @repo3}
     *
     * @param repoReferences List of complete repository references including @ prefix
     * @return This builder for chaining
     */
    public ModCommandBuilder showRepoRawBatch(List<String> repoReferences) {
        validateNotNull(repoReferences, "repoReferences");

        this.subcommand = BazelCommandArguments.MOD_SHOW_REPO;
        this.subcommandArgs.addAll(repoReferences);
        return this;
    }

    /**
     * Adds the graph subcommand to display the module dependency graph.
     *
     * @return This builder for chaining
     */
    public ModCommandBuilder graph() {
        this.subcommand = BazelCommandArguments.MOD_GRAPH;
        return this;
    }

    /**
     * Adds --output json to the command. Typically used with graph() for structured JSON output.
     * Requires Bazel 7.1+.
     *
     * @return This builder for chaining
     */
    public ModCommandBuilder withOutputJson() {
        this.subcommandArgs.add(BazelCommandArguments.OUTPUT_FLAG);
        this.subcommandArgs.add(BazelCommandArguments.MOD_OUTPUT_JSON);
        return this;
    }

    /**
     * Builds the complete Bazel mod command arguments.
     *
     * @return Immutable list of command arguments
     * @throws IllegalStateException if no subcommand was set
     */
    public List<String> build() {
        if (subcommand == null || subcommand.isEmpty()) {
            throw new IllegalStateException("Mod subcommand must be set before building");
        }

        List<String> result = new ArrayList<>(args);
        result.add(subcommand);
        result.addAll(subcommandArgs);

        return Collections.unmodifiableList(result);
    }

    /**
     * Validates that a parameter is not null.
     */
    private static void validateNotNull(Object value, String paramName) {
        if (value == null) {
            throw new IllegalArgumentException(paramName + " cannot be null");
        }
    }
}

