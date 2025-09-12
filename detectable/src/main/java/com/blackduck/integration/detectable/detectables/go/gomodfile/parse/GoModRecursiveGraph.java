package com.blackduck.integration.detectable.detectables.go.gomodfile.parse;

import java.util.ArrayList;
import java.util.List;

import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoDependencyNode;

public class GoModRecursiveGraph {
    private final GoDependencyNode graph;

    public GoModRecursiveGraph(GoDependencyNode graph) {
        this.graph = graph;
    }

    public List<GoDependencyNode> getDependencyPath(GoDependencyNode targetNode) {
        List<GoDependencyNode> path = new ArrayList<>();
        if (findPath(graph, targetNode, path)) {
            return path;
        }
        return null; // Path not found
    }

    private boolean findPath(GoDependencyNode currentNode, GoDependencyNode targetNode, List<GoDependencyNode> path) {
        path.add(currentNode);
        if (currentNode.equals(targetNode)) {
            return true;
        }
        for (GoDependencyNode child : currentNode.getChildren()) {
            if (findPath(child, targetNode, path)) {
                return true;
            }
        }
        path.remove(path.size() - 1);
        return false;
    }
}
    