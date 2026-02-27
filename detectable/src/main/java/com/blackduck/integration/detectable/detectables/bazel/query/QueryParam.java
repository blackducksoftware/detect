package com.blackduck.integration.detectable.detectables.bazel.query;

/**
 * Enum to centralize common parameter names used by query builder classes.
 * Also contains a few shared string constants used across query builders.
 */
public enum QueryParam {
    RULE_PATTERN("rulePattern"),
    EXPRESSION("expression"),
    PATTERN("pattern"),
    PLACEHOLDER("placeholder"),
    TARGET("target"),
    FORMAT("format");

    private final String name;

    QueryParam(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    // Shared format strings and messages used by query builders
    public static final String FUNCTION_FORMAT_TWO_ARGS = "%s(%s, %s)";
    public static final String FUNCTION_FORMAT_SINGLE_ARG = "%s(%s)";
    public static final String QUERY_EXPRESSION_MUST_BE_SET = "Query expression must be set before building";
}
