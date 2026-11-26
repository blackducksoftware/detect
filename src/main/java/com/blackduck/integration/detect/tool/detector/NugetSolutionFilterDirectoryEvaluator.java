package com.blackduck.integration.detect.tool.detector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectables.nuget.NugetProjectDetectable;
import com.blackduck.integration.detectable.detectables.nuget.NugetProjectInspectorDetectable;
import com.blackduck.integration.detectable.detectables.nuget.NugetSolutionDetectable;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.detector.accuracy.detectable.DetectableEvaluationResult;
import com.blackduck.integration.detector.accuracy.detectable.DetectableEvaluator;
import com.blackduck.integration.detector.accuracy.detectable.DetectableExclusionEvaluator;
import com.blackduck.integration.detector.accuracy.directory.DirectoryEvaluation;
import com.blackduck.integration.detector.accuracy.directory.DirectoryEvaluator;
import com.blackduck.integration.detector.accuracy.entrypoint.DetectorRuleEvaluation;
import com.blackduck.integration.detector.accuracy.entrypoint.DetectorRuleEvaluator;
import com.blackduck.integration.detector.accuracy.entrypoint.EntryPointEvaluation;
import com.blackduck.integration.detector.accuracy.entrypoint.EntryPointFoundResult;
import com.blackduck.integration.detector.accuracy.search.SearchEvaluator;
import com.blackduck.integration.detector.accuracy.search.SearchOptions;
import com.blackduck.integration.detector.base.DetectorEvaluationUtil;
import com.blackduck.integration.detector.base.DetectorType;
import com.blackduck.integration.detector.finder.DirectoryFindResult;
import com.blackduck.integration.detector.rule.DetectorRuleSet;

/**
 * Used only when detect.nuget.solution.filter.path is set. The filter limits the NuGet Inspector to the source directory, so the
 * NuGet Project Inspector fallback would only ever see that one directory. If the filtered run does not succeed, NuGet is evaluated
 * a second time with the NuGet Inspector switched off, so that the NuGet Project Inspector is applied where it would have been had the
 * NuGet Inspector failed without a solution filter. The failed filter run is reported in each directory the Project Inspector ran in,
 * as the failed NuGet Inspector run would be without a solution filter.
 */
public class NugetSolutionFilterDirectoryEvaluator extends DirectoryEvaluator {
    private static final String NUGET_SOLUTION_INSPECTOR_NAME = nameOf(NugetSolutionDetectable.class);
    private static final String NUGET_PROJECT_NATIVE_INSPECTOR_NAME = nameOf(NugetProjectDetectable.class);
    private static final String NUGET_PROJECT_INSPECTOR_NAME = nameOf(NugetProjectInspectorDetectable.class);

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final FallbackDirectoryEvaluator fallbackEvaluator;

    public NugetSolutionFilterDirectoryEvaluator(
        SearchOptions searchOptions,
        List<String> excludedDetectors,
        Function<DetectorType, ExtractionEnvironment> extractionEnvironmentSupplier
    ) {
        // The first pass never runs the Project Inspector itself; it is run by the second pass so it sees the same directories as without a filter.
        super(ruleEvaluator(searchOptions, excludedDetectors, NUGET_PROJECT_INSPECTOR_NAME), extractionEnvironmentSupplier);
        this.fallbackEvaluator = new FallbackDirectoryEvaluator(
            ruleEvaluator(searchOptions, excludedDetectors, NUGET_SOLUTION_INSPECTOR_NAME, NUGET_PROJECT_NATIVE_INSPECTOR_NAME),
            extractionEnvironmentSupplier
        );
    }

    @Override
    public DirectoryEvaluation evaluate(DirectoryFindResult rootDirectory, DetectorRuleSet rules) {
        DirectoryEvaluation evaluation = super.evaluate(rootDirectory, rules);
        if (nugetExtractedInRoot(evaluation)) {
            return evaluation;
        }

        logger.debug("The NuGet solution filter was not extracted successfully; evaluating NuGet again with only the NuGet Project Inspector.");
        DetectorRuleSet nugetRules = new DetectorRuleSet(rules.getDetectorRules().stream()
            .filter(rule -> rule.getDetectorType() == DetectorType.NUGET)
            .collect(Collectors.toList()));
        Map<File, DetectorRuleEvaluation> fallbackByDirectory = new HashMap<>();
        DetectorEvaluationUtil.asFlatList(fallbackEvaluator.evaluateQuietly(rootDirectory, nugetRules))
            .forEach(directory -> nugetEvaluation(directory).ifPresent(nuget -> fallbackByDirectory.put(directory.getDirectory(), nuget)));
        if (fallbackByDirectory.values().stream().noneMatch(DetectorRuleEvaluation::wasFound)) {
            return evaluation; // the Project Inspector applies nowhere, so the failed filter run stays as the result
        }

        Optional<EntryPointFoundResult> filterRun = nugetEvaluation(evaluation).flatMap(DetectorRuleEvaluation::getFoundEntryPoint);
        return replaceNuget(evaluation, (directory, nuget) -> {
            DetectorRuleEvaluation fallback = fallbackByDirectory.getOrDefault(directory, nuget);
            return fallback.wasFound() && filterRun.isPresent() ? withFilterRun(filterRun.get(), fallback) : fallback;
        });
    }

    private static DetectorRuleEvaluator ruleEvaluator(SearchOptions searchOptions, List<String> excludedDetectors, String... additionalExclusions) {
        List<String> exclusions = new ArrayList<>(excludedDetectors);
        exclusions.addAll(List.of(additionalExclusions));
        return new DetectorRuleEvaluator(new SearchEvaluator(searchOptions), new DetectableEvaluator(), new DetectableExclusionEvaluator(exclusions));
    }

    private static String nameOf(Class<?> detectableClass) {
        return detectableClass.getAnnotation(DetectableInfo.class).name();
    }

    private boolean nugetExtractedInRoot(DirectoryEvaluation root) {
        return nugetEvaluation(root)
            .flatMap(DetectorRuleEvaluation::getFoundEntryPoint)
            .filter(found -> found.getEntryPointEvaluation().getEvaluatedDetectables().stream().anyMatch(DetectableEvaluationResult::wasExtractionSuccessful))
            .isPresent();
    }

    private Optional<DetectorRuleEvaluation> nugetEvaluation(DirectoryEvaluation directory) {
        return directory.getEvaluations().stream()
            .filter(evaluation -> evaluation.getRule().getDetectorType() == DetectorType.NUGET)
            .findFirst();
    }

    private DirectoryEvaluation replaceNuget(DirectoryEvaluation directory, BiFunction<File, DetectorRuleEvaluation, DetectorRuleEvaluation> replacement) {
        List<DetectorRuleEvaluation> evaluations = directory.getEvaluations().stream()
            .map(evaluation -> evaluation.getRule().getDetectorType() == DetectorType.NUGET ? replacement.apply(directory.getDirectory(), evaluation) : evaluation)
            .collect(Collectors.toList());
        List<DirectoryEvaluation> children = directory.getChildren().stream()
            .map(child -> replaceNuget(child, replacement))
            .collect(Collectors.toList());
        return new DirectoryEvaluation(directory.getDirectory(), directory.getDepth(), evaluations, children);
    }

    private DetectorRuleEvaluation withFilterRun(EntryPointFoundResult filterRun, DetectorRuleEvaluation fallback) {
        List<DetectableEvaluationResult> evaluated = new ArrayList<>(filterRun.getEntryPointEvaluation().getEvaluatedDetectables());
        evaluated.addAll(fallback.getFoundEntryPoint().get().getEntryPointEvaluation().getEvaluatedDetectables());
        EntryPointFoundResult combined = EntryPointFoundResult.evaluated(
            filterRun.getEntryPoint(),
            filterRun.getSearchResult(),
            filterRun.getApplicableResult(),
            new EntryPointEvaluation(filterRun.getEntryPoint(), evaluated)
        );
        return new DetectorRuleEvaluation(fallback.getRule(), fallback.getNotFoundEntryPoints(), combined);
    }

    // Lets the second pass run without repeating the "Evaluating detectors" message.
    private static class FallbackDirectoryEvaluator extends DirectoryEvaluator {
        FallbackDirectoryEvaluator(DetectorRuleEvaluator detectorRuleEvaluator, Function<DetectorType, ExtractionEnvironment> extractionEnvironmentSupplier) {
            super(detectorRuleEvaluator, extractionEnvironmentSupplier);
        }

        DirectoryEvaluation evaluateQuietly(DirectoryFindResult rootDirectory, DetectorRuleSet rules) {
            return evaluate(rootDirectory, rules, new HashSet<>(), new HashSet<>());
        }
    }
}
