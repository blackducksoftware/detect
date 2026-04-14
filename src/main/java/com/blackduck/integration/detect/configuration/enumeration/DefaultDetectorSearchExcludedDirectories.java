package com.blackduck.integration.detect.configuration.enumeration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum DefaultDetectorSearchExcludedDirectories {
    BIN("bin"),
    BUILD("build"),
    DOT_BUILD(".build"),
    DOT_GRADLE(".gradle"),
    DOT_YARN(".yarn"),
    MACOSX("__MACOSX"),
    NODE_MODULES("node_modules"),
    OUT("out"),
    PACKAGES("packages"),
    GIT(".git"),
    TARGET("target"),
    SYNOPSYS(".synopsys"),
    BLACKDUCK(".blackduck"),
    BRIDGE(".bridge");

    private final String directoryName;

    DefaultDetectorSearchExcludedDirectories(String directoryName) {
        this.directoryName = directoryName;
    }

    private String getDirectoryName() {
        return directoryName;
    }

    public static List<String> getDirectoryNames() {
        return Arrays.stream(DefaultDetectorSearchExcludedDirectories.values())
            .map(DefaultDetectorSearchExcludedDirectories::getDirectoryName)
            .collect(Collectors.toList());
    }

}
