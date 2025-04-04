package com.blackduck.integration.detectable.detectables.uv;


import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class UVDetectorOptions {
    private final Set<String> excludedDependencyGroups;

    private final Set<String> includedWorkspaceMembers;

    private final Set<String> excludedWorkspaceMembers;

    public UVDetectorOptions(List<String> excludedDependencyGroups, List<String> includedWorkspaceMembers, List<String> excludedWorkspaceMembers) {
        this.excludedDependencyGroups = new HashSet<>(excludedDependencyGroups);
        this.includedWorkspaceMembers = new HashSet<>(includedWorkspaceMembers);
        this.excludedWorkspaceMembers = new HashSet<>(excludedWorkspaceMembers);
    }

    public Set<String> getExcludedDependencyGroups() {
        return excludedDependencyGroups;
    }

    public Set<String> getIncludedWorkspaceMembers() {
        return includedWorkspaceMembers;
    }

    public Set<String> getExcludedWorkspaceMembers() {
        return excludedWorkspaceMembers;
    }
}
