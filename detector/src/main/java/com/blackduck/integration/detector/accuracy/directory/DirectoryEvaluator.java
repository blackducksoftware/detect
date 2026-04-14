package com.blackduck.integration.detector.accuracy.directory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.detector.accuracy.detectable.DetectableEvaluationResult;
import com.blackduck.integration.detector.accuracy.entrypoint.DetectorRuleEvaluation;
import com.blackduck.integration.detector.accuracy.entrypoint.DetectorRuleEvaluator;
import com.blackduck.integration.detector.accuracy.entrypoint.EntryPointFoundResult;
import com.blackduck.integration.detector.accuracy.search.SearchEnvironment;
import com.blackduck.integration.detector.base.DetectorType;
import com.blackduck.integration.detector.finder.DirectoryFindResult;
import com.blackduck.integration.detector.rule.DetectableDefinition;
import com.blackduck.integration.detector.rule.DetectorRule;
import com.blackduck.integration.detector.rule.DetectorRuleSet;

public class DirectoryEvaluator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DetectorRuleEvaluator detectorRuleEvaluator;
    private final Function<DetectorType, ExtractionEnvironment> extractionEnvironmentSupplier;

    public DirectoryEvaluator(
        DetectorRuleEvaluator detectorRuleEvaluator,
        Function<DetectorType, ExtractionEnvironment> extractionEnvironmentSupplier
    ) {
        this.detectorRuleEvaluator = detectorRuleEvaluator;
        this.extractionEnvironmentSupplier = extractionEnvironmentSupplier;
    }

    public DirectoryEvaluation evaluate(DirectoryFindResult rootDirectory, DetectorRuleSet rules) {
        logger.info("Evaluating detectors. This may take a while.");
        return evaluate(rootDirectory, rules, new HashSet<>(), new HashSet<>());
    }

    protected DirectoryEvaluation evaluate(
        DirectoryFindResult findResult,
        DetectorRuleSet rules,
        Set<DetectorType> appliedInParent,
        Set<DetectableDefinition> extractedInParentDetectables
    ) {
        logger.debug("Determining applicable detectors on the directory: {}", findResult.getDirectory());

        File directory = findResult.getDirectory();
        DetectableEnvironment detectableEnvironment = new DetectableEnvironment(directory);
        Set<DetectorType> appliedSoFar = new HashSet<>();
        Set<DetectableDefinition> extractedSoFar = new HashSet<>();
        List<DetectorRuleEvaluation> evaluations = new LinkedList<>();

        for (DetectorRule rule : rules.getDetectorRules()) {
            SearchEnvironment searchEnvironment = new SearchEnvironment(findResult.getDepthFromRoot(), appliedSoFar, appliedInParent, extractedInParentDetectables);
            DetectorRuleEvaluation detectorRuleEvaluation = detectorRuleEvaluator.evaluate(
                detectableEnvironment,
                searchEnvironment,
                rule,
                () -> extractionEnvironmentSupplier.apply(rule.getDetectorType())
            );
            if (detectorRuleEvaluation.wasFound() && detectorRuleEvaluation.getFoundEntryPoint().isPresent()) { //should this capture only success?
                appliedSoFar.add(rule.getDetectorType());

                EntryPointFoundResult foundEntryPoint = detectorRuleEvaluation.getFoundEntryPoint().get();
                logCascadeResults(rule, foundEntryPoint);
                foundEntryPoint.getEntryPointEvaluation().getEvaluatedDetectables().stream()
                    .filter(DetectableEvaluationResult::wasExtractionSuccessful)
                    .map(DetectableEvaluationResult::getDetectableDefinition)
                    .forEach(extractedSoFar::add);
            }
            evaluations.add(detectorRuleEvaluation);
        }

        if (!appliedSoFar.isEmpty()) {
            logger.debug("Found ({}) applicable detectors in: {}", appliedSoFar.size(), directory);
        }

        Set<DetectorType> nextAppliedInParent = new HashSet<>();
        nextAppliedInParent.addAll(appliedInParent);
        nextAppliedInParent.addAll(appliedSoFar);

        Set<DetectableDefinition> nextExtractedInParentDetectables = new HashSet<>();
        nextExtractedInParentDetectables.addAll(extractedInParentDetectables);
        nextExtractedInParentDetectables.addAll(extractedSoFar);

        List<DirectoryEvaluation> children = new ArrayList<>();
        for (DirectoryFindResult subdirectory : findResult.getChildren()) {
            DirectoryEvaluation child = evaluate(subdirectory, rules, nextAppliedInParent, nextExtractedInParentDetectables);
            children.add(child);
        }

        return new DirectoryEvaluation(directory, findResult.getDepthFromRoot(), evaluations, children);
    }

    private void logCascadeResults(DetectorRule rule, EntryPointFoundResult foundEntryPoint) {
        logger.debug(
            "Detector Type {} Entry Point {} applied and was attempted",
            rule.getDetectorType().toString(),
            foundEntryPoint.getEntryPoint().getPrimary().getName()
        );
        List<String> detectableResultMsgs = foundEntryPoint.getEntryPointEvaluation().getEvaluatedDetectables().stream()
            .map(r -> r.getDetectableDefinition().getName() + ": " + r.wasExtractionSuccessful())
            .collect(Collectors.toList());
        logger.debug("Detector results (\"true\" = succeeded): {}", detectableResultMsgs);
    }
}

