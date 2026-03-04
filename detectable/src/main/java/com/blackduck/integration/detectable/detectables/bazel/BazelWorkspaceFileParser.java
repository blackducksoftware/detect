package com.blackduck.integration.detectable.detectables.bazel;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BazelWorkspaceFileParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Set<DependencySource> parseDependencySources(List<String> workspaceFileLines) {
        return workspaceFileLines.stream()
            .flatMap(this::parseDependencySourcesFromWorkspaceFileLine)
            .collect(Collectors.toSet());
    }

    // Sonar deems peek useful for debugging.
    private Stream<DependencySource> parseDependencySourcesFromWorkspaceFileLine(String workspaceFileLine) {
        return Arrays.stream(DependencySource.values())
            .filter(dependencySource -> workspaceFileLine.matches(String.format("^\\s*%s\\s*\\(", dependencySource.getName())))
            .peek(dependencySource -> logger.debug(String.format("Found workspace dependency source: %s", dependencySource.getName())));
    }
}
