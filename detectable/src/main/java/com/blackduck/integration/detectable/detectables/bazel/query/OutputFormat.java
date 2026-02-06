package com.blackduck.integration.detectable.detectables.bazel.query;

/**
 * Enum representing Bazel query output formats.
 * Used with the --output flag in Bazel query/cquery commands.
 */
public enum OutputFormat {
    /**
     * Build file format - shows the full BUILD rule definition
     */
    BUILD("build"),

    /**
     * XML format - structured XML output
     */
    XML("xml"),

    /**
     * JSON proto format - protobuf in JSON format
     */
    JSONPROTO("jsonproto"),

    /**
     * Label kind format - shows rule kinds with labels
     */
    LABEL_KIND("label_kind");

    private final String formatString;

    OutputFormat(String formatString) {
        this.formatString = formatString;
    }

    /**
     * Returns the format string to use in Bazel commands.
     * @return Format string for --output flag
     */
    public String getFormatString() {
        return formatString;
    }

    @Override
    public String toString() {
        return formatString;
    }
}

