package com.blackduck.integration.detectable.detectables.pip.inspector;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectables.pip.inspector.model.NameVersionCodeLocation;
import com.blackduck.integration.detectable.detectables.pip.inspector.parser.PipInspectorTreeParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.util.ToolVersionLogger;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.executable.ExecutableRunnerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

public class PipInspectorExtractor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DetectableExecutableRunner executableRunner;
    private final PipInspectorTreeParser pipInspectorTreeParser;
    private final ToolVersionLogger toolVersionLogger;
    private static final String NAME_KEY = "name";
    private static final String VERSION_KEY = "version";
    private static final String PROJECT_KEY = "project";

    public PipInspectorExtractor(DetectableExecutableRunner executableRunner, PipInspectorTreeParser pipInspectorTreeParser, ToolVersionLogger toolVersionLogger) {
        this.executableRunner = executableRunner;
        this.pipInspectorTreeParser = pipInspectorTreeParser;
        this.toolVersionLogger = toolVersionLogger;
    }

    public Extraction extract(
        File directory,
        ExecutableTarget pythonExe,
        ExecutableTarget pipExe,
        File pipInspector,
        File setupFile,
        List<Path> requirementFilePaths,
        String providedProjectName,
        TomlParseResult tomlParseResult
    ) {
        toolVersionLogger.log(directory, pythonExe);
        toolVersionLogger.log(directory, pipExe);
        Extraction extractionResult;
        try {
            String projectName = getProjectName(directory, pythonExe, setupFile, tomlParseResult, providedProjectName);

            if (StringUtils.isEmpty(projectName) && requirementFilePaths.isEmpty()) {
                return new Extraction.Builder().failure("Unable to run the Pip Inspector without a project name or a requirements file").build();
            }

            List<CodeLocation> codeLocations = new ArrayList<>();
            String projectVersion = null;

            List<Path> requirementsPaths = new ArrayList<>();

            if (requirementFilePaths.isEmpty()) {
                requirementsPaths.add(null);
            } else {
                requirementsPaths.addAll(requirementFilePaths);
            }

            for (Path requirementFilePath : requirementsPaths) {
                List<String> inspectorOutput = runInspector(directory, pythonExe, pipInspector, projectName, requirementFilePath);
                Optional<NameVersionCodeLocation> result = pipInspectorTreeParser.parse(inspectorOutput, directory.toString());
                if (result.isPresent()) {
                    codeLocations.add(result.get().getCodeLocation());
                    String potentialProjectVersion = result.get().getProjectVersion();
                    if (projectVersion == null && StringUtils.isNotBlank(potentialProjectVersion)) {
                        projectVersion = potentialProjectVersion;
                    }
                }
            }

            if (codeLocations.isEmpty()) {
                extractionResult = new Extraction.Builder().failure("The Pip Inspector tree parse failed to produce output.").build();
            } else {
                extractionResult = new Extraction.Builder()
                    .success(codeLocations)
                    .projectName(projectName)
                    .projectVersion(projectVersion)
                    .build();
            }
        } catch (Exception e) {
            extractionResult = new Extraction.Builder().exception(e).build();
        }

        return extractionResult;
    }

    private List<String> runInspector(File sourceDirectory, ExecutableTarget pythonExe, File inspectorScript, String projectName, Path requirementsFilePath)
        throws ExecutableRunnerException {
        List<String> inspectorArguments = new ArrayList<>();
        inspectorArguments.add(inspectorScript.getAbsolutePath());

        if (requirementsFilePath != null) {
            inspectorArguments.add(String.format("--requirements=%s", requirementsFilePath.toAbsolutePath().toString()));
        }

        if (StringUtils.isNotBlank(projectName)) {
            inspectorArguments.add(String.format("--projectname=%s", projectName));
        }

        return executableRunner.execute(ExecutableUtils.createFromTarget(sourceDirectory, pythonExe, inspectorArguments)).getStandardOutputAsList();
    }

    private String getProjectName(File directory, ExecutableTarget pythonExe, File setupFile, TomlParseResult tomlParseResult, String providedProjectName) throws ExecutableRunnerException {
        if (StringUtils.isNotBlank(providedProjectName)) {
            return providedProjectName;
        }

        String projectName = providedProjectName;

        // TODO this whole section with the toml parsing can throw NPEs at different points, add try/catch instead of ifs at each step before merging.
        if (tomlParseResult != null && (tomlParseResult.getTable(PROJECT_KEY) != null)) {
            logger.info("The value of the condition is: " + (tomlParseResult != null && (tomlParseResult.getTable(PROJECT_KEY) != null)));
            TomlTable projectTable = tomlParseResult.getTable(PROJECT_KEY);
            logger.info("Project table is: " + projectTable);
            if( projectTable!= null && projectTable.contains(NAME_KEY)) {
                projectName = projectTable.getString(NAME_KEY);
            }
        } else if (setupFile != null && setupFile.exists()) {
            List<String> pythonArguments = Arrays.asList(setupFile.getAbsolutePath(), "--name");
            ExecutableOutput executableOutput = executableRunner.execute(ExecutableUtils.createFromTarget(directory, pythonExe, pythonArguments));
            if (executableOutput.getReturnCode() == 0) {
                List<String> output = executableOutput.getStandardOutputAsList();
                projectName = output.get(output.size() - 1).replace('_', '-').trim();
            }
        }

        return projectName;
    }
}
