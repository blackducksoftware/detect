package com.blackduck.integration.detectable.detectables.maven.resolver;

/**
 * Utility class for formatting Maven coordinates and strings for various purposes.
 *
 * <p>Provides consistent formatting for:
 * <ul>
 *   <li>GAV (GroupId:ArtifactId:Version) coordinates</li>
 *   <li>Filename-safe strings</li>
 * </ul>
 */
class MavenCoordinateFormatter {

    private static final String UNKNOWN_GAV = "unknown:unknown:unknown";
    private static final String UNKNOWN_NAME = "unknown";

    /**
     * Formats a Maven project's coordinates as a GAV (GroupId:ArtifactId:Version) string.
     *
     * <p>This method safely extracts coordinates even if some fields are null or the
     * coordinates object itself is malformed.
     *
     * @param project The Maven project to extract coordinates from
     * @return GAV string in the format "groupId:artifactId:version", or "unknown:unknown:unknown" if extraction fails
     */
    public String formatGAV(MavenProject project) {
        try {
            return project.getCoordinates().getGroupId() + ":" +
                   project.getCoordinates().getArtifactId() + ":" +
                   project.getCoordinates().getVersion();
        } catch (Exception e) {
            return UNKNOWN_GAV;
        }
    }

    /**
     * Formats explicit Maven coordinates as a GAV string.
     *
     * @param groupId Maven group ID
     * @param artifactId Maven artifact ID
     * @param version Maven version
     * @return GAV string in the format "groupId:artifactId:version"
     */
    public String formatGAV(String groupId, String artifactId, String version) {
        if (groupId == null || artifactId == null || version == null) {
            return UNKNOWN_GAV;
        }
        return groupId + ":" + artifactId + ":" + version;
    }

    /**
     * Sanitizes a string for safe use in filenames by replacing problematic characters.
     *
     * <p>Replaces forward slashes, backslashes, and colons with underscores to ensure
     * the resulting string can be used in file paths on all operating systems.
     *
     * @param s The string to sanitize
     * @return Sanitized string safe for use in filenames, or "unknown" if input is null
     */
    public String toSafeFilename(String s) {
        if (s == null) {
            return UNKNOWN_NAME;
        }
        // Replace characters that are problematic in filenames across different operating systems
        return s.replace('/', '_').replace('\\', '_').replace(':', '_');
    }
}

