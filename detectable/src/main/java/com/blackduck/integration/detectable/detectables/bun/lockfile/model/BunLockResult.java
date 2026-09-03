package com.blackduck.integration.detectable.detectables.bun.lockfile.model;

import java.util.List;
import java.util.Map;

public class BunLockResult {
    private final BunLockfileData data;

    public BunLockResult(BunLockfileData data) {
        this.data = data;
    }

    public BunLockfileData getData() {
        return data;
    }

    public List<BunLockPackage> getPackages() {
        return data.getPackages();
    }

    public Map<String, Map<String, String>> getRangeToVersion() {
        return data.getRangeToVersion();
    }
}
