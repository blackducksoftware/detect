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

        if ("*".equals(constraint.trim())) {
            return true;
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
            return versionCompatible(constraint, actualVersion);
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

    public static boolean versionCompatible(String declaredVersion, String actualVersion) {
        if (declaredVersion == null || actualVersion == null) {
            return false;
        }

        String[] declaredParts = declaredVersion.split("\\.");
        String[] actualParts = actualVersion.split("\\.");

        if (declaredParts.length == 1) {
            // Matching major version only (e.g., "2" matches "2.x.x")
            return actualParts[0].equals(declaredParts[0]);
        } else if (declaredParts.length == 2) {
            // Matching major and minor versions (e.g., "0.8" matches "0.8.x")
            return actualParts[0].equals(declaredParts[0]) && actualParts[1].equals(declaredParts[1]);
        }


        // Fill both arrays to length 3 with "0" if needed
        String[] normalizedDeclared = new String[] {
            declaredParts.length > 0 ? declaredParts[0] : "0",
            declaredParts.length > 1 ? declaredParts[1] : "0",
            declaredParts.length > 2 ? declaredParts[2] : "0"
        };
        String[] normalizedActual = new String[] {
            actualParts.length > 0 ? actualParts[0] : "0",
            actualParts.length > 1 ? actualParts[1] : "0",
            actualParts.length > 2 ? actualParts[2] : "0"
        };

        int declaredMajor = Integer.parseInt(normalizedDeclared[0]);
        int declaredMinor = Integer.parseInt(normalizedDeclared[1]);
        int declaredPatch = Integer.parseInt(normalizedDeclared[2]);

        int actualMajor = Integer.parseInt(normalizedActual[0]);
        int actualMinor = Integer.parseInt(normalizedActual[1]);
        int actualPatch = Integer.parseInt(normalizedActual[2]);

        // Cargo behavior:
        // - if 0.x.y: treat minor as the compatibility boundary
        // - if >=1.0.0: treat major as the compatibility boundary
        if (declaredMajor == 0) {
            return actualMajor == 0 &&
                actualMinor == declaredMinor &&
                actualPatch >= declaredPatch;
        } else {
            return actualMajor == declaredMajor &&
                (actualMinor > declaredMinor ||
                    (actualMinor == declaredMinor && actualPatch >= declaredPatch));
        }
    }

    public static String stripBuildMetadata(String version) {
        // Remove anything after and including '+'
        return version.split("\\+")[0];
    }
}