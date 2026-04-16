package com.blackduck.integration.detectable.detectables.nuget;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectables.nuget.parse.NugetInspectorParser;
import com.blackduck.integration.detectable.detectables.nuget.parse.NugetParseResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.executable.Executable;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.executable.ExecutableRunnerException;
import com.blackduck.integration.util.NameVersion;

public class NugetInspectorExtractor {
    public static final String INSPECTOR_OUTPUT_PATTERN = "*_inspection.json";

    private final Logger logger = LoggerFactory.getLogger(NugetInspectorExtractor.class);

    private final NugetInspectorParser nugetInspectorParser;
    private final FileFinder fileFinder;
    private final DetectableExecutableRunner executableRunner;

    public NugetInspectorExtractor(NugetInspectorParser nugetInspectorParser, FileFinder fileFinder, DetectableExecutableRunner executableRunner) {
        this.nugetInspectorParser = nugetInspectorParser;
        this.fileFinder = fileFinder;
        this.executableRunner = executableRunner;
    }

    public Extraction extract(List<File> targets, File outputDirectory, ExecutableTarget inspector, NugetInspectorOptions nugetInspectorOptions) {
        try {
            List<NugetTargetResult> results = new ArrayList<>();

            for (int i = 0; i < targets.size(); i++) {
                File targetDirectory = new File(outputDirectory, "inspection-" + i);
                results.add(executeTarget(inspector, targets.get(i), targetDirectory, nugetInspectorOptions));
            }

            List<CodeLocation> codeLocations = results.stream()
                .flatMap(it -> it.codeLocations.stream())
                .collect(Collectors.toList());

            Map<File, CodeLocation> codeLocationsBySource = new HashMap<>();

            codeLocations.forEach(codeLocation -> {
                File sourcePathFile = codeLocation.getSourcePath().orElse(null);
                if (codeLocationsBySource.containsKey(sourcePathFile)) {
                    logger.debug("Combined code location for: {}", sourcePathFile);
                    CodeLocation destination = codeLocationsBySource.get(sourcePathFile);
                    destination.getDependencyGraph().copyGraphToRoot(codeLocation.getDependencyGraph());
                } else {
                    codeLocationsBySource.put(sourcePathFile, codeLocation);
                }
            });

            Optional<NameVersion> nameVersion = results.stream()
                .filter(it -> it.nameVersion != null)
                .map(it -> it.nameVersion)
                .findFirst();

            List<CodeLocation> uniqueCodeLocations = new ArrayList<>(codeLocationsBySource.values());
            return new Extraction.Builder().success(uniqueCodeLocations).nameVersionIfPresent(nameVersion).build();
        } catch (Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }

    private NugetTargetResult executeTarget(ExecutableTarget inspector, File targetFile, File outputDirectory, NugetInspectorOptions nugetInspectorOptions)
        throws ExecutableRunnerException, IOException, DetectableException {
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new DetectableException(String.format("Executing the nuget inspector failed, could not create output directory: %s", outputDirectory));
        }

        List<String> arguments = NugetInspectorArguments.fromInspectorOptions(nugetInspectorOptions, targetFile, outputDirectory);
        Executable executable = ExecutableUtils.createFromTarget(outputDirectory, inspector, arguments);
        ExecutableOutput executableOutput = executableRunner.execute(executable);

        if (executableOutput.getReturnCode() != 0) {
            throw new DetectableException(String.format("Executing the nuget inspector failed: %s", executableOutput.getReturnCode()));
        }

        List<File> dependencyNodeFiles = fileFinder.findFiles(outputDirectory, INSPECTOR_OUTPUT_PATTERN);

        List<NugetParseResult> parseResults = new ArrayList<>();
        for (File dependencyNodeFile : dependencyNodeFiles) {
            String text = FileUtils.readFileToString(dependencyNodeFile, StandardCharsets.UTF_8);
            NugetParseResult result = nugetInspectorParser.createCodeLocation(text);
            parseResults.add(result);
        }

        NugetTargetResult targetResult = new NugetTargetResult();

        targetResult.codeLocations = parseResults.stream()
            .flatMap(it -> it.getCodeLocations().stream())
            .collect(Collectors.toList());

        targetResult.nameVersion = parseResults.stream()
            .filter(it -> StringUtils.isNotBlank(it.getProjectName()))
            .map(it -> new NameVersion(it.getProjectName(), it.getProjectVersion()))
            .findFirst()
            .orElse(null);

        return targetResult;
    }

}
