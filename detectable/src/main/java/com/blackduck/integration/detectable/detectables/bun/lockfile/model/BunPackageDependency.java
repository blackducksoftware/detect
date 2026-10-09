package com.blackduck.integration.detectable.detectables.bun.lockfile.model;

public class BunPackageDependency {
    private final String name;
    private final String range;
    private final BunDependencyType type;

    public BunPackageDependency(String name, String range, BunDependencyType type) {
        this.name = name;
        this.range = range;
        this.type = type;
    }

    public String getName() { return name; }
    public String getRange() { return range; }
    public BunDependencyType getType() { return type; }
}
