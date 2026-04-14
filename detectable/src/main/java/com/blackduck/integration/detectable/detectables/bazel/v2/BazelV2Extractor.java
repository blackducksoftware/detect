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
    private static final String JSON_KEY_NAME = "\"name\"";
    private static final String JSON_KEY_VERSION = "\"version\"";
    private static final String JSON_KEY_EXTERNAL_ID = "\"externalId\"";
    private static final String JSON_KEY_FORGE = "\"forge\"";
    private static final String JSON_KEY_PIECES = "\"pieces\"";
    private static final String JSON_KEY_PREFIX = "\"prefix\"";
    private static final String JSON_KEY_SUFFIX = "\"suffix\"";
    private static final String JSON_KEY_SCOPE = "\"scope\"";

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
        logger.info("Starting the Bazel tool extraction. Target: {}. Pipelines: {}", bazelTarget, sources);
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
            if (logger.isDebugEnabled()) {
                logger.debug("Dependencies discovered for source {}: {}", source, dependenciesToDebugString(deps));
            }
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
        return builder.build();
    }

    /**
     * Formats a list of dependencies as a JSON array string for debug logging.
     */
    private String dependenciesToDebugString(List<Dependency> deps) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < deps.size(); i++) {
            Dependency dep = deps.get(i);
            String forge = dep.getExternalId().getForge() != null ? dep.getExternalId().getForge().getName() : null;
            String[] pieces = dep.getExternalId().getExternalIdPieces();
            String piecesJson = pieces != null && pieces.length > 0
                ? "[" + Arrays.stream(pieces).map(BazelV2Extractor::jsonEscape).collect(Collectors.joining(",")) + "]"
                : "[]";
            sb.append("{")
                .append(JSON_KEY_NAME).append(":").append(jsonEscape(dep.getName())).append(",")
                .append(JSON_KEY_VERSION).append(":").append(jsonEscape(dep.getVersion())).append(",")
                .append(JSON_KEY_EXTERNAL_ID).append(":{")
                .append(JSON_KEY_FORGE).append(":").append(jsonEscape(forge)).append(",")
                .append(JSON_KEY_PIECES).append(":").append(piecesJson).append(",")
                .append(JSON_KEY_PREFIX).append(":null,")
                .append(JSON_KEY_SUFFIX).append(":null")
                .append("},")
                .append(JSON_KEY_SCOPE).append(":null")
                .append("}");
            if (i < deps.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Escapes a string value for safe inclusion in JSON output.
     * Returns literal null (unquoted) if the value is null.
     */
    private static String jsonEscape(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
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
