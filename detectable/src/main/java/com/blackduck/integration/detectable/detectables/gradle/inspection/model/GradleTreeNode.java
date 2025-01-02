package com.blackduck.integration.detectable.detectables.gradle.inspection.model;

import java.util.Optional;

import com.blackduck.integration.util.Stringable;

public class GradleTreeNode extends Stringable {
    public GradleTreeNode(NodeType nodeType, int level, GradleGav gav, String projectName) {
        this.nodeType = nodeType;
        this.level = level;
        this.gav = gav;
        this.projectName = projectName;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public Optional<GradleGav> getGav() {
        return Optional.ofNullable(gav);
    }

    public Optional<String> getProjectName() {
        return Optional.ofNullable(projectName);
    }

    public int getLevel() {
        return level;
    }

    public enum NodeType {
        GAV,
        UNKNOWN,
        PROJECT
    }

    private final NodeType nodeType;
    private final int level;
    private final GradleGav gav;
    private final String projectName;

    public static GradleTreeNode newProject(int level, String subProjectName) {
        return new GradleTreeNode(NodeType.PROJECT, level, null, subProjectName);
    }

    public static GradleTreeNode newGav(int level, String group, String name, String version) {
        return new GradleTreeNode(NodeType.GAV, level, new GradleGav(group, name, version), null);
    }

    public static GradleTreeNode newUnknown(int level) {
        return new GradleTreeNode(NodeType.UNKNOWN, level, null, null);
    }
}
