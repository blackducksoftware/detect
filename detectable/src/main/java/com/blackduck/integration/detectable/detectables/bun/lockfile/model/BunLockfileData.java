package com.blackduck.integration.detectable.detectables.bun.lockfile.model;

import java.util.List;

public class BunLockfileData {
    private final List<DirectDependency> directDependencies;
    private final List<BunPackage> packages;

    public BunLockfileData(List<DirectDependency> directDependencies, List<BunPackage> packages) {
        this.directDependencies = directDependencies;
        this.packages = packages;
    }

    public List<DirectDependency> getDirectDependencies() { return directDependencies; }
    public List<BunPackage> getPackages() { return packages; }
}
