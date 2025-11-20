package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.maven.resolver.result.MavenParseResult;

public class MavenGraphTransformer {
    private final ExternalIdFactory externalIdFactory;

    public MavenGraphTransformer(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public DependencyGraph transform(MavenParseResult root) {
        BasicDependencyGraph graph = new BasicDependencyGraph();

        for (MavenParseResult childNode : root.getChildren()) {
            Dependency childDependency = createDependency(childNode);
            graph.addDirectDependency(childDependency);
            addChildrenToGraph(graph, childDependency, childNode);
        }

        return graph;
    }

    private void addChildrenToGraph(DependencyGraph graph, Dependency parentDependency, MavenParseResult parentNode) {
        for (MavenParseResult childNode : parentNode.getChildren()) {
            Dependency childDependency = createDependency(childNode);
            graph.addChildWithParent(childDependency, parentDependency);
            addChildrenToGraph(graph, childDependency, childNode);
        }
    }

    private Dependency createDependency(MavenParseResult node) {
        ExternalId externalId = externalIdFactory.createMavenExternalId(
            node.getGroupId(),
            node.getArtifactId(),
            node.getVersion()
        );
        return new Dependency(externalId);
    }
}

