package com.blackduck.integration.detectable.detectables.bazel.query;

/**
 * Entry point for constructing Bazel query commands using a fluent API.
 * Provides factory methods for creating query, cquery, and mod command builders.
 *
 * <p>This class centralizes all Bazel query construction in a single, well-organized API.
 * It improves code readability, maintainability, and testability by replacing inline
 * string formatting with type-safe builder methods.
 *
 * <h2>Usage Examples:</h2>
 *
 * <h3>Query Command (bazel query)</h3>
 * <pre>
 * // Query all library dependencies
 * List&lt;String&gt; args = BazelQueryBuilder.query()
 *     .kind(".*library", QueryBuilder.deps("//:target"))
 *     .build();
 * // Results in: ["query", "kind(.*library, deps(//:target))"]
 *
 * // Query with XML output
 * List&lt;String&gt; args = BazelQueryBuilder.query()
 *     .kind("maven_jar", "${input.item}")
 *     .withOutput(OutputFormat.XML)
 *     .build();
 * // Results in: ["query", "kind(maven_jar, ${input.item})", "--output", "xml"]
 * </pre>
 *
 * <h3>Cquery Command (bazel cquery)</h3>
 * <pre>
 * // Query Java imports with build output
 * List&lt;String&gt; args = BazelQueryBuilder.cquery()
 *     .kind("j.*import", CqueryBuilder.deps("//:target"))
 *     .withNoImplicitDeps()
 *     .withOutput(OutputFormat.BUILD)
 *     .build();
 * // Results in: ["cquery", "--noimplicit_deps", "kind(j.*import, deps(//:target))", "--output", "build"]
 *
 * // Filter query with options placeholder
 * List&lt;String&gt; args = BazelQueryBuilder.cquery()
 *     .filter("'@.*:jar'", CqueryBuilder.deps("${detect.bazel.target}"))
 *     .withOptions("${detect.bazel.cquery.options}")
 *     .build();
 * // Results in: ["cquery", "${detect.bazel.cquery.options}", "filter('@.*:jar', deps(${detect.bazel.target}))"]
 * </pre>
 *
 * <h3>Mod Command (bazel mod)</h3>
 * <pre>
 * // Show repository with single @ prefix
 * List&lt;String&gt; args = BazelQueryBuilder.mod()
 *     .showRepo("my_repo", false)
 *     .build();
 * // Results in: ["mod", "show_repo", "@my_repo"]
 *
 * // Show repository with canonical @@ prefix
 * List&lt;String&gt; args = BazelQueryBuilder.mod()
 *     .showRepo("my_repo", true)
 *     .build();
 * // Results in: ["mod", "show_repo", "@@my_repo"]
 *
 * // Get module dependency graph
 * List&lt;String&gt; args = BazelQueryBuilder.mod()
 *     .graph()
 *     .build();
 * // Results in: ["mod", "graph"]
 * </pre>
 *
 * <h3>Helper Methods</h3>
 * <pre>
 * // Use static deps() helper for creating deps() expressions
 * String depsExpr = BazelQueryBuilder.deps("//:target");
 * // Results in: "deps(//:target)"
 *
 * // Works with placeholders too
 * String depsExpr = BazelQueryBuilder.deps("${detect.bazel.target}");
 * // Results in: "deps(${detect.bazel.target})"
 * </pre>
 *
 * @see QueryBuilder
 * @see CqueryBuilder
 * @see ModCommandBuilder
 * @see OutputFormat
 */
public final class BazelQueryBuilder {

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static factory methods.
     */
    private BazelQueryBuilder() {
        throw new IllegalStateException("Utility class - do not instantiate");
    }

    /**
     * Creates a new QueryBuilder for constructing Bazel query commands.
     *
     * @return A new QueryBuilder instance
     */
    public static QueryBuilder query() {
        return QueryBuilder.create();
    }

    /**
     * Creates a new CqueryBuilder for constructing Bazel cquery commands.
     *
     * @return A new CqueryBuilder instance
     */
    public static CqueryBuilder cquery() {
        return CqueryBuilder.create();
    }

    /**
     * Creates a new ModCommandBuilder for constructing Bazel mod commands.
     *
     * @return A new ModCommandBuilder instance
     */
    public static ModCommandBuilder mod() {
        return ModCommandBuilder.create();
    }

    /**
     * Helper method to create a deps() function expression.
     * This is a convenience method that can be used with any query builder.
     *
     * @param target Target to get dependencies for (e.g., "//:test" or "${detect.bazel.target}")
     * @return deps() expression string
     */
    public static String deps(String target) {
        return QueryBuilder.deps(target);
    }
}

