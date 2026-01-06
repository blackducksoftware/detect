package com.blackduck.integration.detectable.detectables.bazel.pipeline;

import java.util.ArrayList;
import java.util.List;

import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelVariableSubstitutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.FinalStep;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.FinalStepTransformColonSeparatedGavsToMaven;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.FinalStepTransformGithubUrl;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.FinalStepTransformJsonProtoHaskellCabalLibrariesToHackage;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.HaskellCabalLibraryJsonProtoParser;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.IntermediateStep;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.IntermediateStepDeDupLines;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.IntermediateStepExecuteBazelOnEachLine;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.IntermediateStepParseFilterLines;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.IntermediateStepParseReplaceInEachLine;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.IntermediateStepParseShowRepoToUrlCandidates;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.IntermediateStepParseSplitEach;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.IntermediateStepParseValuesFromXml;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.parse.GithubUrlParser;

public class PipelineBuilder {
    private final ExternalIdFactory externalIdFactory;
    private final BazelCommandExecutor bazelCommandExecutor;
    private final BazelVariableSubstitutor bazelVariableSubstitutor;
    private final HaskellCabalLibraryJsonProtoParser haskellCabalLibraryJsonProtoParser;
    private final List<IntermediateStep> intermediateSteps = new ArrayList<>();
    private FinalStep finalStep;

    public PipelineBuilder(
        ExternalIdFactory externalIdFactory,
        BazelCommandExecutor bazelCommandExecutor,
        BazelVariableSubstitutor bazelVariableSubstitutor,
        HaskellCabalLibraryJsonProtoParser haskellCabalLibraryJsonProtoParser
    ) {
        this.externalIdFactory = externalIdFactory;
        this.bazelCommandExecutor = bazelCommandExecutor;
        this.bazelVariableSubstitutor = bazelVariableSubstitutor;
        this.haskellCabalLibraryJsonProtoParser = haskellCabalLibraryJsonProtoParser;
    }

    public PipelineBuilder addIntermediateStep(IntermediateStep intermediateStep) {
        intermediateSteps.add(intermediateStep);
        return this;
    }

    public PipelineBuilder setFinalStep(FinalStep finalStep) {
        this.finalStep = finalStep;
        return this;
    }

    public Pipeline build() {
        if (finalStep == null) {
            throw new UnsupportedOperationException("A final step is required");
        }
        return new Pipeline(intermediateSteps, finalStep);
    }

    // Step creation methods
    public PipelineBuilder parseReplaceInEachLine(String from, String to) {
        return addIntermediateStep(new IntermediateStepParseReplaceInEachLine(from, to));
    }

    public PipelineBuilder deDupLines() {
        return addIntermediateStep(new IntermediateStepDeDupLines());
    }

    public PipelineBuilder parseSplitEachLine(String splitOn) {
        return addIntermediateStep(new IntermediateStepParseSplitEach(splitOn));
    }

    public PipelineBuilder executeBazelOnEachLine(List<String> bazelArguments, boolean inputIsExpected) {
        return addIntermediateStep(new IntermediateStepExecuteBazelOnEachLine(bazelCommandExecutor, bazelVariableSubstitutor, bazelArguments, inputIsExpected));
    }

    public PipelineBuilder parseValuesFromXml(String xPathToElement, String targetAttributeName) {
        return addIntermediateStep(new IntermediateStepParseValuesFromXml(xPathToElement, targetAttributeName));
    }

    public PipelineBuilder parseFilterLines(String regex) {
        return addIntermediateStep(new IntermediateStepParseFilterLines(regex));
    }

    // A transform step must be the final step in the pipeline
    public PipelineBuilder transformToMavenDependencies() {
        return setFinalStep(new FinalStepTransformColonSeparatedGavsToMaven(externalIdFactory));
    }

    public PipelineBuilder transformToHackageDependencies() {
        return setFinalStep(new FinalStepTransformJsonProtoHaskellCabalLibrariesToHackage(haskellCabalLibraryJsonProtoParser, externalIdFactory));
    }

    public PipelineBuilder transformGithubUrl() {
        return setFinalStep(new FinalStepTransformGithubUrl(externalIdFactory, new GithubUrlParser()));
    }

    public PipelineBuilder parseShowRepoToUrlCandidates() {
        return addIntermediateStep(new IntermediateStepParseShowRepoToUrlCandidates());
    }
}
