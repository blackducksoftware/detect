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

        int declaredMajor = declaredParts.length > 0 ? Integer.parseInt(declaredParts[0]) : 0;
        int declaredMinor = declaredParts.length > 1 ? Integer.parseInt(declaredParts[1]) : 0;
        int declaredPatch = declaredParts.length > 2 ? Integer.parseInt(declaredParts[2]) : 0;

        int actualMajor = actualParts.length > 0 ? Integer.parseInt(actualParts[0]) : 0;
        int actualMinor = actualParts.length > 1 ? Integer.parseInt(actualParts[1]) : 0;
        int actualPatch = actualParts.length > 2 ? Integer.parseInt(actualParts[2]) : 0;

        return (declaredMajor == 0)
            ? isCompatiblePreOne(declaredMinor, declaredPatch, actualMajor, actualMinor, actualPatch)
            : isCompatibleStable(declaredMajor, declaredMinor, declaredPatch, actualMajor, actualMinor, actualPatch);
    }

    private static boolean isCompatiblePreOne(int declaredMinor, int declaredPatch, int actualMajor, int actualMinor, int actualPatch) {
        if (declaredMinor == 0) {
            return actualMajor == 0 && actualMinor == 0 && actualPatch >= declaredPatch;
        }
        return actualMajor == 0 && actualMinor == declaredMinor && actualPatch >= declaredPatch;
    }

    private static boolean isCompatibleStable(int declaredMajor, int declaredMinor, int declaredPatch, int actualMajor, int actualMinor, int actualPatch) {
        if (actualMajor != declaredMajor) {
            return false;
        }
        if (actualMinor < declaredMinor) {
            return false;
        }
        return actualMinor > declaredMinor || actualPatch >= declaredPatch;
    }

    public static String stripBuildMetadata(String version) {
        if (version == null) {
            return null;
        }
        // Remove anything after and including '+'
        return version.split("\\+")[0];
    }
}