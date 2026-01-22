package com.blackduck.integration.detectable.detectables.maven.resolver.model;

import java.util.List;

/**
 * Java domain model representing a resolved Maven dependency.
 *
 * <p>This is the processed representation of a Maven dependency after parsing,
 * property resolution, and dependency management application.
 *
 * <p>Contains:
 * <ul>
 *   <li>Coordinates (via {@link JavaCoordinates})</li>
 *   <li>Scope (compile, test, provided, runtime, system)</li>
 *   <li>Type/packaging</li>
 *   <li>Classifier (for variant artifacts)</li>
 *   <li>Exclusions list</li>
 *   <li>Dependency path tracking (includedBy)</li>
 *   <li>System path for system-scoped dependencies</li>
 * </ul>
 */
public class JavaDependency {
    private List<String> includedBy;
    private JavaCoordinates coordinates;
    private String scope;
    private String type;
    private String classifier;
    private String projectDependencyFilePath;
    private List<JavaDependencyExclusion> exclusions;

    public JavaDependency() {}

    public JavaDependency(JavaCoordinates coordinates, String scope, String type, String classifier, List<JavaDependencyExclusion> exclusions) {
        this.coordinates = coordinates;
        this.scope = scope;
        this.type = type;
        this.classifier = classifier;
        this.exclusions = exclusions;
    }

    // Getters and Setters
    public List<String> getIncludedBy() { return includedBy; }
    public void setIncludedBy(List<String> includedBy) { this.includedBy = includedBy; }

    public JavaCoordinates getCoordinates() { return coordinates; }
    public void setCoordinates(JavaCoordinates coordinates) { this.coordinates = coordinates; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getClassifier() { return classifier; }
    public void setClassifier(String classifier) { this.classifier = classifier; }

    public String getProjectDependencyFilePath() { return projectDependencyFilePath; }
    public void setProjectDependencyFilePath(String projectDependencyFilePath) { this.projectDependencyFilePath = projectDependencyFilePath; }

    public List<JavaDependencyExclusion> getExclusions() { return exclusions; }
    public void setExclusions(List<JavaDependencyExclusion> exclusions) { this.exclusions = exclusions; }
}

