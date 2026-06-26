package com.blackduck.integration.detectable.detectables.npm.cli;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectables.npm.cli.parse.NpmCliParser;
import com.blackduck.integration.detectable.detectables.npm.lockfile.result.NpmPackagerResult;
import com.blackduck.integration.detectable.detectables.npm.packagejson.CombinedPackageJson;
import com.blackduck.integration.detectable.detectables.npm.packagejson.CombinedPackageJsonExtractor;
import com.blackduck.integration.detectable.detectables.npm.packagejson.model.PackageJson;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.util.ToolVersionLogger;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.util.ExcludedIncludedWildcardFilter;

public class NpmCliExtractor {
    public static final String OUTPUT_FILE = "detect_npm_proj_dependencies.json";
    public static final String ERROR_FILE = "detect_npm_error.json";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DetectableExecutableRunner executableRunner;
    private final NpmCliParser npmCliParser;
    private final Gson gson;
    private final ToolVersionLogger toolVersionLogger;
    @Nullable private final ExcludedIncludedWildcardFilter workspaceFilter;

    public NpmCliExtractor(DetectableExecutableRunner executableRunner, NpmCliParser npmCliParser, Gson gson, ToolVersionLogger toolVersionLogger) {
        this(executableRunner, npmCliParser, gson, toolVersionLogger, null);
    }

    public NpmCliExtractor(DetectableExecutableRunner executableRunner, NpmCliParser npmCliParser,
            Gson gson, ToolVersionLogger toolVersionLogger,
            @Nullable ExcludedIncludedWildcardFilter workspaceFilter) {
        this.executableRunner = executableRunner;
        this.npmCliParser = npmCliParser;
        this.gson = gson;
        this.toolVersionLogger = toolVersionLogger;
        this.workspaceFilter = workspaceFilter;
    }

    public Extraction extract(File directory, ExecutableTarget npmExe, @Nullable String npmArguments, File packageJsonFile) {
        toolVersionLogger.log(directory, npmExe);
        CombinedPackageJson combinedPackageJson;
        try {
            combinedPackageJson = parsePackageJson(packageJsonFile);
        } catch (IOException e) {
            return new Extraction.Builder().exception(e).build();
        }

        List<String> exeArgs = new ArrayList<>();
        exeArgs.add("ls");
        exeArgs.add("-json");
        exeArgs.add("--all");
        
        Optional.ofNullable(npmArguments)
            .map(arg -> arg.split(" "))
            .ifPresent(additionalArguments -> exeArgs.addAll(Arrays.asList(additionalArguments)));

        ExecutableOutput npmLsOutput;
        try {
            npmLsOutput = executableRunner.execute(ExecutableUtils.createFromTarget(directory, npmExe, exeArgs));
        } catch (Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
        String standardOutput = npmLsOutput.getStandardOutput();
        String errorOutput = npmLsOutput.getErrorOutput();
        if (StringUtils.isNotBlank(errorOutput)) {
            logger.error("Error when running npm ls -json command");
            logger.error(errorOutput);
            return new Extraction.Builder().failure("Npm wrote to stderr while running npm ls.").build();
        } else if (StringUtils.isNotBlank(standardOutput)) {
            logger.debug("Parsing npm ls file.");
            logger.debug(standardOutput);
            NpmPackagerResult result = npmCliParser.generateCodeLocation(standardOutput, combinedPackageJson, workspaceFilter);
            String projectName = result.getProjectName() != null ? result.getProjectName() : combinedPackageJson.getName();
            String projectVersion = result.getProjectVersion() != null ? result.getProjectVersion() : combinedPackageJson.getVersion();
            return new Extraction.Builder().success(result.getCodeLocation()).projectName(projectName).projectVersion(projectVersion).build();
        } else {
            logger.error("Nothing returned from npm ls -json command");
            return new Extraction.Builder().failure("Npm returned error after running npm ls.").build();
        }
    }

    private CombinedPackageJson parsePackageJson(File packageJson) throws IOException {
        String packageJsonText = FileUtils.readFileToString(packageJson, StandardCharsets.UTF_8);
        
        CombinedPackageJsonExtractor extractor = new CombinedPackageJsonExtractor(gson);
        CombinedPackageJson combinedPackageJson = extractor.constructCombinedPackageJson(packageJson.getPath(), packageJsonText, workspaceFilter);

        return combinedPackageJson;
    }
}
