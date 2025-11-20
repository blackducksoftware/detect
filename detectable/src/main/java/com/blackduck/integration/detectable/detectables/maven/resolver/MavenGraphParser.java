package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.detectable.detectables.maven.resolver.result.MavenParseResult;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.DependencyNode;

public class MavenGraphParser {

    public MavenParseResult parse(CollectResult collectResult) {
        DependencyNode rootNode = collectResult.getRoot();
        MavenParseResult rootParseResult = new MavenParseResult(
                rootNode.getArtifact().getGroupId(),
                rootNode.getArtifact().getArtifactId(),
                rootNode.getArtifact().getVersion()
        );

        for (DependencyNode childNode : rootNode.getChildren()) {
            parseNode(childNode, rootParseResult);
        }

        return rootParseResult;
    }

    private void parseNode(DependencyNode dependencyNode, MavenParseResult parentParseResult) {
        MavenParseResult childParseResult = new MavenParseResult(
                dependencyNode.getArtifact().getGroupId(),
                dependencyNode.getArtifact().getArtifactId(),
                dependencyNode.getArtifact().getVersion()
        );
        parentParseResult.addChild(childParseResult);

        for (DependencyNode childNode : dependencyNode.getChildren()) {
            parseNode(childNode, childParseResult);
        }
    }
}

