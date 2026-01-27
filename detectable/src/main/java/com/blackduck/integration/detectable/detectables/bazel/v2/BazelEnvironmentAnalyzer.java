package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

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
        try {
            Optional<String> modProbe = bazel.executeToString(Arrays.asList("mod", "show_repo", "bazel_tools"));
            if (modProbe.isPresent()) {
                logger.debug("Mode detection via 'bazel mod show_repo bazel_tools': BZLMOD");
                return Mode.BZLMOD;
            }
        } catch (Exception e) {
            logger.debug("BZLMOD detection failed, assuming WORKSPACE mode", e);
        }
        logger.debug("Mode detection: WORKSPACE");
        return Mode.WORKSPACE;
    }

}

