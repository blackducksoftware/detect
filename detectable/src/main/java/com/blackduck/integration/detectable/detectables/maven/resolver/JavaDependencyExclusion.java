package com.blackduck.integration.detectable.detectables.maven.resolver;

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

