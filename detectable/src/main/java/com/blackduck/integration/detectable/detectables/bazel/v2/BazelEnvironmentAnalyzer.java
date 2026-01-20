package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

/**
 * Detects the active Bazel environment mode for this invocation: BZLMOD vs WORKSPACE.
 * Uses fast, read-only Bazel commands (no builds) and caches the decision.
 */
public class BazelEnvironmentAnalyzer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public enum Mode { BZLMOD, WORKSPACE, UNKNOWN }

    private final BazelCommandExecutor bazel;
    private Mode cachedMode;

    public BazelEnvironmentAnalyzer(BazelCommandExecutor bazel) {
        this.bazel = bazel;
    }

    public synchronized Mode getMode() {
        if (cachedMode != null) return cachedMode;
        cachedMode = detectMode();
        logger.info("Bazel mode detected: {}", cachedMode);
        return cachedMode;
    }

    public boolean isBzlmodActive() {
        return getMode() == Mode.BZLMOD;
    }

    private Mode detectMode() {
        // Primary: mod show_repo should exist and respond under bzlmod
        try {
            Optional<String> modProbe = bazel.executeToString(Arrays.asList("mod", "show_repo", "@bazel_tools"));
            if (modProbe.isPresent()) {
                logger.debug("Mode detection via 'bazel mod show_repo @bazel_tools': BZLMOD");
                return Mode.BZLMOD;
            }
        } catch (Exception ignored) {
            // proceed to next probe
        }

        // Secondary: explicit make env flag often present
        try {
            Optional<String> infoOut = bazel.executeToString(Arrays.asList("info", "--show_make_env"));
            if (infoOut.isPresent() && infoOut.get().contains("ENABLE_BZLMOD=1")) {
                logger.debug("Mode detection via 'bazel info --show_make_env': BZLMOD");
                return Mode.BZLMOD;
            }
        } catch (Exception ignored) {
            // proceed to next probe
        }

        // Tertiary: legacy //external probe; if it succeeds, assume workspace mode
        try {
            Optional<String> extProbe = bazel.executeToString(Arrays.asList("query", "kind(.*, //external:bazel_tools)", "--output", "xml"));
            if (extProbe.isPresent()) {
                logger.debug("Mode detection via '//external' probe: WORKSPACE");
                return Mode.WORKSPACE;
            }
        } catch (Exception ignored) {
            // fall through
        }

        // Default posture for 2026: prefer BZLMOD when inconclusive
        logger.debug("Mode detection inconclusive; defaulting to BZLMOD");
        return Mode.BZLMOD;
    }
}

