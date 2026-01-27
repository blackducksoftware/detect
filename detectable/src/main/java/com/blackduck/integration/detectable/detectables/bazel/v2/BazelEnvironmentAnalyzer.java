package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.executable.ExecutableOutput;
import org.apache.commons.lang3.StringUtils;
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
        // 'mod show_repo' is the standard way to probe Bzlmod state via CLI
        ExecutableOutput output = bazel.executeWithoutThrowing(Arrays.asList("mod", "show_repo", "bazel_tools"));
        int returnCode = output.getReturnCode();
        String stdout = (output.getStandardOutput() != null ? output.getStandardOutput() : "");
        String stderr = (output.getErrorOutput() != null ? output.getErrorOutput() : "").toLowerCase();

        // Success: Command recognized and executed in a Bzlmod-enabled environment
        if (returnCode == 0 && StringUtils.isNotBlank(stdout)) {
            logger.debug("Bzlmod detected: 'bazel mod' succeeded with output.");
            return Mode.BZLMOD;
        }

        // Failure: Analyze exit codes and official error strings
        if (returnCode != 0) {
            // Case 1: Command not supported (Bazel < 6.3 or ancient versions)
            if (stderr.contains("command 'mod' not found") ||
                    stderr.contains("unknown command") ||
                    stderr.contains("no such command")) {
                logger.warn("Bazel version does not support 'mod' command. Falling back to WORKSPACE.");
                return Mode.WORKSPACE;
            }

            // Case 2: Official WORKSPACE indicators (Bzlmod disabled or file missing)
            // These strings match official outputs from Bazel 6.x and 7.x
            if (stderr.contains("bzlmod has to be enabled") ||
                    stderr.contains("bzlmod is disabled") ||
                    stderr.contains("not a module.bazel repo") ||
                    stderr.contains("no module.bazel file found")) {
                logger.debug("WORKSPACE mode detected: Bzlmod is explicitly disabled or MODULE.bazel is missing.");
                return Mode.WORKSPACE;
            }

            // Case 3: Unexpected execution failure (e.g., network, internal crash)
            logger.error("Bazel mode detection failed with an unexpected error (Exit {}): {}", returnCode, stderr);
            return Mode.UNKNOWN;
        }

        // Empty output with Exit 0 usually implies no modules defined/WORKSPACE mode fallback
        return Mode.WORKSPACE;
    }


}

