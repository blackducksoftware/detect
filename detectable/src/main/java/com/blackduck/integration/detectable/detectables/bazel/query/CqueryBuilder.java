package com.blackduck.integration.detectable.detectables.bazel.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builder for constructing Bazel cquery (configured query) commands.
 * Provides a fluent API for building cquery expressions with proper syntax.
 *
 * <p>Example usage:
 * <pre>
 * List&lt;String&gt; args = CqueryBuilder.create()
 *     .kind("j.*import", deps("//:target"))
 *     .withNoImplicitDeps()
 *     .withOutput(OutputFormat.BUILD)
 *     .build();
 * </pre>
 */
public class CqueryBuilder {
    private final List<String> args;
    private final List<String> flags;
    private String queryExpression;
    private OutputFormat outputFormat;
    private String optionsPlaceholder;

    // Format for functions with two arguments, e.g. kind(pattern, expression)
    private static final String FUNCTION_FORMAT_TWO_ARGS = "%s(%s, %s)";
    // Format for functions with a single argument, e.g. deps(target)
    private static final String FUNCTION_FORMAT_SINGLE_ARG = "%s(%s)";
    private static final String QUERY_EXPRESSION_MUST_BE_SET = "Query expression must be set before building";
    private static final String PARAM_RULE_PATTERN = "rulePattern";
    private static final String PARAM_EXPRESSION = "expression";
    private static final String PARAM_PATTERN = "pattern";
    private static final String PARAM_PLACEHOLDER = "placeholder";
    private static final String PARAM_TARGET = "target";
    private static final String PARAM_FORMAT = "format";

    /**
     * Private constructor - use create() factory method
     */
    private CqueryBuilder() {
        this.args = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.args.add(BazelCommandArguments.CQUERY);
    }

    /**
     * Creates a new CqueryBuilder instance.
     * @return New CqueryBuilder
     */
    public static CqueryBuilder create() {
        return new CqueryBuilder();
    }

    /**
     * Adds a kind() query function.
     * Filters rules by type pattern.
     *
     * @param rulePattern Pattern to match rule types (e.g., "j.*import", "haskell_cabal_library")
     * @param expression Inner expression (e.g., result of deps())
     * @return This builder for chaining
     */
    public CqueryBuilder kind(String rulePattern, String expression) {
        validateNotNull(rulePattern, PARAM_RULE_PATTERN);
        validateNotNull(expression, PARAM_EXPRESSION);
        this.queryExpression = String.format(FUNCTION_FORMAT_TWO_ARGS,
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
    public CqueryBuilder filter(String pattern, String expression) {
        validateNotNull(pattern, PARAM_PATTERN);
        validateNotNull(expression, PARAM_EXPRESSION);
        this.queryExpression = String.format(FUNCTION_FORMAT_TWO_ARGS,
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
    public CqueryBuilder expression(String expression) {
        validateNotNull(expression, PARAM_EXPRESSION);
        this.queryExpression = expression;
        return this;
    }

    /**
     * Adds the --noimplicit_deps flag to exclude implicit dependencies.
     *
     * @return This builder for chaining
     */
    public CqueryBuilder withNoImplicitDeps() {
        this.flags.add(BazelCommandArguments.NO_IMPLICIT_DEPS);
        return this;
    }

    /**
     * Adds a cquery options placeholder for variable substitution.
     * This is used with BazelVariableSubstitutor for dynamic option injection.
     *
     * @param placeholder Placeholder string (e.g., "${detect.bazel.cquery.options}")
     * @return This builder for chaining
     */
    public CqueryBuilder withOptions(String placeholder) {
        validateNotNull(placeholder, PARAM_PLACEHOLDER);
        this.optionsPlaceholder = placeholder;
        return this;
    }

    /**
     * Specifies the output format for the query results.
     *
     * @param format Output format (BUILD, XML, JSONPROTO, LABEL_KIND)
     * @return This builder for chaining
     */
    public CqueryBuilder withOutput(OutputFormat format) {
        validateNotNull(format, PARAM_FORMAT);
        this.outputFormat = format;
        return this;
    }

    /**
     * Builds the complete Bazel cquery command arguments.
     * Arguments are ordered as: cquery [flags] [options] expression [--output format]
     *
     * @return Immutable list of command arguments
     * @throws IllegalStateException if no query expression was set
     */
    public List<String> build() {
        if (queryExpression == null || queryExpression.isEmpty()) {
            throw new IllegalStateException(QUERY_EXPRESSION_MUST_BE_SET);
        }

        List<String> result = new ArrayList<>(args);

        // Add flags first (like --noimplicit_deps)
        result.addAll(flags);

        // Add options placeholder if present
        if (optionsPlaceholder != null) {
            result.add(optionsPlaceholder);
        }

        // Add the query expression
        result.add(queryExpression);

        // Add output format last
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
     * @param target Target to get dependencies for (e.g., "//:test" or "${detect.bazel.target}")
     * @return deps() expression string
     */
    public static String deps(String target) {
        validateNotNull(target, PARAM_TARGET);
        return String.format(FUNCTION_FORMAT_SINGLE_ARG, BazelCommandArguments.DEPS_FUNCTION, target);
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
