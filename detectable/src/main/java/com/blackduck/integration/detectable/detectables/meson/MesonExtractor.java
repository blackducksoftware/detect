package com.blackduck.integration.detectable.detectables.meson;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.meson.parse.MesonDependencyFileParser;
import com.blackduck.integration.detectable.detectables.meson.parse.MesonProjectFileParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;

public class MesonExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final MesonProjectFileParser mesonProjectFileParser;
    private final MesonDependencyFileParser mesonDependencyFileParser;
    private final FileFinder fileFinder;
    private final Gson gson;

    private static final String introspect_project = "intro-projectinfo.json";
    private static final String introspect_dependencies = "intro-dependencies.json";

    public MesonExtractor(
        MesonProjectFileParser mesonProjectFileParser,
        MesonDependencyFileParser mesonDependencyFileParser,
        FileFinder fileFinder,
        Gson gson
    ) {
        this.mesonProjectFileParser = mesonProjectFileParser;
        this.mesonDependencyFileParser = mesonDependencyFileParser;
        this.fileFinder = fileFinder;
        this.gson = gson;
    }

    public Extraction extract(File directory) {
        try {
            // Find the intro-projectinfo.json file in subdirectories
            File projectFile = fileFinder.findFile(directory, introspect_project, false, 2);
            if (projectFile == null) {
                return new Extraction.Builder()
                    .failure("Could not find " + introspect_project + " in any subdirectory").build();
            }

            NameVersion nameVersion = determineProjectNameVersion(projectFile.getAbsolutePath());
            logger.debug("Found Meson project file: {}", projectFile.getAbsolutePath());

            // Parse dependencies from intro-dependencies.json
            File dependencyFile = fileFinder.findFile(directory, introspect_dependencies, false, 2);
            if (dependencyFile == null) {
                return new Extraction.Builder()
                    .failure("Could not find " + introspect_dependencies + " in any subdirectory").build();
            }
            try (BufferedReader reader = Files.newBufferedReader(dependencyFile.toPath(), StandardCharsets.UTF_8)) {
                DependencyGraph dependencyGraph = mesonDependencyFileParser.parseProjectDependencies(gson, reader);
                logger.debug("Found Meson dependency file: {}", dependencyFile.getAbsolutePath());
                CodeLocation codeLocation = new CodeLocation(dependencyGraph);
                
                return new Extraction.Builder()
                    .success(codeLocation)
                    .projectName(nameVersion.getName())
                    .projectVersion(nameVersion.getVersion())
                    .build();
            }

        } catch (Exception e) {
            logger.error("Failed to extract Meson dependencies", e);
            return new Extraction.Builder().exception(e).build();
        }
    }

    private NameVersion determineProjectNameVersion(String projectFilePath) {
        final String defaultProjectName = "";
        final String defaultProjectVersion = "";

        try {
            File projectInfoFile = new File(projectFilePath);
            if (projectInfoFile != null) {
                try (BufferedReader reader = Files.newBufferedReader(projectInfoFile.toPath(), StandardCharsets.UTF_8)) {
                    logger.debug("Found Meson project info file: {}", projectInfoFile.getAbsolutePath());
                    return mesonProjectFileParser.getProjectNameVersion(gson, reader, defaultProjectName, defaultProjectVersion);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse Meson introspect, using defaults", e);
        }

        return new NameVersion(defaultProjectName, defaultProjectVersion);
    }
}
