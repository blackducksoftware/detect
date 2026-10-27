package com.blackduck.integration.detectable.detectables.bun.lockfile.model;

import org.jetbrains.annotations.Nullable;

public class WorkspaceDependency {
    private final String name;
    private final String range;
    private final BunDependencyType type;
    private final String workspaceName;

    /**
     * @param workspaceName the "name" of the workspace that declares this dependency, or null when the workspace
     *                      has none. Bun prefixes workspace-specific entries in "packages" with this name
     *                      (e.g. "@scope/ui/vite"), not with the workspace directory path.
     */
    public WorkspaceDependency(String name, String range, BunDependencyType type, @Nullable String workspaceName) {
        this.name = name;
        this.range = range;
        this.type = type;
        this.workspaceName = workspaceName;
    }

    public String getName() { return name; }
    public String getRange() { return range; }
    public BunDependencyType getType() { return type; }
    @Nullable
    public String getWorkspaceName() { return workspaceName; }
}
