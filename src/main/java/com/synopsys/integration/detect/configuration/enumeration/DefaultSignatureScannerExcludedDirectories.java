package com.synopsys.integration.detect.configuration.enumeration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum DefaultSignatureScannerExcludedDirectories {
    DOT_GRADLE(".gradle"),
    MACOSX("__MACOSX"),
    NODE_MODULES("node_modules"),
    GIT(".git"),
    SYNOPSYS(".synopsys");

    private final String directoryName;

    DefaultSignatureScannerExcludedDirectories(String directoryName) {
        this.directoryName = directoryName;
    }

    private String getDirectoryName() {
        return directoryName;
    }

    public static List<String> getDirectoryNames() {
        return Arrays.stream(DefaultSignatureScannerExcludedDirectories.values())
            .map(DefaultSignatureScannerExcludedDirectories::getDirectoryName)
            .collect(Collectors.toList());
    }
}
