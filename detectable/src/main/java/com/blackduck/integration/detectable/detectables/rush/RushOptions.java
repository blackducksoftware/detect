package com.blackduck.integration.detectable.detectables.rush;

import java.util.List;

public class RushOptions {
    private final List<String> excludedSubspaces;
    private final List<String> includedSubspaces;

    public RushOptions(List<String> excludedSubspaces, List<String> includedSubspaces) {
        this.excludedSubspaces = excludedSubspaces;
        this.includedSubspaces = includedSubspaces;
    }
    public List<String> getExcludedSubspaces() {
        return excludedSubspaces;
    }

    public List<String> getIncludedSubspaces() {
        return includedSubspaces;
    }
}
