package com.blackduck.integration.detectable.detectables.clang.dependencyfile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyListFileParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public List<String> parseDepsMk(File depsMkFile) {
        try {
            String depsMkText = FileUtils.readFileToString(depsMkFile, StandardCharsets.UTF_8);
            return parseDepsMk(depsMkText);
        } catch (Exception e) {
            logger.warn(String.format("Error getting dependency file paths from '%s': %s", depsMkFile.getAbsolutePath(), e.getMessage()));
            return Collections.emptyList();
        }
    }

    public List<String> parseDepsMk(String depsMkText) {
        String[] depsMkTextParts = depsMkText.split(": ");
        if (depsMkTextParts.length != 2) {
            logger.warn(String.format("Unable to split mk text parts reasonably: %s", String.join(" ", depsMkTextParts)));
            return Collections.emptyList();
        }
        String depsListString = depsMkTextParts[1];
        logger.trace(String.format("dependencies: %s", depsListString));

        depsListString = depsListString.replace("\n", " ");
        logger.trace(String.format("dependencies, newlines removed: %s", depsListString));

        depsListString = depsListString.replace("\\", " "); //TODO: This does not work on Windows paths.
        logger.trace(String.format("dependencies, backslashes removed: %s", depsListString));

        String[] deps = depsListString.split("\\s+");
        List<String> depsList = Arrays.stream(deps)
            .filter(StringUtils::isNotBlank)
            .map(this::normalize)
            .collect(Collectors.toList());
        return depsList;
    }

    private String normalize(String rawPath) {
        String normalizedPath = Paths.get(rawPath).normalize().toString();
        logger.trace(String.format("Normalized %s to %s", rawPath, normalizedPath));
        return normalizedPath;
    }
}
