package com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model;

import java.util.List;

import com.blackduck.integration.bdio.model.dependency.Dependency;

public class GoDependencyNode {
    private final Dependency dependency;
    private final List<GoDependencyNode> children;
    private boolean isRootNode;


    public GoDependencyNode(boolean isRootNode, Dependency dependency, List<GoDependencyNode> children) {
        this.dependency = dependency;
        this.children = children;
        this.isRootNode = isRootNode;
    }

    public boolean isRootNode() {
        return isRootNode;
    }

    public Dependency getDependency() {
        return dependency;
    }

    public List<GoDependencyNode> getChildren() {
        return children;
    }

    public void appendChildren(List<GoDependencyNode> additionalChildren) {
        this.children.addAll(additionalChildren);
    }

    public void addChild(GoDependencyNode child) {
        this.children.add(child);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GoDependencyNode that = (GoDependencyNode) o;

        return dependency != null ? dependency.equals(that.dependency) : that.dependency == null;
    }
    
    /**
     * Utility method to get the total count of all dependencies in the graph (including transitive).
     * 
     * @return Total count of dependencies
     */
    public int getTotalDependencyCount() {
        int count = (dependency != null && !isRootNode) ? 1 : 0;
        
        for (GoDependencyNode child : children) {
            count += child.getTotalDependencyCount();
        }
        
        return count;
    }
    
    /**
     * Utility method to get the maximum depth of the dependency graph.
     * 
     * @return Maximum depth
     */
    public int getMaxDepth() {
        if (children.isEmpty()) {
            return isRootNode ? 0 : 1;
        }
        
        int maxChildDepth = 0;
        for (GoDependencyNode child : children) {
            maxChildDepth = Math.max(maxChildDepth, child.getMaxDepth());
        }
        
        return (isRootNode ? 0 : 1) + maxChildDepth;
    }
    
    @Override
    public String toString() {
        String depName = (dependency != null) ? dependency.getName() : "null";
        return "GoDependencyNode{" +
                "dependency=" + depName +
                ", isRootNode=" + isRootNode +
                ", childrenCount=" + children.size() +
                '}';
    }
}