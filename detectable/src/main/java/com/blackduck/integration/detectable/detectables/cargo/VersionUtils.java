package com.blackduck.integration.detectable.detectables.cargo;

public class VersionUtils {

    enum CargoOperator {
        CARET("^", "Major range operator, example: ^1.2.3 is equivalent to >=1.2.3, <2.0.0"),
        TILDE("~", "Minor range operator, example: ~1.2.3 is equivalent to >=1.2.3, <1.3.0"),
        GREATER_THAN_OR_EQUAL(">=", "Greater than or equal to operator"),
        LESS_THAN_OR_EQUAL("<=", "Less than or equal to operator"),
        EQUAL("=", "Equal operator"),
        GREATER_THAN(">", "Greater than operator"),
        LESS_THAN("<", "Less than operator");

        private final String symbol;
        private final String description;

        CargoOperator(String symbol, String description) {
            this.symbol = symbol;
            this.description = description;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getDescription() {
            return description;
        }
    }

    public static int compareVersions(String version1, String version2) {
        String[] v1Parts = sanitizeVersion(version1).split("\\.");
        String[] v2Parts = sanitizeVersion(version2).split("\\.");

        for (int i = 0; i < Math.max(v1Parts.length, v2Parts.length); i++) {
            int v1 = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2 = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
            if (v1 != v2) {
                return Integer.compare(v1, v2);
            }
        }
        return 0;
    }

    public static boolean versionMatches(String constraint, String actualVersion) {
        if (constraint == null || actualVersion == null) {
            return false;
        }

        String trimmed = constraint.trim();
        if ("*".equals(trimmed)) {
            return true;
        }

        CargoOperator operator = CargoOperator.CARET;
        String version = trimmed;

        for (CargoOperator op : CargoOperator.values()) {
            if (trimmed.startsWith(op.symbol)) {
                operator = op;
                version = trimmed.substring(op.symbol.length()).trim();
                break;
            }
        }

        String normalizedConstraint = normalizeVersion(version);
        String normalizedActual = normalizeVersion(actualVersion);

        switch (operator) {
            case GREATER_THAN_OR_EQUAL:
                return compareVersions(normalizedActual, normalizedConstraint) >= 0;
            case GREATER_THAN:
                return compareVersions(normalizedActual, normalizedConstraint) > 0;
            case LESS_THAN_OR_EQUAL:
                return compareVersions(normalizedActual, normalizedConstraint) <= 0;
            case LESS_THAN:
                return compareVersions(normalizedActual, normalizedConstraint) < 0;
            case EQUAL:
                return compareVersions(normalizedActual, normalizedConstraint) == 0;
            case CARET:
                return matchesCaretRange(version, actualVersion);
            case TILDE:
                return matchesTildeRange(version, actualVersion);
            default:
                return false;
        }
    }

    /**
     * Caret (^) allows changes that do not modify the left-most non-zero digit.
     * ^1.2.3 means >=1.2.3 <2.0.0
     * ^0.2.3 means >=0.2.3 <0.3.0
     */
    private static boolean matchesCaretRange(String constraintVersion, String actualVersion) {
        String upperBound = constraintVersion.startsWith("0.")
            ? nextMinorVersion(constraintVersion)
            : nextMajorVersion(constraintVersion);
        return compareVersions(normalizeVersion(actualVersion), normalizeVersion(constraintVersion)) >= 0
            && compareVersions(normalizeVersion(actualVersion), normalizeVersion(upperBound)) < 0;
    }

    /**
     * Tilde (~) allows patch-level changes.
     * ~1.2.3 means >=1.2.3 <1.3.0
     * ~1.2   means >=1.2.0 <1.3.0
     */
    private static boolean matchesTildeRange(String constraintVersion, String actualVersion) {
        String upperBound = nextMinorVersion(constraintVersion);
        return compareVersions(normalizeVersion(actualVersion), normalizeVersion(constraintVersion)) >= 0
            && compareVersions(normalizeVersion(actualVersion), normalizeVersion(upperBound)) < 0;
    }

    private static String nextMajorVersion(String version) {
        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]);
        StringBuilder sb = new StringBuilder();
        sb.append(major + 1);
        for (int i = 1; i < Math.max(parts.length, 3); i++) {
            sb.append(".0");
        }
        return sb.toString();
    }

    private static String nextMinorVersion(String version) {
        String[] parts = version.split("\\.");
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        StringBuilder sb = new StringBuilder();
        sb.append(parts[0]);
        sb.append(".").append(minor + 1);
        for (int i = 2; i < Math.max(parts.length, 3); i++) {
            sb.append(".0");
        }
        return sb.toString();
    }

    private static String normalizeVersion(String version) {
        String[] parts = version.split("\\.");
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            if (i < parts.length) {
                normalized.append(parts[i]);
            } else {
                normalized.append("0");
            }
            if (i < 2) {
                normalized.append(".");
            }
        }
        return normalized.toString();
    }

    /**
     * Strips pre-release tags (e.g., "-alpha") and build metadata (e.g., "+build")
     * from a version string, returning only the major.minor.patch portion.
     */
    public static String sanitizeVersion(String version) {
        if (version == null) {
            return null;
        }
        // Strip build metadata first (+build), then pre-release tag (-alpha)
        return version.split("\\+")[0].split("-")[0];
    }
}
