package com.blackduck.integration.detector.accuracy.entrypoint;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.detector.accuracy.detectable.DetectableEvaluationResult;
import com.blackduck.integration.detector.accuracy.detectable.DetectableEvaluator;
import com.blackduck.integration.detector.accuracy.search.SearchEnvironment;
import com.blackduck.integration.detector.accuracy.search.SearchEvaluator;
import com.blackduck.integration.detector.result.DetectorResult;
import com.blackduck.integration.detector.rule.DetectableDefinition;
import com.blackduck.integration.detector.rule.DetectorRule;
import com.blackduck.integration.detector.rule.EntryPoint;

public class DetectorRuleEvaluator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SearchEvaluator searchEvaluator;
    private final DetectableEvaluator detectableEvaluator;

    public DetectorRuleEvaluator(SearchEvaluator searchEvaluator, DetectableEvaluator detectableEvaluator) {
        this.searchEvaluator = searchEvaluator;
        this.detectableEvaluator = detectableEvaluator;
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
            DetectorResult searchResult = searchEvaluator.evaluateSearchable(
                detectorRule.getDetectorType(),
                entryPoint.getSearchRule(),
                searchEnvironment
            );
            if (!searchResult.getPassed()) {
                notFoundEntryPoints.add(EntryPointNotFoundResult.notSearchable(entryPoint, searchResult));
                continue;
            }

            Detectable primaryDetectable = entryPoint.getPrimary().getDetectableCreatable().createDetectable(environment);
            DetectableResult applicable = primaryDetectable.applicable();
            if (!applicable.getPassed()) {
                notFoundEntryPoints.add(EntryPointNotFoundResult.notApplicable(entryPoint, searchResult, applicable));
                continue;
            }

            EntryPointEvaluation entryPointEvaluation = extract(entryPoint, environment, extractionEnvironmentSupplier);
            foundEntryPoint = EntryPointFoundResult.evaluated(entryPoint, searchResult, applicable, entryPointEvaluation);
            break; //Either way, we have found an entry point and extracted. We are done.
        }
        return new DetectorRuleEvaluation(detectorRule, environment, notFoundEntryPoints, foundEntryPoint);
    }

    public EntryPointEvaluation extract(EntryPoint entryPoint, DetectableEnvironment detectableEnvironment, Supplier<ExtractionEnvironment> extractionEnvironmentSupplier) {
        List<DetectableDefinition> toCascade = entryPoint.allDetectables();
        List<DetectableEvaluationResult> evaluated = new ArrayList<>();
        for (DetectableDefinition detectable : toCascade) {
            DetectableEvaluationResult detectableEvaluationResult = detectableEvaluator.evaluate(detectable, detectableEnvironment, extractionEnvironmentSupplier);
            evaluated.add(detectableEvaluationResult);
            if (detectableEvaluationResult.wasExtractionSuccessful()) {
                break;
            }
        }
        return new EntryPointEvaluation(entryPoint, evaluated);
    }
}
