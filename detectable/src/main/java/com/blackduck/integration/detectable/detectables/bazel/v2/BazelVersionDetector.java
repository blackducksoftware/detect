package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.executable.ExecutableOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;

/**
 * Detects the installed Bazel version by running `bazel --version`.
 * Used to gate features that require a minimum Bazel version (e.g., mod graph --output json requires 7.1+).
 */
public class BazelVersionDetector {
    private static final Logger logger = LoggerFactory.getLogger(BazelVersionDetector.class);

    private final BazelCommandExecutor bazel;

    public BazelVersionDetector(BazelCommandExecutor bazel) {
        this.bazel = bazel;
    }

    /**
     * Runs `bazel --version` and parses the output to determine the installed Bazel version.
     * Returns empty if the command fails or the output cannot be parsed.
     */
    public Optional<BazelVersion> detectVersion() {
        try {
            ExecutableOutput output = bazel.executeWithoutThrowing(Collections.singletonList("--version"));
            if (output.getReturnCode() != 0) {
                logger.debug("bazel --version returned non-zero exit code: {}", output.getReturnCode());
                return Optional.empty();
            }
            String stdout = output.getStandardOutput();
            Optional<BazelVersion> version = BazelVersion.parse(stdout);
            if (version.isPresent()) {
                logger.info("Detected Bazel version: {}", version.get());
            } else {
                logger.warn("Could not parse Bazel version from output: {}", stdout);
            }
            return version;
        } catch (Exception e) {
            logger.debug("Failed to detect Bazel version: {}", e.getMessage());
            return Optional.empty();
        }
    }
}

