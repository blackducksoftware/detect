package com.blackduck.integration.detectable.detectables.ivy;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectables.ivy.parse.IvyDependencyTreeParser;
import com.blackduck.integration.detectable.detectables.ivy.parse.IvyProjectNameParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.util.ToolVersionLogger;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.executable.ExecutableRunnerException;
import com.blackduck.integration.util.NameVersion;

public class IvyCliExtractor {
    private static final Logger logger = LoggerFactory.getLogger(IvyCliExtractor.class);
    private static final String IVY_DEPENDENCY_TREE_MARKER = "[ivy:dependencytree]";

    private final DetectableExecutableRunner executableRunner;
    private final IvyDependencyTreeParser ivyDependencyTreeParser;
    private final IvyProjectNameParser ivyProjectNameParser;
    private final ToolVersionLogger toolVersionLogger;

    public IvyCliExtractor(
        DetectableExecutableRunner executableRunner,
        IvyDependencyTreeParser ivyDependencyTreeParser,
        IvyProjectNameParser ivyProjectNameParser,
        ToolVersionLogger toolVersionLogger
    ) {
        this.executableRunner = executableRunner;
        this.ivyDependencyTreeParser = ivyDependencyTreeParser;
        this.ivyProjectNameParser = ivyProjectNameParser;
        this.toolVersionLogger = toolVersionLogger;
    }

    public Extraction extract(
        File directory,
        ExecutableTarget antExe,
        @Nullable File buildXmlFile,
        String targetName
    ) throws IOException {
        toolVersionLogger.log(directory, antExe);

        ExecutableOutput antOutput = executeAntTarget(directory, antExe, targetName);
        if (antOutput == null) {
            return createFailure(String.format("Failed to execute ant %s", targetName));
        }

        Optional<Extraction> validationFailure = validateAntOutput(antOutput, targetName);
        if (validationFailure.isPresent()) {
            return validationFailure.get();
        }

        // Parse dependency graph
        List<String> dependencyTreeOutput = antOutput.getStandardOutputAsList();
        DependencyGraph graph = ivyDependencyTreeParser.parse(dependencyTreeOutput);
        CodeLocation codeLocation = new CodeLocation(graph);

        // Extract project name and version if available
        Optional<NameVersion> projectNameVersion = Optional.empty();
        if (buildXmlFile != null) {
            projectNameVersion = ivyProjectNameParser.parseProjectName(buildXmlFile);
        }

        return new Extraction.Builder()
            .success(codeLocation)
            .nameVersionIfPresent(projectNameVersion)
            .build();
    }

    @Nullable
    private ExecutableOutput executeAntTarget(File directory, ExecutableTarget antExe, String targetName) {
        try {
            List<String> antCommand = Collections.singletonList(targetName);
            return executableRunner.execute(ExecutableUtils.createFromTarget(directory, antExe, antCommand));
        } catch (ExecutableRunnerException e) {
            logger.error("Failed to execute ant {}: {}", targetName, e.getMessage());
            return null;
        }
    }

    private Optional<Extraction> validateAntOutput(ExecutableOutput antOutput, String targetName) {
        List<String> dependencyTreeOutput = antOutput.getStandardOutputAsList();

        if (dependencyTreeOutput.isEmpty()) {
            String errorOutput = antOutput.getErrorOutputAsList().isEmpty()
                ? "No output produced"
                : String.join("\n", antOutput.getErrorOutputAsList());
            return Optional.of(createFailure(String.format(
                "Ant %s produced no output. Exit code: %d. Error: %s",
                targetName, antOutput.getReturnCode(), errorOutput
            )));
        }

        boolean hasDependencyTree = dependencyTreeOutput.stream()
            .anyMatch(line -> line.contains(IVY_DEPENDENCY_TREE_MARKER));

        if (!hasDependencyTree) {
            logger.warn("Ant command completed with exit code {} but no dependency tree found in output", antOutput.getReturnCode());
            return Optional.of(createFailure(String.format(
                "No dependency tree found in ant %s output. Ensure the target contains <ivy:dependencytree /> task.",
                targetName
            )));
        }

        if (antOutput.getReturnCode() != 0) {
            logger.warn("Ant command returned non-zero exit code {} but produced valid output, continuing...", antOutput.getReturnCode());
        }

        return Optional.empty();
    }

    private Extraction createFailure(String message) {
        return new Extraction.Builder()
            .failure(message)
            .build();
    }
}