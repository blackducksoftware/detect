package com.blackduck.integration.detectable.detectables.bazel.pipeline;

import java.util.EnumMap;
import java.util.List;

import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.bazel.DependencySource;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelVariableSubstitutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.HaskellCabalLibraryJsonProtoParser;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.IntermediateStepExecuteShowRepoHeuristic;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.xpathquery.HttpArchiveXpath;
import com.blackduck.integration.detectable.detectables.bazel.query.BazelQueryBuilder;
import com.blackduck.integration.detectable.detectables.bazel.query.OutputFormat;
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

    // Bazel rule kind patterns for pipeline queries
    private static final String JAVA_IMPORT_RULE_PATTERN = "j.*import";
    private static final String MAVEN_JAR_FILTER_PATTERN = "'@.*:jar'";
    private static final String HASKELL_CABAL_RULE_PATTERN = "haskell_cabal_library";
    private static final String LIBRARY_RULE_PATTERN = ".*library";
    private static final String ANY_RULE_PATTERN = ".*";
    private static final String MAVEN_JAR_RULE_PATTERN = "maven_jar";

    // Parse/regex literals for pipeline processing
    private static final String SPLIT_NEWLINE_REGEX = "\\r?\\n";
    private static final String SPLIT_WHITESPACE_REGEX = "\\s+";
    private static final String MAVEN_COORDINATES_FILTER = ".*maven_coordinates=.*";
    private static final String MAVEN_COORDINATES_PREFIX_REMOVE = ".*\"maven_coordinates=";
    private static final String TRAILING_QUOTE_REMOVE = "\".*";
    private static final String TRAILING_PARENS_REGEX = " \\([0-9a-z]+\\)";

    // HTTP / repo parsing regexes
    private static final String HTTP_REPO_FILTER_REGEX = "^@.*//.*$";
    private static final String STRIP_LEADING_ATS_REGEX = "^@+";
    private static final String STRIP_SINGLE_AT_REGEX = "^@";
    private static final String STRIP_REPO_PATH_REGEX = "//.*";
    private static final String EXCLUDE_BUILTINS_REGEX = "^(?!(bazel_tools|platforms|remotejdk|local_config_.*|rules_python|rules_java|rules_cc|maven|unpinned_maven|rules_jvm_external)).*$";
    private static final String PREPEND_AT = "@";
    private static final String PREPEND_EXTERNAL = "//external:";

    // Map of available pipelines by DependencySource
    private final EnumMap<DependencySource, Pipeline> availablePipelines = new EnumMap<>(DependencySource.class);
    // Logger for this class
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Constructs pipelines and auto-detects Bazel mode for legacy callers.
     * Deprecated in favor of the mode-aware constructor.
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
        List<String> mavenJarCquery = BazelQueryBuilder.cquery()
            .filter(MAVEN_JAR_FILTER_PATTERN, BazelQueryBuilder.deps("${detect.bazel.target}"))
            .withOptions(CQUERY_OPTIONS_PLACEHOLDER)
            .build();

        Pipeline mavenJarPipeline = (new PipelineBuilder(externalIdFactory, bazelCommandExecutor, bazelVariableSubstitutor, haskellCabalLibraryJsonProtoParser))
            .executeBazelOnEachLine(mavenJarCquery, false)
            // The trailing parens may contain a hex number, or "null"; the pattern below handles either
            .parseReplaceInEachLine(TRAILING_PARENS_REGEX, "")
            .parseSplitEachLine(SPLIT_WHITESPACE_REGEX)
            .parseReplaceInEachLine(STRIP_SINGLE_AT_REGEX, "")
            .parseReplaceInEachLine(STRIP_REPO_PATH_REGEX, "")
            .parseReplaceInEachLine("^", PREPEND_EXTERNAL)
            .executeBazelOnEachLine(
                BazelQueryBuilder.query()
                    .kind(MAVEN_JAR_RULE_PATTERN, "${input.item}")
                    .withOutput(OutputFormat.XML)
                    .build(),
                true)
            .parseValuesFromXml("/query/rule[@class='maven_jar']/string[@name='artifact']", "value")
            .transformToMavenDependencies()
            .build();
        availablePipelines.put(DependencySource.MAVEN_JAR, mavenJarPipeline);

        // Pipeline for rules_jvm_external (maven_install): extracts Maven dependencies from j.*import rules
        List<String> mavenInstallCquery = BazelQueryBuilder.cquery()
            .kind(JAVA_IMPORT_RULE_PATTERN, BazelQueryBuilder.deps("${detect.bazel.target}"))
            .withNoImplicitDeps()
            .withOptions(CQUERY_OPTIONS_PLACEHOLDER)
            .withOutput(OutputFormat.BUILD)
            .build();

        Pipeline mavenInstallPipeline = (new PipelineBuilder(externalIdFactory, bazelCommandExecutor, bazelVariableSubstitutor, haskellCabalLibraryJsonProtoParser))
            .executeBazelOnEachLine(mavenInstallCquery, false)
            .parseSplitEachLine(SPLIT_NEWLINE_REGEX)
            .parseFilterLines(MAVEN_COORDINATES_FILTER)
            .parseReplaceInEachLine(MAVEN_COORDINATES_PREFIX_REMOVE, "")
            .parseReplaceInEachLine(TRAILING_QUOTE_REMOVE, "")
            .transformToMavenDependencies()
            .build();
        availablePipelines.put(DependencySource.MAVEN_INSTALL, mavenInstallPipeline);

        // Pipeline for haskell_cabal_library: extracts Hackage dependencies from Haskell cabal library rules
        List<String> haskellCquery = BazelQueryBuilder.cquery()
            .kind(HASKELL_CABAL_RULE_PATTERN, BazelQueryBuilder.deps("${detect.bazel.target}"))
            .withNoImplicitDeps()
            .withOptions(CQUERY_OPTIONS_PLACEHOLDER)
            .withOutput(OutputFormat.JSONPROTO)
            .build();

        Pipeline haskellCabalLibraryPipeline = (new PipelineBuilder(externalIdFactory, bazelCommandExecutor, bazelVariableSubstitutor, haskellCabalLibraryJsonProtoParser))
            .executeBazelOnEachLine(haskellCquery, false)
            .transformToHackageDependencies()
            .build();
        availablePipelines.put(DependencySource.HASKELL_CABAL_LIBRARY, haskellCabalLibraryPipeline);

        // Select HTTP_ARCHIVE pipeline variant based on Bazel mode
        boolean bzlmodActive = (mode == BazelEnvironmentAnalyzer.Mode.BZLMOD);
        logger.info("HTTP pipeline variant: {}", bzlmodActive ? "bzlmod" : "workspace");

        if (bzlmodActive) {
            // bzlmod: Use robust show_repo parser for HTTP pipeline
            logger.info("Using robust show_repo parser for bzlmod HTTP pipeline.");

            List<String> httpLibraryQuery = BazelQueryBuilder.query()
                .kind(LIBRARY_RULE_PATTERN, BazelQueryBuilder.deps("${detect.bazel.target}"))
                .build();

            Pipeline httpArchiveBzlmodPipeline = (new PipelineBuilder(externalIdFactory, bazelCommandExecutor, bazelVariableSubstitutor, haskellCabalLibraryJsonProtoParser))
                .executeBazelOnEachLine(httpLibraryQuery, false)
                .parseSplitEachLine(SPLIT_NEWLINE_REGEX)
                .parseFilterLines(HTTP_REPO_FILTER_REGEX)
                .parseReplaceInEachLine(STRIP_LEADING_ATS_REGEX, "")
                .parseReplaceInEachLine(STRIP_REPO_PATH_REGEX, "")
                .deDupLines()
                .parseFilterLines(EXCLUDE_BUILTINS_REGEX)
                .parseReplaceInEachLine("^", PREPEND_AT)
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
            List<String> httpLibraryQuery = BazelQueryBuilder.query()
                .kind(LIBRARY_RULE_PATTERN, BazelQueryBuilder.deps("${detect.bazel.target}"))
                .build();

            Pipeline httpArchiveGithubUrlPipeline = (new PipelineBuilder(externalIdFactory, bazelCommandExecutor, bazelVariableSubstitutor, haskellCabalLibraryJsonProtoParser))
                    .executeBazelOnEachLine(httpLibraryQuery, false)
                    .parseSplitEachLine(SPLIT_NEWLINE_REGEX)
                    .parseFilterLines(HTTP_REPO_FILTER_REGEX)
                    .parseReplaceInEachLine(STRIP_SINGLE_AT_REGEX, "")
                    .parseReplaceInEachLine(STRIP_REPO_PATH_REGEX, "")
                    .deDupLines()
                    .parseReplaceInEachLine("^", PREPEND_EXTERNAL)
                    .executeBazelOnEachLine(
                        BazelQueryBuilder.query()
                            .kind(ANY_RULE_PATTERN, "${input.item}")
                            .withOutput(OutputFormat.XML)
                            .build(),
                        true)
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
