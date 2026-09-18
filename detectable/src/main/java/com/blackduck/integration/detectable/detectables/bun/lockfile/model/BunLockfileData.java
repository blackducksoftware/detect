package com.blackduck.integration.detectable.detectables.bun.lockfile.model;

import java.util.List;
import java.util.Map;

public class BunLockfileData {
    private final List<DirectDependency> directDependencies;
    // name to exact version; populated from the "catalog" section of bun.lock
    private final Map<String, String> catalog;
    private final List<BunPackage> packages;

    public BunLockfileData(List<DirectDependency> directDependencies, Map<String, String> catalog, List<BunPackage> packages) {
        this.directDependencies = directDependencies;
        this.catalog = catalog;
        this.packages = packages;
    }

    public List<DirectDependency> getDirectDependencies() { return directDependencies; }
    public Map<String, String> getCatalog() { return catalog; }
    public List<BunPackage> getPackages() { return packages; }
}
