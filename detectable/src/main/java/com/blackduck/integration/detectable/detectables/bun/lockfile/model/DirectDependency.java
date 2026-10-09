package com.blackduck.integration.detectable.detectables.bun.lockfile.model;

public class DirectDependency {
    private final String name;
    private final String version;
    private final BunDependencyType type;

    public DirectDependency(String name, String version, BunDependencyType type) {
        this.name = name;
        this.version = version;
        this.type = type;
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public BunDependencyType getType() { return type; }
}
