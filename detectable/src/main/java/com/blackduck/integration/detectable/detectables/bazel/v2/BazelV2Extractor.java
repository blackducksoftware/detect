package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectables.bazel.WorkspaceRule;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.Pipelines;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelVariableSubstitutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.HaskellCabalLibraryJsonProtoParser;
import com.blackduck.integration.detectable.detectables.bazel.BazelProjectNameGenerator;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.extraction.Extraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Executes selected Bazel dependency pipelines deterministically and produces an Extraction.
 */
public class BazelV2Extractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExternalIdFactory externalIdFactory;
    private final BazelVariableSubstitutor bazelVariableSubstitutor;
    private final HaskellCabalLibraryJsonProtoParser haskellParser;
    private final BazelProjectNameGenerator projectNameGenerator;

    /**
     * Constructor for BazelV2Extractor
     * @param externalIdFactory Factory for creating ExternalId objects
     * @param bazelVariableSubstitutor Substitutes variables in Bazel commands
     * @param haskellParser Parses Haskell cabal library JSON proto output
     * @param projectNameGenerator Generates project names from Bazel targets
     */
    public BazelV2Extractor(ExternalIdFactory externalIdFactory,
                            BazelVariableSubstitutor bazelVariableSubstitutor,
                            HaskellCabalLibraryJsonProtoParser haskellParser,
                            BazelProjectNameGenerator projectNameGenerator) {
        this.externalIdFactory = externalIdFactory;
        this.bazelVariableSubstitutor = bazelVariableSubstitutor;
        this.haskellParser = haskellParser;
        this.projectNameGenerator = projectNameGenerator;
    }

    /**
     * Runs the extraction process for the given Bazel target and set of workspace rules.
     * Executes each pipeline in a deterministic order, aggregates dependencies, and builds the Extraction result.
     * @param bazelCmd Bazel command executor
     * @param rules Set of workspace rules to execute
     * @param bazelTarget Bazel target to analyze
     * @param mode Bazel environment mode (for HTTP variant selection)
     * @return Extraction result containing discovered dependencies and project name
     * @throws ExecutableFailedException if a Bazel command fails
     * @throws DetectableException if extraction fails
     */
    public Extraction run(BazelCommandExecutor bazelCmd,
                          Set<WorkspaceRule> rules,
                          String bazelTarget,
                          BazelEnvironmentAnalyzer.Mode mode) throws ExecutableFailedException, DetectableException {
        logger.info("Starting Bazel V2 extraction. Target: {}. Pipelines: {}", bazelTarget, rules);
        // Create pipelines for each workspace rule
        Pipelines pipelines = new Pipelines(bazelCmd, bazelVariableSubstitutor, externalIdFactory, haskellParser, mode);

        // Sort rules by priority for deterministic execution
        List<WorkspaceRule> ordered = rules.stream()
            .sorted(Comparator.comparingInt(this::priority))
            .collect(Collectors.toList());

        List<Dependency> aggregated = new ArrayList<>();
        // Execute each pipeline and aggregate discovered dependencies
        for (WorkspaceRule rule : ordered) {
            logger.info("Executing pipeline for rule: {}", rule);
            List<Dependency> deps = pipelines.get(rule).run();
            logger.info("Number of dependencies discovered for rule {}: {}", rule, deps.size());
            aggregated.addAll(deps);
        }

        // Build the dependency graph and code location
        DependencyGraph graph = new BasicDependencyGraph();
        graph.addChildrenToRoot(aggregated);
        CodeLocation cl = new CodeLocation(graph);
        // Generate project name from Bazel target
        String projectName = projectNameGenerator.generateFromBazelTarget(bazelTarget);
        Extraction.Builder builder = new Extraction.Builder()
            .success(java.util.Collections.singletonList(cl))
            .projectName(projectName);
        logger.info("Bazel V2 extraction complete. Project name: {}. Total dependencies: {}", projectName, aggregated.size());
        return builder.build();
    }

    /**
     * Determines the execution priority for each workspace rule.
     * Lower number means earlier execution.
     * @param rule WorkspaceRule to prioritize
     * @return Priority value
     */
    private int priority(WorkspaceRule rule) {
        // Lower number = earlier execution
        switch (rule) {
            case MAVEN_INSTALL:
                return 0;
            case MAVEN_JAR:
                return 1;
            case HASKELL_CABAL_LIBRARY:
                return 2;
            case HTTP_ARCHIVE:
                return 3;
            default:
                return 100;
        }
    }
}
