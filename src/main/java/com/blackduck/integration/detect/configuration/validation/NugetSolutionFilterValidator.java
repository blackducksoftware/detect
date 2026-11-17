package com.blackduck.integration.detect.configuration.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;

public class NugetSolutionFilterValidator {
    public static final String SOLUTION_FILTER_EXTENSION = ".slnf";

    public Optional<DetectUserFriendlyException> validate(@Nullable Path solutionFilterPath, @Nullable List<String> includedModules, @Nullable List<String> excludedModules) {
        if (solutionFilterPath == null) {
            return Optional.empty();
        }

        String propertyKey = DetectProperties.DETECT_NUGET_SOLUTION_FILTER_PATH.getKey();

        List<String> conflictingProperties = new ArrayList<>();
        if (includedModules != null && !includedModules.isEmpty()) {
            conflictingProperties.add(DetectProperties.DETECT_NUGET_INCLUDED_MODULES.getKey());
        }
        if (excludedModules != null && !excludedModules.isEmpty()) {
            conflictingProperties.add(DetectProperties.DETECT_NUGET_EXCLUDED_MODULES.getKey());
        }
        if (!conflictingProperties.isEmpty()) {
            return failure(String.format(
                "Invalid configuration: %s cannot be combined with %s or %s (found: %s). The solution filter (.slnf) already defines which projects to scan. "
                    + "Either remove %s, or remove %s and use the included/excluded modules properties against the full solution.",
                propertyKey,
                DetectProperties.DETECT_NUGET_INCLUDED_MODULES.getKey(),
                DetectProperties.DETECT_NUGET_EXCLUDED_MODULES.getKey(),
                String.join(", ", conflictingProperties),
                String.join(" and ", conflictingProperties),
                propertyKey
            ));
        }

        Path absolutePath = solutionFilterPath.toAbsolutePath().normalize();
        Path fileName = absolutePath.getFileName();
        if (fileName == null || !fileName.toString().toLowerCase().endsWith(SOLUTION_FILTER_EXTENSION)) {
            return failure(String.format(
                "Invalid configuration: %s must point to a solution filter file with a %s extension, but was: %s. Provide the path to a single .slnf file.",
                propertyKey,
                SOLUTION_FILTER_EXTENSION,
                absolutePath
            ));
        }
        if (!Files.exists(absolutePath)) {
            return failure(String.format(
                "Invalid configuration: %s points to a file that does not exist: %s. Verify the path is correct (relative paths are resolved against the current working directory).",
                propertyKey,
                absolutePath
            ));
        }
        if (!Files.isRegularFile(absolutePath)) {
            return failure(String.format(
                "Invalid configuration: %s must point to a .slnf file, but the path is not a regular file: %s.",
                propertyKey,
                absolutePath
            ));
        }

        return Optional.empty();
    }

    private Optional<DetectUserFriendlyException> failure(String message) {
        return Optional.of(new DetectUserFriendlyException(message, ExitCodeType.FAILURE_CONFIGURATION));
    }
}
