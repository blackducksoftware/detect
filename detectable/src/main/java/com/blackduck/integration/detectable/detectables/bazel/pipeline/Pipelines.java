package com.blackduck.integration.detectable.detectables.bazel.pipeline;

import java.util.Arrays;
import java.util.EnumMap;

import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.bazel.DependencySource;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelVariableSubstitutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.HaskellCabalLibraryJsonProtoParser;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.IntermediateStepExecuteShowRepoHeuristic;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.xpathquery.HttpArchiveXpath;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelEnvironmentAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages and constructs dependency extraction pipelines for each supported Bazel DependencySource.
 * Each pipeline defines a sequence of Bazel commands and parsing steps to extract dependencies for a rule.
 * Pipelines are selected and constructed based on the Bazel environment (workspace or bzlmod).
 */
public class Pipelines {
    // Placeholder for cquery options in command templates
    private static final String CQUERY_OPTIONS_PLACEHOLDER = "${detect.bazel.cquery.options}";
    // Bazel query and cquery command names
    private static final String QUERY_COMMAND = "query";
    private static final String CQUERY_COMMAND = "cquery";
    // Output flag for Bazel commands
    private static final String OUTPUT_FLAG = "--output";
    // Map of available pipelines by DependencySource
    private final EnumMap<DependencySource, Pipeline> availablePipelines = new EnumMap<>(DependencySource.class);
    // Logger for this class
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Constructs pipelines and auto-detects Bazel mode for legacy callers.
     */
    public Pipelines(
        BazelCommandExecutor bazelCommandExecutor,
        BazelVariableSubstitutor bazelVariableSubstitutor,
        ExternalIdFactory externalIdFactory,
        HaskellCabalLibraryJsonProtoParser haskellCabalLibraryJsonProtoParser
    ) {
        // Auto-detect mode for legacy callers and delegate to the mode-aware constructor.
        BazelEnvironmentAnalyzer analyzer = new BazelEnvironmentAnalyzer(bazelCommandExecutor);
        BazelEnvironmentAnalyzer.Mode mode = analyzer.getMode();
        this.init(bazelCommandExecutor, bazelVariableSubstitutor, externalIdFactory, haskellCabalLibraryJsonProtoParser, mode);
    }

    /**
     * Constructs pipelines for the specified Bazel mode.
     */
    public Pipelines(
        BazelCommandExecutor bazelCommandExecutor,
        BazelVariableSubstitutor bazelVariableSubstitutor,
        ExternalIdFactory externalIdFactory,
        HaskellCabalLibraryJsonProtoParser haskellCabalLibraryJsonProtoParser,
        BazelEnvironmentAnalyzer.Mode mode
    ) {
        this.init(bazelCommandExecutor, bazelVariableSubstitutor, externalIdFactory, haskellCabalLibraryJsonProtoParser, mode);
    }

    /**
     * Initializes and registers all supported pipelines for each DependencySource.
     * Pipelines are constructed as sequences of Bazel commands and parsing steps.
     * The HTTP_ARCHIVE pipeline is selected based on the Bazel mode (bzlmod or WORKSPACE).
     */
    private void init(BazelCommandExecutor bazelCommandExecutor,
                      BazelVariableSubstitutor bazelVariableSubstitutor,
                      ExternalIdFactory externalIdFactory,
                      HaskellCabalLibraryJsonProtoParser haskellCabalLibraryJsonProtoParser,
                      BazelEnvironmentAnalyzer.Mode mode) {
        // Pipeline for maven_jar: extracts Maven dependencies from maven_jar rules
        Pipeline mavenJarPipeline = (new PipelineBuilder(externalIdFactory, bazelCommandExecutor, bazelVariableSubstitutor, haskellCabalLibraryJsonProtoParser))
            .executeBazelOnEachLine(Arrays.asList(CQUERY_COMMAND, CQUERY_OPTIONS_PLACEHOLDER, "filter('@.*:jar', deps(${detect.bazel.target}))"), false)
            // The trailing parens may contain a hex number, or "null"; the pattern below handles either
            .parseReplaceInEachLine(" \\([0-9a-z]+\\)", "")
            .parseSplitEachLine("\\s+")
            .parseReplaceInEachLine("^@", "")
            .parseReplaceInEachLine("//.*", "")
            .parseReplaceInEachLine("^", "//external:")
            .executeBazelOnEachLine(Arrays.asList(QUERY_COMMAND, "kind(maven_jar, ${input.item})", OUTPUT_FLAG, "xml"), true)
            .parseValuesFromXml("/query/rule[@class='maven_jar']/string[@name='artifact']", "value")
            .transformToMavenDependencies()
            .build();
        availablePipelines.put(DependencySource.MAVEN_JAR, mavenJarPipeline);

        // Pipeline for rules_jvm_external (maven_install): extracts Maven dependencies from j.*import rules
        Pipeline mavenInstallPipeline = (new PipelineBuilder(externalIdFactory, bazelCommandExecutor, bazelVariableSubstitutor, haskellCabalLibraryJsonProtoParser))
            .executeBazelOnEachLine(Arrays.asList(
                CQUERY_COMMAND,
                "--noimplicit_deps",
                CQUERY_OPTIONS_PLACEHOLDER,
                "kind(j.*import, deps(${detect.bazel.target}))",
                OUTPUT_FLAG,
                "build"
            ), false)
            .parseSplitEachLine("\r?\n")
            .parseFilterLines(".*maven_coordinates=.*")
            .parseReplaceInEachLine(".*\"maven_coordinates=", "")
            .parseReplaceInEachLine("\".*", "")
            .transformToMavenDependencies()
            .build();
        availablePipelines.put(DependencySource.MAVEN_INSTALL, mavenInstallPipeline);

        // Pipeline for haskell_cabal_library: extracts Hackage dependencies from Haskell cabal library rules
        Pipeline haskellCabalLibraryPipeline = (new PipelineBuilder(externalIdFactory, bazelCommandExecutor, bazelVariableSubstitutor, haskellCabalLibraryJsonProtoParser))
            .executeBazelOnEachLine(Arrays.asList(
                CQUERY_COMMAND,
                "--noimplicit_deps",
                CQUERY_OPTIONS_PLACEHOLDER,
                "kind(haskell_cabal_library, deps(${detect.bazel.target}))",
                OUTPUT_FLAG,
                "jsonproto"
            ), false)
            .transformToHackageDependencies()
            .build();
        availablePipelines.put(DependencySource.HASKELL_CABAL_LIBRARY, haskellCabalLibraryPipeline);

        // Select HTTP_ARCHIVE pipeline variant based on Bazel mode
        boolean bzlmodActive = (mode == BazelEnvironmentAnalyzer.Mode.BZLMOD);
        logger.info("HTTP pipeline variant: {}", bzlmodActive ? "bzlmod" : "workspace");

        if (bzlmodActive) {
            // bzlmod: Use robust show_repo parser for HTTP pipeline
            logger.info("Using robust show_repo parser for bzlmod HTTP pipeline.");
            Pipeline httpArchiveBzlmodPipeline = (new PipelineBuilder(externalIdFactory, bazelCommandExecutor, bazelVariableSubstitutor, haskellCabalLibraryJsonProtoParser))
                .executeBazelOnEachLine(Arrays.asList(QUERY_COMMAND, "kind(.*library, deps(${detect.bazel.target}))"), false)
                .parseSplitEachLine("\r?\n")
                .parseFilterLines("^@.*//.*$")
                .parseReplaceInEachLine("^@+", "")
                .parseReplaceInEachLine("//.*", "")
                .deDupLines()
                .parseFilterLines("^(?!(bazel_tools|platforms|remotejdk|local_config_.*|rules_python|rules_java|rules_cc|maven|unpinned_maven|rules_jvm_external)).*$")
                .parseReplaceInEachLine("^", "@")
                // Add intermediate step to run 'bazel mod show_repo' for each repo
                .addIntermediateStep(new IntermediateStepExecuteShowRepoHeuristic(
                    bazelCommandExecutor
                ))
                .parseShowRepoToUrlCandidates()
                .transformGithubUrl()
                .build();
            availablePipelines.put(DependencySource.HTTP_ARCHIVE, httpArchiveBzlmodPipeline);
        } else {
            // WORKSPACE: Use XML parsing pipeline for HTTP pipeline
            Pipeline httpArchiveGithubUrlPipeline = (new PipelineBuilder(externalIdFactory, bazelCommandExecutor, bazelVariableSubstitutor, haskellCabalLibraryJsonProtoParser))
                    .executeBazelOnEachLine(Arrays.asList(QUERY_COMMAND, "kind(.*library, deps(${detect.bazel.target}))"), false)
                    .parseSplitEachLine("\r?\n")
                    .parseFilterLines("^@.*//.*$")
                    .parseReplaceInEachLine("^@", "")
                    .parseReplaceInEachLine("//.*", "")
                    .deDupLines()
                    .parseReplaceInEachLine("^", "//external:")
                    .executeBazelOnEachLine(Arrays.asList(QUERY_COMMAND, "kind(.*, ${input.item})", OUTPUT_FLAG, "xml"), true)
                    .parseValuesFromXml(HttpArchiveXpath.QUERY, "value")
                    .transformGithubUrl()
                    .build();
            availablePipelines.put(DependencySource.HTTP_ARCHIVE, httpArchiveGithubUrlPipeline); // add the pipeline to the available pipelines
        }
    }

    /**
     * Returns the pipeline for the given DependencySource, or throws if not found.
     * @param bazelDependencyType The DependencySource to get the pipeline for
     * @return The Pipeline for the given source
     * @throws DetectableException if no pipeline is found for the source
     */
    public Pipeline get(DependencySource bazelDependencyType) throws DetectableException {
        if (!availablePipelines.containsKey(bazelDependencyType)) {
            throw new DetectableException(String.format("No pipeline found for dependency type %s", bazelDependencyType.getName()));
        }
        return availablePipelines.get(bazelDependencyType);
    }
}
