package com.blackduck.integration.detectable.detectables.bazel.pipeline;

import java.util.Arrays;
import java.util.EnumMap;

import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.bazel.WorkspaceRule;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelVariableSubstitutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.HaskellCabalLibraryJsonProtoParser;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.IntermediateStepExecuteShowRepoHeuristic;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.xpathquery.HttpArchiveXpath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pipelines {
    private static final String CQUERY_OPTIONS_PLACEHOLDER = "${detect.bazel.cquery.options}";
    private static final String QUERY_COMMAND = "query";
    private static final String CQUERY_COMMAND = "cquery";
    private static final String OUTPUT_FLAG = "--output";
    private final EnumMap<WorkspaceRule, Pipeline> availablePipelines = new EnumMap<>(WorkspaceRule.class);
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Pipelines(
        BazelCommandExecutor bazelCommandExecutor,
        BazelVariableSubstitutor bazelVariableSubstitutor,
        ExternalIdFactory externalIdFactory,
        HaskellCabalLibraryJsonProtoParser haskellCabalLibraryJsonProtoParser
    ) {
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
        availablePipelines.put(WorkspaceRule.MAVEN_JAR, mavenJarPipeline);

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
        availablePipelines.put(WorkspaceRule.MAVEN_INSTALL, mavenInstallPipeline);

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
        availablePipelines.put(WorkspaceRule.HASKELL_CABAL_LIBRARY, haskellCabalLibraryPipeline);

        // Era detection: prefer authoritative bazel info; fallback to probing bzlmod/mod and //external availability.
        boolean bzlmodActive = false;
        try {
            java.util.Optional<String> infoOut = bazelCommandExecutor.executeToString(java.util.Arrays.asList("info", "--show_make_env"));
            if (infoOut.isPresent()) {
                String info = infoOut.get();
                bzlmodActive = info.contains("ENABLE_BZLMOD=1");
            }
            // If not decided yet, try a cheap mod probe; if this succeeds, bzlmod is active
            if (!bzlmodActive) {
                try {
                    java.util.Optional<String> modProbe = bazelCommandExecutor.executeToString(java.util.Arrays.asList("mod", "show_repo", "@bazel_tools"));
                    if (modProbe.isPresent()) {
                        bzlmodActive = true;
                    }
                } catch (Exception ignoredMod) {
                    // ignore; may not be bzlmod or command unsupported
                }
            }
            // If still not decided, probe //external; any failure indicates bzlmod
            if (!bzlmodActive) {
                try {
                    bazelCommandExecutor.executeToString(java.util.Arrays.asList(QUERY_COMMAND, "kind(.*, //external:bazel_tools)", OUTPUT_FLAG, "xml"));
                } catch (Exception any) {
                    bzlmodActive = true; // treat any non-zero/exception as bzlmod active
                }
            }
        } catch (Exception ignored) {
            // If we cannot detect, default to legacy; pipeline remains robust.
        }

        logger.info("HTTP pipeline variant: {}", bzlmodActive ? "bzlmod" : "legacy");

        if (bzlmodActive) {
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
                // Heuristic-only show_repo execution (no mapping)
                .addIntermediateStep(new IntermediateStepExecuteShowRepoHeuristic(
                    bazelCommandExecutor
                ))
                // Robust parsing of show_repo attributes and synthesis for go_repository
                .parseShowRepoToUrlCandidates()
                .transformGithubUrl()
                .build();
            availablePipelines.put(WorkspaceRule.HTTP_ARCHIVE, httpArchiveBzlmodPipeline);
        } else {
            // Legacy WORKSPACE HTTP pipeline (existing behavior using //external), but exclude known repos
            Pipeline httpArchiveGithubUrlPipeline = (new PipelineBuilder(externalIdFactory, bazelCommandExecutor, bazelVariableSubstitutor, haskellCabalLibraryJsonProtoParser))
                .executeBazelOnEachLine(Arrays.asList(QUERY_COMMAND, "kind(.*library, deps(${detect.bazel.target}))"), false)
                .parseSplitEachLine("\r?\n")
                .parseFilterLines("^@.*//.*$")
                .parseReplaceInEachLine("^@", "")
                .parseReplaceInEachLine("//.*", "")
                .deDupLines()
                .parseFilterLines("^(?!(bazel_tools|platforms|remotejdk|local_config_.*|rules_python|rules_java|rules_cc|maven|unpinned_maven|rules_jvm_external)).*$")
                .parseReplaceInEachLine("^", "//external:")
                .executeBazelOnEachLine(Arrays.asList(QUERY_COMMAND, "kind(.*, ${input.item})", OUTPUT_FLAG, "xml"), true)
                .parseValuesFromXml(HttpArchiveXpath.QUERY, "value")
                .transformGithubUrl()
                .build();
            availablePipelines.put(WorkspaceRule.HTTP_ARCHIVE, httpArchiveGithubUrlPipeline);
        }
    }

    public Pipeline get(WorkspaceRule bazelDependencyType) throws DetectableException {
        if (!availablePipelines.containsKey(bazelDependencyType)) {
            throw new DetectableException(String.format("No pipeline found for dependency type %s", bazelDependencyType.getName()));
        }
        return availablePipelines.get(bazelDependencyType);
    }
}
