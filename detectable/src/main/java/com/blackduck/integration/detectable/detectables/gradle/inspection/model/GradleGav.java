package com.blackduck.integration.detectable.detectables.gradle.inspection.model;

import com.blackduck.integration.bdio.graph.builder.LazyId;

public class GradleGav implements GradleGavId {
    private final String name;
    private final String group;
    private final String version;

    public GradleGav(String group, String name, String version) {
        this.group = group;
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public LazyId toDependencyId() {
        return LazyId.fromString(String.format("%s:%s:%s", getGroup(), getName(), getVersion()));
    }

    @Override
    public String toString() {
        return getGroup() + ":" + getName() + ":" + getVersion();
    }
}
