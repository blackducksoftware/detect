package com.blackduck.integration.detectable.detectables.maven.resolver.result;

import java.util.ArrayList;
import java.util.List;

public class MavenParseResult {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final List<MavenParseResult> children = new ArrayList<>();

    public MavenParseResult(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public List<MavenParseResult> getChildren() {
        return children;
    }

    public void addChild(MavenParseResult child) {
        children.add(child);
    }
}

