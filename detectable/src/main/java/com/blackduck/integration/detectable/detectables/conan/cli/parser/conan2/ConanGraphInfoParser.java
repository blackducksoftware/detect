package com.blackduck.integration.detectable.detectables.conan.cli.parser.conan2;

import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.conan.Constants;
import com.blackduck.integration.detectable.detectables.conan.cli.config.ConanCliOptions;
import com.blackduck.integration.detectable.detectables.conan.cli.config.ConanDependencyType;
import com.blackduck.integration.detectable.detectables.conan.cli.parser.conan2.model.ConanGraphInfo;
import com.blackduck.integration.detectable.detectables.conan.cli.parser.conan2.model.ConanGraphInfoGraphNode;
import com.blackduck.integration.detectable.extraction.Extraction;

public class ConanGraphInfoParser {
    private final int ROOT_NODE_INDEX = 0;
    private final Gson gson;
    private final ConanCliOptions conanCliOptions;
    private final ExternalIdFactory externalIdFactory;

    public ConanGraphInfoParser(Gson gson, ConanCliOptions conanCliOptions, ExternalIdFactory externalIdFactory) {
        this.gson = gson;
        this.conanCliOptions = conanCliOptions;
        this.externalIdFactory = externalIdFactory;
    }

    public Extraction parse(String jsonString) throws DetectableException {
        ConanGraphInfo graphInfo;

        try {
            graphInfo = gson.fromJson(jsonString, ConanGraphInfo.class);
        } catch (JsonSyntaxException e) {
            return new Extraction.Builder().failure("Unable to parse conan graph info").build();
        }

        Map<Integer, ConanGraphInfoGraphNode> nodeMap = graphInfo.getGraph().getNodeMap();

        ConanGraphInfoGraphNode root = nodeMap.get(ROOT_NODE_INDEX);
        if (root == null) {
            return new Extraction.Builder().failure("No root node was found in the conan graph info").build();
        }

        DependencyGraph graph = new BasicDependencyGraph();
        boolean includeBuild = conanCliOptions.getDependencyTypeFilter().shouldInclude(ConanDependencyType.BUILD);
        boolean includeTest = conanCliOptions.getDependencyTypeFilter().shouldInclude(ConanDependencyType.TEST);
        populateDependencyGraph(graph, nodeMap, root.getDirectDependencyIndices(includeBuild, includeTest), null, includeBuild, includeTest);
        CodeLocation codeLocation = new CodeLocation(graph);

        return new Extraction.Builder()
            .success(codeLocation)
            .projectName(root.getName())
            .projectVersion(root.getVersion())
            .build();
    }

    private void populateDependencyGraph(
        DependencyGraph graph,
        Map<Integer, ConanGraphInfoGraphNode> nodeMap,
        Set<Integer> nodeIndicesToAdd,
        Dependency parent,
        boolean includeBuild,
        boolean includeTest) throws DetectableException
    {
        for (Integer i : nodeIndicesToAdd) {
            ConanGraphInfoGraphNode node = nodeMap.get(i);
            if (node == null) {
                throw new DetectableException("Unexpected dependency reference: " + i);
            }

            Dependency dependency = nodeToDependency(node);

            boolean processChildren = !graph.hasDependency(dependency.getExternalId());

            if (parent == null) {
                graph.addDirectDependency(nodeToDependency(node));
            } else {
                graph.addChildWithParent(dependency, parent);
            }

            if (processChildren) {
                populateDependencyGraph(graph, nodeMap, node.getDirectDependencyIndices(includeBuild, includeTest), dependency, includeBuild, includeTest);
            }
        }
    }

    private Dependency nodeToDependency(ConanGraphInfoGraphNode node) {
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(
            Constants.conanForge,
            node.getName(),
            node.generateExternalIdVersion(conanCliOptions.preferLongFormExternalIds())
        );
        return new Dependency(node.getName(), node.getVersion(), externalId, null);
    }
}
