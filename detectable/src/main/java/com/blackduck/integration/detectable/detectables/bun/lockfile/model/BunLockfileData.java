package com.blackduck.integration.detectable.detectables.bun.lockfile.model;

import java.util.List;
import java.util.Map;

public class BunLockfileData {
    private final List<BunLockPackage> packages;
    // name → { range-or-version → resolvedVersion }
    // Built from path-qualified key analysis; used to wire dep edges in the graph.
    private final Map<String, Map<String, String>> rangeToVersion;

    public BunLockfileData(List<BunLockPackage> packages, Map<String, Map<String, String>> rangeToVersion) {
        this.packages = packages;
        this.rangeToVersion = rangeToVersion;
    }

    public List<BunLockPackage> getPackages() { return packages; }
    public Map<String, Map<String, String>> getRangeToVersion() { return rangeToVersion; }
}
