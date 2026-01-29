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
        int returnCode = output.getReturnCode();
        if (returnCode == 0) {
            return Mode.BZLMOD;
        } else if (returnCode >= 2 && returnCode <= 7) {
            // Covers: Disabled (2), Unknown Command (3), and Missing MODULE.bazel (7)
            return Mode.WORKSPACE;
        } else {
            return Mode.UNKNOWN;
        }
    }



}

