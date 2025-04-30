package com.blackduck.integration.detectable.detectables.cargo;

public class VersionUtils {
    public static int compareVersions(String version1, String version2) {
        String[] version1Parts = version1.split("\\.");
        String[] version2Parts = version2.split("\\.");

        for (int i = 0; i < Math.max(version1Parts.length, version2Parts.length); i++) {
            int v1 = i < version1Parts.length ? Integer.parseInt(version1Parts[i]) : 0;
            int v2 = i < version2Parts.length ? Integer.parseInt(version2Parts[i]) : 0;
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

        String normalizedActual = normalizeVersion(actualVersion);
        String normalizedConstraintVersion;

        if (constraint.startsWith(">=")) {
            normalizedConstraintVersion = normalizeVersion(constraint.substring(2));
            return compareVersions(normalizedActual, normalizedConstraintVersion) >= 0;
        } else if (constraint.startsWith(">")) {
            normalizedConstraintVersion = normalizeVersion(constraint.substring(1));
            return compareVersions(normalizedActual, normalizedConstraintVersion) > 0;
        } else if (constraint.startsWith("<=")) {
            normalizedConstraintVersion = normalizeVersion(constraint.substring(2));
            return compareVersions(normalizedActual, normalizedConstraintVersion) <= 0;
        } else if (constraint.startsWith("<")) {
            normalizedConstraintVersion = normalizeVersion(constraint.substring(1));
            return compareVersions(normalizedActual, normalizedConstraintVersion) < 0;
        } else if (constraint.startsWith("=")) {
            normalizedConstraintVersion = normalizeVersion(constraint.substring(1));
            return compareVersions(normalizedActual, normalizedConstraintVersion) == 0;
        } else {
            // Default to exact match
            normalizedConstraintVersion = normalizeVersion(constraint);
            return compareVersions(normalizedActual, normalizedConstraintVersion) == 0;
        }
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
}