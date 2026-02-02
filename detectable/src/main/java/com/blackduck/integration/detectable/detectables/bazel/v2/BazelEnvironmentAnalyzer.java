package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.executable.ExecutableOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Detects the active Bazel environment mode for this invocation: BZLMOD vs WORKSPACE.
 * Uses fast, read-only Bazel commands (no builds).
 */
public class BazelEnvironmentAnalyzer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public enum Mode { BZLMOD, WORKSPACE, UNKNOWN }

    private final BazelCommandExecutor bazel;

    public BazelEnvironmentAnalyzer(BazelCommandExecutor bazel) {
        this.bazel = bazel;
    }

    public Mode getMode() {
        Mode mode = detectMode();
        logger.info("Bazel mode detected: {}", mode);
        return mode;
    }

    private Mode detectMode() {
        ExecutableOutput output = bazel.executeWithoutThrowing(Arrays.asList("mod", "graph"));

        if (output.getReturnCode() == 0) {
            String stdout = output.getStandardOutput().trim();
            // If the graph only contains the root module, Bzlmod is active but empty.
            // Look for typical empty signals like "<root> (@_)" or just a single line.
            if (isGraphEmpty(stdout)) {
                logger.info("Bzlmod is enabled but graph is empty; falling back to WORKSPACE.");
                return Mode.WORKSPACE;
            }
            return Mode.BZLMOD;
        } else if (output.getReturnCode() >= 2 && output.getReturnCode() <= 7) {
            return Mode.WORKSPACE;
        }
        return Mode.UNKNOWN;
    }

    private boolean isGraphEmpty(String stdout) {
        // A single line usually means only the root module is present
        return stdout.isEmpty() || !stdout.contains("\n") || stdout.contains("<root> (@_)");
    }
}
