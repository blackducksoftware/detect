package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.detectable.detectables.bazel.DependencySource;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class BazelGraphProber {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final BazelCommandExecutor bazel;
    private final String target;
    private final BazelEnvironmentAnalyzer.Mode mode;
    private final int httpProbeLimit;

    /**
     * Constructor for BazelGraphProber
     * @param bazel Bazel command executor
     * @param target Bazel target to probe
     * @param mode Bazel environment mode (defaults to BZLMOD if null)
     * @param httpProbeLimit Maximum number of repositories to probe for HTTP detection
     */
    public BazelGraphProber(BazelCommandExecutor bazel, String target, BazelEnvironmentAnalyzer.Mode mode, int httpProbeLimit) {
        this.bazel = bazel;
        this.target = target;
        this.mode = mode != null ? mode : BazelEnvironmentAnalyzer.Mode.BZLMOD;
        this.httpProbeLimit = httpProbeLimit;
    }

    /**
     * Probes the Bazel dependency graph to decide which pipelines (dependency sources) are enabled for the given target.
     * @return Set of enabled DependencySource
     */
    public Set<DependencySource> decidePipelines() {
        logger.info("Starting Bazel graph probing for target: {}", target);
        Set<DependencySource> enabled = new HashSet<>();

        boolean mavenInstall = false;
        boolean mavenJar = false;
        boolean haskell = false;
        boolean httpFamily = false;

        // Probe for rules_jvm_external (maven_install)
        try {
            mavenInstall = detectMavenInstall();
        } catch (Exception e) {
            logger.info("MAVEN_INSTALL probe failed: {}", e.getMessage());
        }
        // Probe for maven_jar
        try {
            mavenJar = detectMavenJar();
        } catch (Exception e) {
            logger.info("MAVEN_JAR probe failed: {}", e.getMessage());
        }
        // Probe for haskell_cabal_library
        try {
            haskell = detectHaskellCabal();
        } catch (Exception e) {
            logger.info("HASKELL_CABAL_LIBRARY probe failed: {}", e.getMessage());
        }
        // Probe for http_archive and related rules
        try {
            HttpFamilyProber httpProber = new HttpFamilyProber(bazel, mode == null ? BazelEnvironmentAnalyzer.Mode.BZLMOD : mode, httpProbeLimit);
            httpFamily = httpProber.detect(target);
        } catch (Exception e) {
            logger.info("HTTP_ARCHIVE family probe failed: {}", e.getMessage());
        }

        // Prefer maven_install over maven_jar if both are detected
        if (mavenInstall) {
            enabled.add(DependencySource.MAVEN_INSTALL);
            if (mavenJar) {
                logger.info("Both MAVEN_INSTALL and MAVEN_JAR indicated; preferring MAVEN_INSTALL and suppressing MAVEN_JAR.");
            }
        } else if (mavenJar) {
            enabled.add(DependencySource.MAVEN_JAR);
        }
        if (haskell) {
            enabled.add(DependencySource.HASKELL_CABAL_LIBRARY);
        }
        if (httpFamily) {
            enabled.add(DependencySource.HTTP_ARCHIVE);
        }

        logger.info("Probing completed. Enabled pipelines: {}", enabled);
        return enabled;
    }

    /**
     * Detects if rules_jvm_external (maven_install) is used by querying for j.*import rules with maven_coordinates.
     * @return true if maven_install is detected, false otherwise
     */
    private boolean detectMavenInstall() throws Exception {
        // Query j.*import rules under deps(target) and look for maven_coordinates tags in build output.
        Optional<String> out = bazel.executeToString(java.util.Arrays.asList(
            "cquery", "--noimplicit_deps", "kind(j.*import, deps(" + target + "))", "--output", "build"
        ));
        if (!out.isPresent()) {
            return false;
        }
        boolean found = out.get().contains("maven_coordinates=");
        if (found) {
            logger.info("Detected rules_jvm_external via j.*import with maven_coordinates.");
        }
        return found;
    }

    /**
     * Detects if maven_jar is used by filtering for @repo//jar:jar labels in the dependency graph.
     * @return true if maven_jar is detected, false otherwise
     */
    private boolean detectMavenJar() throws Exception {
        Optional<String> out = bazel.executeToString(java.util.Arrays.asList(
            "cquery", "filter('@.*:jar', deps(" + target + "))"
        ));
        boolean found = out.isPresent() && !out.get().trim().isEmpty();
        if (found) {
            logger.info("Detected maven_jar artifacts via @repo//jar:jar labels.");
        }
        return found;
    }

    /**
     * Detects if haskell_cabal_library rules are present in the dependency graph.
     * @return true if haskell_cabal_library is detected, false otherwise
     */
    private boolean detectHaskellCabal() throws Exception {
        Optional<String> out = bazel.executeToString(java.util.Arrays.asList(
            "cquery", "--noimplicit_deps", "kind(haskell_cabal_library, deps(" + target + "))", "--output", "label_kind"
        ));
        boolean found = out.isPresent() && out.get().contains("haskell_cabal_library");
        if (found) {
            logger.info("Detected haskell_cabal_library rules.");
        }
        return found;
    }
}
