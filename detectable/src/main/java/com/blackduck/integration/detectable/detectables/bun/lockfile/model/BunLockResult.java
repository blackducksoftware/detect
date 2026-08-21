package com.blackduck.integration.detectable.detectables.bun.lockfile.model;

public class BunLockResult {
    private final BunLockfileData data;

    public BunLockResult(BunLockfileData data) {
        this.data = data;
    }

    public BunLockfileData getData() {
        return data;
    }
}
