package com.blackduck.integration.detectable.detectables.maven.cli;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Optional;

import com.blackduck.integration.detectable.detectables.maven.parsing.MavenProjectInspectorDetectable;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import org.apache.commons.lang3.StringUtils;

import com.blackduck.integration.common.util.Bds;
import com.blackduck.integration.common.util.parse.CommandParser;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.util.ToolVersionLogger;
import com.blackduck.integration.executable.ExecutableOutput;

public class MavenCliExtractor {
    private final DetectableExecutableRunner executableRunner;
    private final MavenCodeLocationPackager mavenCodeLocationPackager;
    private final CommandParser commandParser;
    private final ToolVersionLogger toolVersionLogger;
    private Map<String, Set<String>> shadedDependencies = new HashMap<>();

    public MavenCliExtractor(
        DetectableExecutableRunner executableRunner,
        MavenCodeLocationPackager mavenCodeLocationPackager,
        CommandParser commandParser,
        ToolVersionLogger toolVersionLogger
    ) {
        this.executableRunner = executableRunner;
        this.mavenCodeLocationPackager = mavenCodeLocationPackager;
        this.commandParser = commandParser;
        this.toolVersionLogger = toolVersionLogger;
    }

    public Extraction extract(File directory, ExecutableTarget mavenExe, MavenCliExtractorOptions mavenCliExtractorOptions, MavenProjectInspectorDetectable mavenProjectInspectorDetectable, ExtractionEnvironment extractionEnvironment) throws ExecutableFailedException {
        toolVersionLogger.log(directory, mavenExe);
        List<String> commandArguments = mavenCliExtractorOptions.buildCliArguments(commandParser);

        ExecutableOutput mvnExecutableResult = executableRunner.executeSuccessfully(ExecutableUtils.createFromTarget(directory, mavenExe, commandArguments));

        List<String> mavenOutput = mvnExecutableResult.getStandardOutputAsList();
        List<String> excludedScopes = mavenCliExtractorOptions.getMavenExcludedScopes();
        List<String> includedScopes = mavenCliExtractorOptions.getMavenIncludedScopes();
        List<String> excludedModules = mavenCliExtractorOptions.getMavenExcludedModules();
        List<String> includedModules = mavenCliExtractorOptions.getMavenIncludedModules();
        Boolean includeShadedDependencies = mavenCliExtractorOptions.getMavenIncludeShadedDependencies();

        if(includeShadedDependencies) {
            Extraction extraction;
            mavenProjectInspectorDetectable.setIncludeShadedDependencies(true);

            try {
                mavenProjectInspectorDetectable.extractable();
                extraction = mavenProjectInspectorDetectable.extract(extractionEnvironment);
            } catch (Exception e) {
               throw new RuntimeException("There was an error extracting the shaded dependencies from Project Inspector. There might be version mismatch between Detect and Project Inspector, confirm that compatible versions of them are in use.", e);
            }

            if(extraction.isSuccess()) {
                shadedDependencies = mavenProjectInspectorDetectable.getShadedDependencies();
            } else {
                Exception exception = new RuntimeException("There was an error extracting the shaded dependencies from Project Inspector. There might be version mismatch between Detect and Project Inspector, confirm that compatible versions of them are in use.", extraction.getError());
                return new Extraction.Builder().exception(exception).build();
            }
        }

        List<MavenParseResult> mavenResults = mavenCodeLocationPackager.extractCodeLocations(
            directory.toString(),
            mavenOutput,
            excludedScopes,
            includedScopes,
            excludedModules,
            includedModules,
            shadedDependencies
        );

        List<CodeLocation> codeLocations = Bds.of(mavenResults)
            .map(MavenParseResult::getCodeLocation)
            .toList();

        Optional<MavenParseResult> firstWithName = Bds.of(mavenResults)
            .firstFiltered(it -> StringUtils.isNotBlank(it.getProjectName()));

        Extraction.Builder builder = new Extraction.Builder().success(codeLocations);
        if (firstWithName.isPresent()) {
            builder.projectName(firstWithName.get().getProjectName());
            builder.projectVersion(firstWithName.get().getProjectVersion());
        }
        return builder.build();
    }
}
