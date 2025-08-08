package com.blackduck.integration.detectable.detectables.poetry;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;
import org.tomlj.TomlTable;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.poetry.parser.PoetryLockParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.util.NameVersion;

public class PoetryExtractor {
    private static final String NAME_KEY = "name";
    private static final String VERSION_KEY = "version";

    private final PoetryLockParser poetryLockParser;

    public PoetryExtractor(PoetryLockParser poetryLockParser) {
        this.poetryLockParser = poetryLockParser;
    }

    public Extraction extract(File poetryLock, @Nullable TomlTable toolDotPoetrySection, @Nullable TomlTable projectSection, Set<String> rootPackages) {
        try {
            DependencyGraph graph = poetryLockParser.parseLockFile(
                FileUtils.readFileToString(poetryLock, StandardCharsets.UTF_8),
                rootPackages
            );
            CodeLocation codeLocation = new CodeLocation(graph);

            Extraction.Builder extractionBuilder = new Extraction.Builder().success(codeLocation);

            Optional<NameVersion> poetryNameVersion = extractNameVersionFromSection(toolDotPoetrySection);
            if (poetryNameVersion.isEmpty()) {
                // Poetry 2.x support
                poetryNameVersion = extractNameVersionFromSection(projectSection);
            }

            if (poetryNameVersion.isPresent()) {
                extractionBuilder.projectName(poetryNameVersion.get().getName())
                    .projectVersion(poetryNameVersion.get().getVersion());
            }
            return extractionBuilder.build();
        } catch (IOException e) {
            return new Extraction.Builder().exception(e).build();
        }
    }

    private Optional<NameVersion> extractNameVersionFromSection(@Nullable TomlTable toolDotPoetryOrProject) {
        if (toolDotPoetryOrProject != null) {
            if (toolDotPoetryOrProject.get(NAME_KEY) != null && toolDotPoetryOrProject.get(VERSION_KEY) != null) {
                return Optional.of(new NameVersion(toolDotPoetryOrProject.getString(NAME_KEY), toolDotPoetryOrProject.getString(VERSION_KEY)));
            }
        }
        return Optional.empty();
    }

}
