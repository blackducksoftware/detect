package com.blackduck.integration.detectable.detectables.bazel.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builder for constructing Bazel query commands.
 * Provides a fluent API for building query expressions with proper syntax.
 *
 * <p>Example usage:
 * <pre>
 * List&lt;String&gt; args = QueryBuilder.create()
 *     .kind(".*library", deps("//:target"))
 *     .withOutput(OutputFormat.XML)
 *     .build();
 * </pre>
 */
public class QueryBuilder {
    private final List<String> args;
    private String queryExpression;
    private OutputFormat outputFormat;

    private static final String FUNCTION_FORMAT_TWO = "%s(%s, %s)";
    private static final String FUNCTION_FORMAT_ONE = "%s(%s)";
    private static final String QUERY_EXPRESSION_MUST_BE_SET = "Query expression must be set before building";
    private static final String PARAM_RULE_PATTERN = "rulePattern";
    private static final String PARAM_EXPRESSION = "expression";
    private static final String PARAM_PATTERN = "pattern";
    private static final String PARAM_OPTIONS = "options";
    private static final String PARAM_TARGET = "target";
    private static final String PARAM_FORMAT = "format";

    /**
     * Private constructor - use create() factory method
     */
    private QueryBuilder() {
        this.args = new ArrayList<>();
        this.args.add(BazelCommandArguments.QUERY);
    }

    /**
     * Creates a new QueryBuilder instance.
     * @return New QueryBuilder
     */
    public static QueryBuilder create() {
        return new QueryBuilder();
    }

    /**
     * Adds a kind() query function.
     * Filters rules by type pattern.
     *
     * @param rulePattern Pattern to match rule types (e.g., "j.*import", ".*library")
     * @param expression Inner expression (e.g., result of deps())
     * @return This builder for chaining
     */
    public QueryBuilder kind(String rulePattern, String expression) {
        validateNotNull(rulePattern, PARAM_RULE_PATTERN);
        validateNotNull(expression, PARAM_EXPRESSION);
        this.queryExpression = String.format(FUNCTION_FORMAT_TWO,
            BazelCommandArguments.KIND_FUNCTION, rulePattern, expression);
        return this;
    }

    /**
     * Adds a filter() query function.
     * Filters labels by pattern.
     *
     * @param pattern Pattern to match labels (e.g., "'@.*:jar'")
     * @param expression Inner expression (e.g., result of deps())
     * @return This builder for chaining
     */
    public QueryBuilder filter(String pattern, String expression) {
        validateNotNull(pattern, PARAM_PATTERN);
        validateNotNull(expression, PARAM_EXPRESSION);
        this.queryExpression = String.format(FUNCTION_FORMAT_TWO,
            BazelCommandArguments.FILTER_FUNCTION, pattern, expression);
        return this;
    }

    /**
     * Sets a custom query expression directly.
     * Use this for complex queries that don't fit the builder pattern.
     *
     * @param expression The complete query expression
     * @return This builder for chaining
     */
    public QueryBuilder expression(String expression) {
        validateNotNull(expression, PARAM_EXPRESSION);
        this.queryExpression = expression;
        return this;
    }

    /**
     * Adds options to the query command, such as placeholders to be substituted
     * (e.g., "${detect.bazel.query.options}") or explicit flags (e.g., "--enable_bzlmod").
     * The provided string is added as a single argument if non-empty.
     *
     * @param options Arbitrary options string to append
     * @return This builder for chaining
     */
    public QueryBuilder withOptions(String options) {
        if (options != null && !options.trim().isEmpty()) {
            this.args.add(options);
        }
        return this;
    }

    /**
     * Specifies the output format for the query results.
     *
     * @param format Output format (BUILD, XML, JSONPROTO, LABEL_KIND)
     * @return This builder for chaining
     */
    public QueryBuilder withOutput(OutputFormat format) {
        validateNotNull(format, PARAM_FORMAT);
        this.outputFormat = format;
        return this;
    }

    /**
     * Builds the complete Bazel query command arguments.
     *
     * @return Immutable list of command arguments
     * @throws IllegalStateException if no query expression was set
     */
    public List<String> build() {
        if (queryExpression == null || queryExpression.isEmpty()) {
            throw new IllegalStateException(QUERY_EXPRESSION_MUST_BE_SET);
        }

        List<String> result = new ArrayList<>(args);
        result.add(queryExpression);

        if (outputFormat != null) {
            result.add(BazelCommandArguments.OUTPUT_FLAG);
            result.add(outputFormat.getFormatString());
        }

        return Collections.unmodifiableList(result);
    }

    // ===== Helper Methods =====

    /**
     * Creates a deps() function expression.
     *
     * @param target Target to get dependencies for (e.g., "//:test")
     * @return deps() expression string
     */
    public static String deps(String target) {
        validateNotNull(target, PARAM_TARGET);
        return String.format(FUNCTION_FORMAT_ONE, BazelCommandArguments.DEPS_FUNCTION, target);
    }

    /**
     * Validates that a parameter is not null.
     */
    private static void validateNotNull(Object value, String paramName) {
        if (value == null) {
            throw new IllegalArgumentException(paramName + " cannot be null");
        }
    }

    /**
     * Validates that a string parameter is not null or empty.
     */
    private void validateNotEmpty(String value, String paramName) {
        validateNotNull(value, paramName);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be empty");
        }
    }
}
