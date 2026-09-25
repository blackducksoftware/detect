package com.blackduck.integration.detect.lifecycle.boot.product.version;

/**
 * Static helpers for parsing and comparing the version strings used in the Detect &harr; Black Duck
 * SCA compatibility matrix. Handles both exact ("X.Y.Z") entries and wildcarded ("X.Y.x")
 * entries; a wildcard's patch is modeled as {@link Integer#MAX_VALUE} so it sorts as "any patch
 * of major.minor".
 */
final class CompatibilityVersions {

    private CompatibilityVersions() {
    }

    /**
     * Parse a version string of the form "X.Y.Z" or "X.Y.x" into a three-element
     * {@code [major, minor, patch]} array. Returns {@code null} when the input is null,
     * has fewer than two dot-separated numeric parts, or fails to parse.
     */
    static int[] parse(String version) {
        if (version == null) {
            return null;
        }
        String[] parts = version.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch;
            if (parts.length >= 3) {
                patch = "x".equalsIgnoreCase(parts[2])
                    ? Integer.MAX_VALUE
                    : Integer.parseInt(parts[2]);
            } else {
                patch = 0;
            }
            return new int[]{ major, minor, patch };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Compare two parsed versions lexicographically by (major, minor, patch). */
    static int compare(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) {
            int cmp = Integer.compare(a[i], b[i]);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }
}
