package com.blackduck.integration.detectable.detectables.bun.lockfile.model;

public class BunLockDependency {
    private final String name;
    private final String range;
    private final boolean optional;

    public BunLockDependency(String name, String range, boolean optional) {
        this.name = name;
        this.range = range;
        this.optional = optional;
    }

    public String getName() { return name; }
    public String getRange() { return range; }
    public boolean isOptional() { return optional; }
}
