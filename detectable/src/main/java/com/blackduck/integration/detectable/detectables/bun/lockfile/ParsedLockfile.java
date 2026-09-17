package com.blackduck.integration.detectable.detectables.bun.lockfile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockDependency;
import com.blackduck.integration.util.NameVersion;

// Raw, insertion-ordered contents of a bun.lock file before resolution.
public class ParsedLockfile {
    // entry key to (name, resolvedVersion)
    private final Map<String, NameVersion> keyToVersion = new LinkedHashMap<>();
    // entry key to dep list (dependencies + optionalDependencies)
    private final Map<String, List<BunLockDependency>> rawEntryDeps = new LinkedHashMap<>();
    // dep ranges from the workspaces section
    private final List<BunLockDependency> workspaceDeps = new ArrayList<>();

    public Map<String, NameVersion> getKeyToVersion() {
        return keyToVersion;
    }

    public Map<String, List<BunLockDependency>> getRawEntryDeps() {
        return rawEntryDeps;
    }

    public List<BunLockDependency> getWorkspaceDeps() {
        return workspaceDeps;
    }
}
