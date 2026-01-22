package com.blackduck.integration.detectable.detectables.maven.resolver.model;

/**
 * Java domain model representing a dependency exclusion.
 *
 * <p>Used in the processed dependency model ({@link JavaDependency}) to represent
 * exclusions that should prevent specific transitive dependencies from being included.
 *
 * <p>This is the normalized form after converting from {@link PomXmlDependencyExclusion}.
 */
public class JavaDependencyExclusion {
    private final String groupId;
    private final String artifactId;

    public JavaDependencyExclusion(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }
}

