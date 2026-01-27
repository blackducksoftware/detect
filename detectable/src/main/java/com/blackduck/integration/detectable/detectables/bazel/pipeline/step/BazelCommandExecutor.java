package com.blackduck.integration.detectable.detectables.bazel.pipeline.step;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.executable.ExecutableOutput;

public class BazelCommandExecutor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DetectableExecutableRunner executableRunner;
    private final File workspaceDir;
    private final ExecutableTarget bazelExe;

    public BazelCommandExecutor(DetectableExecutableRunner executableRunner, File workspaceDir, ExecutableTarget bazelExe) {
        this.executableRunner = executableRunner;
        this.workspaceDir = workspaceDir;
        this.bazelExe = bazelExe;
    }

    public Optional<String> executeToString(List<String> args) throws ExecutableFailedException {
        ExecutableOutput targetDependenciesQueryResults = executableRunner.executeSuccessfully(ExecutableUtils.createFromTarget(workspaceDir, bazelExe, args));
        String cmdStdErr = targetDependenciesQueryResults.getErrorOutput();
        if (cmdStdErr != null && cmdStdErr.contains("ERROR")) {
            logger.warn("Bazel error: {}", cmdStdErr.trim());
        }
        String cmdStdOut = targetDependenciesQueryResults.getStandardOutput();
        if ((StringUtils.isBlank(cmdStdOut))) {
            logger.debug("bazel command produced no output");
            return Optional.empty();
        }
        return Optional.of(cmdStdOut);
    }

    /**
     * Executes a Bazel command without throwing on failure, allowing caller to inspect exit code and error output.
     * Used for probing commands where we need to distinguish different failure modes.
     * @param args Bazel command arguments
     * @return ExecutableOutput containing return code, stdout, and stderr
     */
    public ExecutableOutput executeWithoutThrowing(List<String> args) {
        try {
            return executableRunner.execute(ExecutableUtils.createFromTarget(workspaceDir, bazelExe, args));
        } catch (Exception e) {
            logger.error("Failed to execute Bazel command: {}", e.getMessage());
            throw new RuntimeException("Bazel execution failed", e);
        }
    }
}
