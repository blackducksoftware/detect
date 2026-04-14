package com.blackduck.integration.detector.accuracy.detectable;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.ExceptionDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.detector.rule.DetectableDefinition;

public class DetectableEvaluator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DetectableEvaluationResult evaluate(
        Detectable detectable,
        DetectableDefinition detectableDefinition,
        Supplier<ExtractionEnvironment> extractionEnvironmentSupplier
    ) {
        DetectableResult applicable = detectable.applicable();
        if (!applicable.getPassed()) {
            return DetectableEvaluationResult.notApplicable(detectableDefinition, applicable);
        }

        logger.debug("Attempting {}", detectable.getClass().getSimpleName());
        DetectableResult extractable;
        try {
            extractable = detectable.extractable();
            if (!extractable.getPassed()) {
                return DetectableEvaluationResult.notExtractable(detectableDefinition, applicable, extractable);
            }
        } catch (Exception e) {
            return DetectableEvaluationResult.notExtractable(detectableDefinition, applicable, new ExceptionDetectableResult(e));
        }

        ExtractionEnvironment extractionEnvironment = extractionEnvironmentSupplier.get();
        Extraction extraction;
        try {
            extraction = detectable.extract(extractionEnvironment);
        } catch (Exception e) {
            extraction = new Extraction.Builder().exception(e).build();
        }
        return DetectableEvaluationResult.extracted(detectableDefinition, applicable, extractable, extractionEnvironment, extraction);
    }
}
