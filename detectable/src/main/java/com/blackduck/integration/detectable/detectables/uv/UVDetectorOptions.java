package com.blackduck.integration.detectable.detectables.uv;


import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
        return excludedDependencyGroups.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    public Set<String> getIncludedWorkspaceMembers() {
        return includedWorkspaceMembers.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    public Set<String> getExcludedWorkspaceMembers() {
        return excludedWorkspaceMembers.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }
}
