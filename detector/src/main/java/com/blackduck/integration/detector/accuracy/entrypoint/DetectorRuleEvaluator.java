package com.blackduck.integration.detector.accuracy.entrypoint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.detector.accuracy.detectable.DetectableEvaluationResult;
import com.blackduck.integration.detector.accuracy.detectable.DetectableEvaluator;
import com.blackduck.integration.detector.accuracy.detectable.DetectableExclusionEvaluator;
import com.blackduck.integration.detector.accuracy.search.SearchEnvironment;
import com.blackduck.integration.detector.accuracy.search.SearchEvaluator;
import com.blackduck.integration.detector.result.DetectorResult;
import com.blackduck.integration.detector.rule.DetectableDefinition;
import com.blackduck.integration.detector.rule.DetectorRule;
import com.blackduck.integration.detector.rule.EntryPoint;

public class DetectorRuleEvaluator {
    private final SearchEvaluator searchEvaluator;
    private final DetectableEvaluator detectableEvaluator;
    private final DetectableExclusionEvaluator detectableExclusionEvaluator;

    public DetectorRuleEvaluator(
        SearchEvaluator searchEvaluator,
        DetectableEvaluator detectableEvaluator,
        DetectableExclusionEvaluator detectableExclusionEvaluator
    ) {
        this.searchEvaluator = searchEvaluator;
        this.detectableEvaluator = detectableEvaluator;
        this.detectableExclusionEvaluator = detectableExclusionEvaluator;
    }

    public DetectorRuleEvaluation evaluate(
        DetectableEnvironment environment,
        SearchEnvironment searchEnvironment,
        DetectorRule detectorRule,
        Supplier<ExtractionEnvironment> extractionEnvironmentSupplier
    ) {
        List<EntryPointNotFoundResult> notFoundEntryPoints = new ArrayList<>();
        EntryPointFoundResult foundEntryPoint = null;
        for (EntryPoint entryPoint : detectorRule.getEntryPoints()) {
            foundEntryPoint = evaluateEntryPoint(
                environment,
                searchEnvironment,
                entryPoint,
                detectorRule,
                notFoundEntryPoints,
                extractionEnvironmentSupplier
            );

            if (foundEntryPoint != null) {
                break; // we have found an entry point and extracted. We are done.
            }
        }
        return new DetectorRuleEvaluation(detectorRule, notFoundEntryPoints, foundEntryPoint);
    }

    private EntryPointFoundResult evaluateEntryPoint(
        DetectableEnvironment environment,
        SearchEnvironment searchEnvironment,
        EntryPoint entryPoint,
        DetectorRule detectorRule,
        List<EntryPointNotFoundResult> notFoundEntryPoints,
        Supplier<ExtractionEnvironment> extractionEnvironmentSupplier
    ) {
        DetectorResult searchResult = searchEvaluator.evaluateSearchable(
            detectorRule.getDetectorType(),
            entryPoint.getSearchRule(),
            searchEnvironment
        );
        if (!searchResult.getPassed()) {
            notFoundEntryPoints.add(EntryPointNotFoundResult.notSearchable(entryPoint, searchResult));
            return null;
        }

        DetectableDefinition definition = selectDetectableDefinition(entryPoint);
        if (definition == null) {
            return null;
        }

        Detectable selectedDetectable = definition.getDetectableCreatable().createDetectable(environment);
        DetectableResult applicable = selectedDetectable.applicable();
        if (!applicable.getPassed()) {
            notFoundEntryPoints.add(EntryPointNotFoundResult.notApplicable(entryPoint, searchResult, applicable));
            return null;
        }

        EntryPointEvaluation entryPointEvaluation = extract(entryPoint, environment, extractionEnvironmentSupplier);
        return EntryPointFoundResult.evaluated(entryPoint, searchResult, applicable, entryPointEvaluation);
    }

    private DetectableDefinition selectDetectableDefinition(EntryPoint entryPoint) {
        DetectableDefinition primary = entryPoint.getPrimary();
        if (!detectableExclusionEvaluator.isDetectableExcluded(primary)) {
            return primary;
        }

        // Primary is excluded, look for the first non-excluded fallback
        List<DetectableDefinition> fallbacks = entryPoint.getFallbacks();
        if (fallbacks != null) {
            for (DetectableDefinition fallback : fallbacks) {
                if (!detectableExclusionEvaluator.isDetectableExcluded(fallback)) {
                    return fallback;
                }
            }
        }

        // No valid definition found
        return null;
    }

    private EntryPointEvaluation extract(EntryPoint entryPoint, DetectableEnvironment detectableEnvironment, Supplier<ExtractionEnvironment> extractionEnvironmentSupplier) {
        List<DetectableDefinition> toCascade = entryPoint.allDetectables();
        List<DetectableEvaluationResult> evaluated = new ArrayList<>();
        for (DetectableDefinition detectableDefinition : toCascade) {
            if (!detectableExclusionEvaluator.isDetectableExcluded(detectableDefinition)) {
                Detectable detectable = detectableDefinition.getDetectableCreatable().createDetectable(detectableEnvironment);
                DetectableEvaluationResult detectableEvaluationResult = detectableEvaluator.evaluate(detectable, detectableDefinition, extractionEnvironmentSupplier);
                evaluated.add(detectableEvaluationResult);
                if (detectableEvaluationResult.wasExtractionSuccessful()) {
                    break;
                }
            }
        }
        return new EntryPointEvaluation(entryPoint, evaluated);
    }
}
