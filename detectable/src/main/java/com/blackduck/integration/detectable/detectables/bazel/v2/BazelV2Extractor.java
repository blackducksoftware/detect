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
 * Executes selected pipelines deterministically and produces an Extraction.
 */
public class BazelV2Extractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExternalIdFactory externalIdFactory;
    private final BazelVariableSubstitutor bazelVariableSubstitutor;
    private final HaskellCabalLibraryJsonProtoParser haskellParser;
    private final BazelProjectNameGenerator projectNameGenerator;

    public BazelV2Extractor(ExternalIdFactory externalIdFactory,
                            BazelVariableSubstitutor bazelVariableSubstitutor,
                            HaskellCabalLibraryJsonProtoParser haskellParser,
                            BazelProjectNameGenerator projectNameGenerator) {
        this.externalIdFactory = externalIdFactory;
        this.bazelVariableSubstitutor = bazelVariableSubstitutor;
        this.haskellParser = haskellParser;
        this.projectNameGenerator = projectNameGenerator;
    }

    public Extraction run(BazelCommandExecutor bazelCmd,
                          Set<WorkspaceRule> rules,
                          String bazelTarget) throws ExecutableFailedException, DetectableException {
        logger.info("Starting Bazel V2 extraction. Target: {}. Pipelines: {}", bazelTarget, rules);
        Pipelines pipelines = new Pipelines(bazelCmd, bazelVariableSubstitutor, externalIdFactory, haskellParser);

        List<WorkspaceRule> ordered = rules.stream()
            .sorted(Comparator.comparingInt(this::priority))
            .collect(Collectors.toList());

        List<Dependency> aggregated = new ArrayList<>();
        for (WorkspaceRule rule : ordered) {
            logger.info("Executing pipeline for rule: {}", rule);
            List<Dependency> deps = pipelines.get(rule).run();
            logger.info("Pipeline {} produced {} dependencies.", rule, deps.size());
            aggregated.addAll(deps);
        }

        DependencyGraph graph = new BasicDependencyGraph();
        graph.addChildrenToRoot(aggregated);
        CodeLocation cl = new CodeLocation(graph);
        String projectName = projectNameGenerator.generateFromBazelTarget(bazelTarget);
        Extraction.Builder builder = new Extraction.Builder()
            .success(java.util.Collections.singletonList(cl))
            .projectName(projectName);
        logger.info("Bazel V2 extraction complete. Project name: {}. Total dependencies: {}", projectName, aggregated.size());
        return builder.build();
    }

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

