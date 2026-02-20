package com.blackduck.integration.detectable.detectables.bazel.pipeline;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.bazel.DependencySource;

public class DependencySourceChooser {

    @NotNull
    public Set<DependencySource> choose(Set<DependencySource> sourcesFromWorkspaceFile, Set<DependencySource> sourcesFromProperty)
        throws DetectableException {
        if (sourcesFromProperty != null && !sourcesFromProperty.isEmpty()) {
            return sourcesFromProperty;
        } else if (!sourcesFromWorkspaceFile.isEmpty()) {
            return sourcesFromWorkspaceFile;
        } else {
            throw new DetectableException("Unable to determine Bazel dependency sources; try setting it via the property");
        }
    }

}
