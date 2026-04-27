package com.blackduck.integration.detectable.detectables.maven.resolver.shadeinspection.model;

import java.util.Collections;
import java.util.Set;

/**
 * Parsed, ready-to-use summary of the maven-shade-plugin configuration extracted
 * from a dependency's embedded {@code pom.xml}.
 *
 * <p>This is NOT a Jackson-mapped model — it is a clean, pre-processed data holder
 * built by {@code ShadePluginConfigExtractor}
 * from the raw {@code PomXmlPlugin} XML model.
 *
 * <p>Two sets are extracted:
 * <ul>
 *   <li>{@code excludedGavPatterns} — artifacts explicitly excluded from the fat JAR via
 *       {@code <artifactSet><excludes>}. Patterns are in {@code groupId:artifactId} format
 *       and may contain {@code *} wildcards.</li>
 *   <li>{@code relocatedPackagePrefixes} — Java package prefixes that were relocated via
 *       {@code <relocations><relocation><pattern>}. Stored as slash-separated class path
 *       prefixes (e.g., {@code "com/google/guava/"}) for direct comparison against JAR entries.</li>
 * </ul>
 *
 * <h2>How the ghost filter uses this</h2>
 * <p>A dependency candidate (found via delta analysis or pom.properties scan) is a ghost
 * (not actually bundled) if and only if it is listed in {@code excludedGavPatterns}.
 * If it is NOT explicitly excluded, it WAS bundled — possibly with relocation — and should
 * be reported as a shaded dependency.
 */
public class ShadePluginConfig {

    /** Raw exclude patterns from {@code <artifactSet><excludes>}, e.g. {@code "com.google.guava:guava"}, {@code "org.apache.hadoop:*"} */
    private final Set<String> excludedGavPatterns;

    /** Class-path prefixes of relocated packages, e.g. {@code "com/google/guava/"} */
    private final Set<String> relocatedPackagePrefixes;

    public ShadePluginConfig(Set<String> excludedGavPatterns, Set<String> relocatedPackagePrefixes) {
        this.excludedGavPatterns = excludedGavPatterns != null ? excludedGavPatterns : Collections.<String>emptySet();
        this.relocatedPackagePrefixes = relocatedPackagePrefixes != null ? relocatedPackagePrefixes : Collections.<String>emptySet();
    }

    /**
     * Returns the set of explicitly excluded GAV patterns from {@code <artifactSet><excludes>}.
     * These are deps that are listed in the original POM but were deliberately NOT bundled.
     */
    public Set<String> getExcludedGavPatterns() {
        return excludedGavPatterns;
    }

    /**
     * Returns the set of relocated package class-path prefixes (slash-separated),
     * e.g. {@code "com/google/guava/"} for a {@code <pattern>com.google.guava</pattern>} relocation.
     */
    public Set<String> getRelocatedPackagePrefixes() {
        return relocatedPackagePrefixes;
    }

    /**
     * Returns {@code true} if this dep (by groupId + artifactId) matches any of the
     * explicitly excluded patterns from {@code <artifactSet><excludes>}.
     *
     * <p>Supported pattern formats:
     * <ul>
     *   <li>{@code com.google.guava:guava}   — exact match</li>
     *   <li>{@code com.google.guava:*}        — wildcard on artifactId</li>
     *   <li>{@code *:guava}                   — wildcard on groupId (rare)</li>
     *   <li>{@code com.google.guava:guava:jar} — with packaging suffix, matched by prefix</li>
     * </ul>
     *
     * @param groupId    the candidate dependency's groupId
     * @param artifactId the candidate dependency's artifactId
     * @return true if the dep is explicitly excluded from the fat JAR
     */
    public boolean isExcluded(String groupId, String artifactId) {
        if (groupId == null || artifactId == null || excludedGavPatterns.isEmpty()) {
            return false;
        }
        for (String pattern : excludedGavPatterns) {
            if (matchesExcludePattern(pattern, groupId, artifactId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the dep's groupId was relocated (i.e., its classes live at a
     * different path in the JAR than the original package suggests).
     *
     * @param groupId the candidate dependency's groupId
     */
    public boolean isRelocated(String groupId) {
        if (groupId == null || relocatedPackagePrefixes.isEmpty()) {
            return false;
        }
        String classPrefix = groupId.replace('.', '/') + "/";
        for (String relocated : relocatedPackagePrefixes) {
            // A dep is relocated if its package prefix starts with any relocation pattern prefix
            if (classPrefix.startsWith(relocated) || relocated.startsWith(classPrefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesExcludePattern(String pattern, String groupId, String artifactId) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return false;
        }
        // Strip packaging suffix if present (e.g., "com.google.guava:guava:jar" → "com.google.guava:guava")
        String normalised = pattern.trim();
        int secondColon = normalised.indexOf(':', normalised.indexOf(':') + 1);
        if (secondColon >= 0) {
            normalised = normalised.substring(0, secondColon);
        }

        String[] parts = normalised.split(":", 2);
        if (parts.length != 2) {
            return false;
        }
        String patternGroup = parts[0].trim();
        String patternArtifact = parts[1].trim();

        boolean groupMatches = "*".equals(patternGroup) || patternGroup.equals(groupId);
        boolean artifactMatches = "*".equals(patternArtifact) || patternArtifact.equals(artifactId);
        return groupMatches && artifactMatches;
    }
}


