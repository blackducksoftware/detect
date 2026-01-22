package com.blackduck.integration.detectable.detectables.maven.resolver.model;

/**
 * Java domain model representing a Maven repository.
 *
 * <p>This is the processed representation of a Maven repository after parsing
 * and normalizing repository information from the POM.
 *
 * <p>Includes repository identification (id, name, URL) and policy flags for
 * whether snapshots and releases should be retrieved from this repository.
 *
 * <p><strong>Default Behavior:</strong>
 * <ul>
 *   <li>Snapshots: Disabled by default</li>
 *   <li>Releases: Enabled by default</li>
 * </ul>
 */
public class JavaRepository {
    private String id;
    private String name;
    private String url;
    private boolean snapshotsEnabled = false; // default: snapshots disabled
    private boolean releasesEnabled = true;   // default: releases enabled

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public boolean isSnapshotsEnabled() { return snapshotsEnabled; }
    public void setSnapshotsEnabled(boolean snapshotsEnabled) { this.snapshotsEnabled = snapshotsEnabled; }

    public boolean isReleasesEnabled() { return releasesEnabled; }
    public void setReleasesEnabled(boolean releasesEnabled) { this.releasesEnabled = releasesEnabled; }
}

