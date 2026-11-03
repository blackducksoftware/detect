package com.blackduck.integration.detectable.detectables.bun.lockfile.model;

import java.util.List;

public class BunPackage {
    private final String key;
    private final String name;
    private final String version;
    private final List<BunPackageDependency> dependencies;

    public BunPackage(String key, String name, String version, List<BunPackageDependency> dependencies) {
        this.key = key;
        this.name = name;
        this.version = version;
        this.dependencies = dependencies;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public List<BunPackageDependency> getDependencies() { return dependencies; }
}
