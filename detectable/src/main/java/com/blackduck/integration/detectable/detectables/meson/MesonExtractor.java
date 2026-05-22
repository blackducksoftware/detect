package com.blackduck.integration.detectable.detectables.meson;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.DependencyGraph;
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
    private final Gson gson;

    public MesonExtractor(
        MesonProjectFileParser mesonProjectFileParser,
        MesonDependencyFileParser mesonDependencyFileParser,
        Gson gson
    ) {
        this.mesonProjectFileParser = mesonProjectFileParser;
        this.mesonDependencyFileParser = mesonDependencyFileParser;
        this.gson = gson;
    }

    public Extraction extract(File projectInfoFile, File dependenciesFile) {
        try {
            logger.debug("Parsing Meson project info: {}", projectInfoFile.getAbsolutePath());
            NameVersion nameVersion = determineProjectNameVersion(projectInfoFile);

            logger.debug("Parsing Meson dependencies: {}", dependenciesFile.getAbsolutePath());
            try (BufferedReader reader = Files.newBufferedReader(dependenciesFile.toPath(), StandardCharsets.UTF_8)) {
                DependencyGraph dependencyGraph = mesonDependencyFileParser.parseProjectDependencies(gson, reader);
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

    private NameVersion determineProjectNameVersion(File projectInfoFile) {
        final String defaultProjectName = "";
        final String defaultProjectVersion = "";

        try (BufferedReader reader = Files.newBufferedReader(projectInfoFile.toPath(), StandardCharsets.UTF_8)) {
            return mesonProjectFileParser.getProjectNameVersion(gson, reader, defaultProjectName, defaultProjectVersion);
        } catch (Exception e) {
            logger.warn("Failed to parse Meson introspect, using defaults", e);
        }

        return new NameVersion(defaultProjectName, defaultProjectVersion);
    }
}
