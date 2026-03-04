package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.detectable.detectables.bazel.DependencySource;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.query.BazelQueryBuilder;
import com.blackduck.integration.detectable.detectables.bazel.query.OutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class BazelGraphProber {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Bazel rule kind patterns for dependency detection
    private static final String JAVA_IMPORT_RULE_PATTERN = "j.*import";
    private static final String MAVEN_JAR_FILTER_PATTERN = "'@.*:jar'";
    private static final String HASKELL_CABAL_RULE_PATTERN = "haskell_cabal_library";

    private final BazelCommandExecutor bazel;
    private final String target;
    private final BazelEnvironmentAnalyzer.Mode mode;

    /**
     * Constructor for BazelGraphProber
     * @param bazel Bazel command executor
     * @param target Bazel target to probe
     * @param mode Bazel environment mode
     */
    public BazelGraphProber(BazelCommandExecutor bazel, String target, BazelEnvironmentAnalyzer.Mode mode) {
        this.bazel = bazel;
        this.target = target;
        this.mode = mode;
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
            logger.debug("MAVEN_INSTALL probe failed: {}", e.getMessage());
        }
        // Probe for maven_jar
        try {
            mavenJar = detectMavenJar();
        } catch (Exception e) {
            logger.debug("MAVEN_JAR probe failed: {}", e.getMessage());
        }
        // Probe for haskell_cabal_library
        try {
            haskell = detectHaskellCabal();
        } catch (Exception e) {
            logger.debug("HASKELL_CABAL_LIBRARY probe failed: {}", e.getMessage());
        }
        // Probe for http_archive and related rules
        try {
            HttpFamilyProber httpProber = new HttpFamilyProber(bazel, mode);
            httpFamily = httpProber.detect(target);
        } catch (Exception e) {
            logger.debug("HTTP_ARCHIVE family probe failed: {}", e.getMessage());
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
        // Use cquery for rules_jvm_external detection --- we need build-output fidelity (maven_coordinates)
        // because this information comes from the action graph and reflects resolved artifact coordinates.
        List<String> queryArgs = BazelQueryBuilder.cquery()
            .kind(JAVA_IMPORT_RULE_PATTERN, BazelQueryBuilder.deps(target))
            .withNoImplicitDeps()
            .withOutput(OutputFormat.BUILD)
            .build();

        Optional<String> out = bazel.executeToString(queryArgs);
        if (!out.isPresent()) {
            return false;
        }
        boolean found = out.get().contains("maven_coordinates=");
        if (found) {
            logger.debug("Detected rules_jvm_external via j.*import with maven_coordinates.");
        }
        return found;
    }

    /**
     * Detects if maven_jar is used by filtering for @repo//jar:jar labels in the dependency graph.
     * @return true if maven_jar is detected, false otherwise
     */
    private boolean detectMavenJar() throws Exception {
        // Use cquery here too: we want to inspect the action graph for actual jar labels
        // (cquery reduces false positives from statically-declared but not-built labels).
        List<String> queryArgs = BazelQueryBuilder.cquery()
            .filter(MAVEN_JAR_FILTER_PATTERN, BazelQueryBuilder.deps(target))
            .withNoImplicitDeps()
            .build();

        Optional<String> out = bazel.executeToString(queryArgs);
        boolean found = out.isPresent() && !out.get().trim().isEmpty();
        if (found) {
            logger.debug("Detected maven_jar artifacts via @repo//jar:jar labels.");
        }
        return found;
    }

    /**
     * Detects if haskell_cabal_library rules are present in the dependency graph.
     * @return true if haskell_cabal_library is detected, false otherwise
     */
    private boolean detectHaskellCabal() throws Exception {
        // cquery with label kind / JSONPROTO used here because the action graph contains
        // the relevant proto/json information needed to extract library details.
        List<String> queryArgs = BazelQueryBuilder.cquery()
            .kind(HASKELL_CABAL_RULE_PATTERN, BazelQueryBuilder.deps(target))
            .withNoImplicitDeps()
            .withOutput(OutputFormat.LABEL_KIND)
            .build();

        Optional<String> out = bazel.executeToString(queryArgs);
        boolean found = out.isPresent() && out.get().contains(HASKELL_CABAL_RULE_PATTERN);
        if (found) {
            logger.debug("Detected haskell_cabal_library rules.");
        }
        return found;
    }
}
