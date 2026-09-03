package com.blackduck.integration.detectable.detectables.bun.lockfile.model;

import java.util.List;

public class BunLockPackage {
    private final String name;
    private final String version;
    private final List<BunLockDependency> dependencies;

    public BunLockPackage(String name, String version, List<BunLockDependency> dependencies) {
        this.name = name;
        this.version = version;
        this.dependencies = dependencies;
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public List<BunLockDependency> getDependencies() { return dependencies; }
}
