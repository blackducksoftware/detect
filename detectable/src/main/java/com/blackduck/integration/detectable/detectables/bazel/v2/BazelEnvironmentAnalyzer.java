package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

/**
 * Detects the active Bazel environment "era" for this invocation: BZLMOD vs LEGACY.
 * Uses fast, read-only Bazel commands (no builds) and caches the decision.
 */
public class BazelEnvironmentAnalyzer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public enum Era { BZLMOD, LEGACY, UNKNOWN }

    private final BazelCommandExecutor bazel;
    private Era cachedEra;

    public BazelEnvironmentAnalyzer(BazelCommandExecutor bazel) {
        this.bazel = bazel;
    }

    public synchronized Era getEra() {
        if (cachedEra != null) return cachedEra;
        cachedEra = detectEra();
        logger.info("Bazel era detected: {}", cachedEra);
        return cachedEra;
    }

    public boolean isBzlmodActive() {
        return getEra() == Era.BZLMOD;
    }

    private Era detectEra() {
        // Primary: mod show_repo should exist and respond under bzlmod
        try {
            Optional<String> modProbe = bazel.executeToString(Arrays.asList("mod", "show_repo", "@bazel_tools"));
            if (modProbe.isPresent()) {
                logger.debug("Era detection via 'bazel mod show_repo @bazel_tools': BZLMOD");
                return Era.BZLMOD;
            }
        } catch (Exception ignored) {
            // proceed to next probe
        }

        // Secondary: explicit make env flag often present
        try {
            Optional<String> infoOut = bazel.executeToString(Arrays.asList("info", "--show_make_env"));
            if (infoOut.isPresent() && infoOut.get().contains("ENABLE_BZLMOD=1")) {
                logger.debug("Era detection via 'bazel info --show_make_env': BZLMOD");
                return Era.BZLMOD;
            }
        } catch (Exception ignored) {
            // proceed to next probe
        }

        // Tertiary: legacy //external probe; if it succeeds, assume legacy
        try {
            Optional<String> extProbe = bazel.executeToString(Arrays.asList("query", "kind(.*, //external:bazel_tools)", "--output", "xml"));
            if (extProbe.isPresent()) {
                logger.debug("Era detection via '//external' probe: LEGACY");
                return Era.LEGACY;
            }
        } catch (Exception ignored) {
            // fall through
        }

        // Default posture for 2026: prefer BZLMOD when inconclusive
        logger.debug("Era detection inconclusive; defaulting to BZLMOD");
        return Era.BZLMOD;
    }
}

