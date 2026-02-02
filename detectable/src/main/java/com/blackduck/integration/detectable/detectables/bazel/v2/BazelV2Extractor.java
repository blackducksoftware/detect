package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectables.bazel.DependencySource;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.Pipelines;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelVariableSubstitutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.HaskellCabalLibraryJsonProtoParser;
import com.blackduck.integration.detectable.detectables.bazel.BazelProjectNameGenerator;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.extraction.Extraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
     * Runs the extraction process for the given Bazel target and set of dependency sources.
     * Executes each pipeline in a deterministic order, aggregates dependencies, and builds the Extraction result.
     * @param bazelCmd Bazel command executor
     * @param sources Set of dependency sources to execute
     * @param bazelTarget Bazel target to analyze
     * @param mode Bazel environment mode (for HTTP variant selection)
     * @return Extraction result containing discovered dependencies and project name
     * @throws ExecutableFailedException if a Bazel command fails
     * @throws DetectableException if extraction fails
     */
    public Extraction run(BazelCommandExecutor bazelCmd,
                          Set<DependencySource> sources,
                          String bazelTarget,
                          BazelEnvironmentAnalyzer.Mode mode) throws ExecutableFailedException, DetectableException {
        logger.info("Starting Bazel V2 extraction. Target: {}. Pipelines: {}", bazelTarget, sources);
        // Create pipelines for each dependency source
        Pipelines pipelines = new Pipelines(bazelCmd, bazelVariableSubstitutor, externalIdFactory, haskellParser, mode);

        // Sort sources by priority for deterministic execution
        List<DependencySource> ordered = sources.stream()
            .sorted(Comparator.comparingInt(this::priority))
            .collect(Collectors.toList());

        List<Dependency> aggregated = new ArrayList<>();
        // Execute each pipeline and aggregate discovered dependencies
        for (DependencySource source : ordered) {
            logger.info("Executing pipeline for dependency source: {}", source);
            List<Dependency> deps = pipelines.get(source).run();
            logger.info("Number of dependencies discovered for source {}: {}", source, deps.size());
            aggregated.addAll(deps);
        }

        // Build the dependency graph and code location
        DependencyGraph graph = new BasicDependencyGraph();
        graph.addChildrenToRoot(aggregated);
        CodeLocation cl = new CodeLocation(graph);
        // Generate project name from Bazel target
        String projectName = projectNameGenerator.generateFromBazelTarget(bazelTarget);
        Extraction.Builder builder = new Extraction.Builder()
            .success(Collections.singletonList(cl))
            .projectName(projectName);
        logger.info("Bazel V2 extraction complete. Project name: {}. Total dependencies: {}", projectName, aggregated.size());
        return builder.build();
    }

    /**
     * Determines the execution priority for each dependency source.
     * Lower number means earlier execution.
     * @param source DependencySource to prioritize
     * @return Priority value
     */
    private int priority(DependencySource source) {
        // Lower number = earlier execution
        switch (source) {
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
