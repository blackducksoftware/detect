package com.blackduck.integration.detectable.detectables.bazel.pipeline.step;

import java.io.File;
import java.util.Collections;
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
    private static final String BAZEL = "bazel";

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
     * Executes a Bazel {@code mod} command (e.g., {@code mod show_repo}, {@code mod graph}),
     * accepting stdout even when the exit code is non-zero.
     *
     * <p>This is intentionally permissive for {@code mod} commands because a broken but unrelated
     * module extension (e.g., {@code bazel_jar_jar+} on Bazel 9) poisons the process exit code to 2
     * while the requested output is still fully and correctly written to stdout. For these commands,
     * stdout presence is the authoritative success signal — if Bazel resolved the repo/graph it
     * writes the definition to stdout; if it genuinely failed (e.g., "no such repo") stdout is empty.</p>
     *
     * <p><b>NOT</b> safe for {@code query}/{@code cquery}: partial query output on a non-zero exit
     * could be misleading (some targets resolved before the failure). Use {@link #executeToString}
     * for those commands.</p>
     *
     * @param args Bazel command arguments (should start with "mod")
     * @return stdout content if non-empty, {@link Optional#empty()} otherwise
     */
    public Optional<String> executeModCommandToString(List<String> args) {
        ExecutableOutput result = executeWithoutThrowing(args);
        int exitCode = result.getReturnCode();
        if (exitCode != 0) {
            String stderr = result.getErrorOutput();
            String firstStderrLine = "";
            if (!StringUtils.isBlank(stderr)) {
                String[] lines = stderr.split("\\r?\\n", 2);
                firstStderrLine = lines.length > 0 ? lines[0] : "(none)";
            } else {
                firstStderrLine = "(none)";
            }
            logger.debug("Bazel mod command returned exit code {}; first stderr line: {}", exitCode, firstStderrLine);
        }
        String stdout = result.getStandardOutput();
        if (StringUtils.isBlank(stdout)) {
            logger.debug("Bazel mod command produced no stdout output");
            return Optional.empty();
        }
        return Optional.of(stdout);
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
            String command = (bazelExe != null ? bazelExe.toCommand() : BAZEL) + " " + String.join(" ", args == null ? Collections.emptyList() : args);
            String msg = String.format("Failed to execute Bazel command '%s': %s", command, e.getMessage());
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }
}
