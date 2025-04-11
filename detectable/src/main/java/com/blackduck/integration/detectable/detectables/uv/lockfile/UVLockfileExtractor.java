package com.blackduck.integration.detectable.detectables.uv.lockfile;

import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import com.blackduck.integration.detectable.detectables.uv.parse.UVTomlParser;
import com.blackduck.integration.detectable.detectables.uv.transform.UVLockParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.executable.ExecutableRunnerException;
import com.blackduck.integration.util.NameVersion;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

public class UVLockfileExtractor {

    private static final Logger logger = LoggerFactory.getLogger(UVLockfileExtractor.class);

    private final UVTomlParser uvTomlParser;
    private final File sourceDirectory;
    private final UVLockParser uvLockParser;

    public UVLockfileExtractor(File sourceDirectory, UVTomlParser uvTomlParser, UVLockParser uvLockParser) {
        this.sourceDirectory = sourceDirectory;
        this.uvTomlParser = uvTomlParser;
        this.uvLockParser = uvLockParser;
    }

    public Extraction extract(UVDetectorOptions uvDetectorOptions, File uvTomlFile, File uvLockFile, File requirementsTxtFile) throws ExecutableRunnerException, IOException {
        try {
            Optional<NameVersion> projectNameVersion = Optional.empty();
            if (uvTomlFile != null) {
                String uvTomlContents = FileUtils.readFileToString(uvTomlFile, StandardCharsets.UTF_8);
                projectNameVersion = uvTomlParser.parseNameVersion(uvTomlContents);
            }

            List<CodeLocation> codeLocations = new ArrayList<>();
            if (uvLockFile != null) {
                String uvLockContents = FileUtils.readFileToString(uvLockFile, StandardCharsets.UTF_8);
                if (projectNameVersion.isPresent()) {
                    codeLocations = uvLockParser.parseLockFile(uvLockContents, projectNameVersion.get().getName(), uvDetectorOptions);
                } else {
                    logger.warn("Name and Version were not found in pyproject.toml file. Creating a project with default name and version.");
                    codeLocations = uvLockParser.parseLockFile(uvLockContents, "uvProject", uvDetectorOptions);
                }
            }

            return new Extraction.Builder()
                    .success(codeLocations)
                    .nameVersionIfPresent(projectNameVersion)
                    .build();
        } catch (Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }

}
