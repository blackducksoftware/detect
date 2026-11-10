package com.blackduck.integration.detect.workflow.discovery;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detector.accuracy.search.SearchEnvironment;
import com.blackduck.integration.detector.accuracy.search.SearchEvaluator;
import com.blackduck.integration.detector.accuracy.search.SearchOptions;
import com.blackduck.integration.detector.base.DetectorType;
import com.blackduck.integration.detector.finder.DirectoryFindResult;
import com.blackduck.integration.detector.finder.DirectoryFinder;
import com.blackduck.integration.detector.finder.DirectoryFinderOptions;
import com.blackduck.integration.detector.result.DetectorResult;
import com.blackduck.integration.detector.rule.DetectableDefinition;
import com.blackduck.integration.detector.rule.DetectorRule;
import com.blackduck.integration.detector.rule.DetectorRuleSet;
import com.blackduck.integration.detector.rule.EntryPoint;

/**
 * Walks the source directory tree and runs ONLY the search + applicable() phases of Detect's detector
 * pipeline. This is the same logic a real scan runs for steps 1 and 2 (see {@code DirectoryEvaluator}
 * and {@code DetectorRuleEvaluator}) — it deliberately stops before extractable() and extract().
 *
 * Because applicable() only relies on the file system (never on inspector resolvers), the underlying
 * detectables can be built with null resolvers safely. See {@code DetectorDiscoveryRunner}.
 */
public class DetectorApplicabilityScanner {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DirectoryFinder directoryFinder = new DirectoryFinder();

    public ScanResult scan(File sourceDirectory, DetectorRuleSet ruleSet, DirectoryFinderOptions finderOptions, SearchOptions searchOptions, FileFinder fileFinder) {
        SearchEvaluator searchEvaluator = new SearchEvaluator(searchOptions);

        // De-duplicate by DetectorType — the first directory a detector fires in wins. This keeps
        // multi-module projects (where MAVEN fires in root AND every submodule) clean.
        Map<DetectorType, ApplicableHit> hitsByType = new LinkedHashMap<>();

        Optional<DirectoryFindResult> root = directoryFinder.findDirectories(sourceDirectory, finderOptions, fileFinder);
        root.ifPresent(findResult -> walk(findResult, ruleSet, searchEvaluator, new HashSet<>(), sourceDirectory.toPath(), hitsByType));

        List<ApplicableHit> hits = new ArrayList<>(hitsByType.values());

        List<String> notApplicable = new ArrayList<>();
        for (DetectorType type : DetectorType.values()) {
            if (!hitsByType.containsKey(type)) {
                notApplicable.add(type.name());
            }
        }

        return new ScanResult(hits, notApplicable);
    }

    private void walk(
        DirectoryFindResult findResult,
        DetectorRuleSet ruleSet,
        SearchEvaluator searchEvaluator,
        Set<DetectorType> appliedInParent,
        Path sourceRoot,
        Map<DetectorType, ApplicableHit> hitsByType
    ) {
        File directory = findResult.getDirectory();
        DetectableEnvironment environment = new DetectableEnvironment(directory);
        Set<DetectorType> appliedSoFar = new HashSet<>();

        for (DetectorRule rule : ruleSet.getDetectorRules()) {
            SearchEnvironment searchEnvironment = new SearchEnvironment(
                findResult.getDepthFromRoot(),
                appliedSoFar,
                appliedInParent,
                new HashSet<>() // extractedInParent — we never extract, so this is always empty
            );

            Optional<ApplicableHit> hit = evaluateRule(rule, searchEnvironment, searchEvaluator, environment, sourceRoot);
            if (hit.isPresent()) {
                appliedSoFar.add(rule.getDetectorType());
                hitsByType.putIfAbsent(rule.getDetectorType(), hit.get());
            }
        }

        Set<DetectorType> nextAppliedInParent = new HashSet<>(appliedInParent);
        nextAppliedInParent.addAll(appliedSoFar);

        for (DirectoryFindResult child : findResult.getChildren()) {
            walk(child, ruleSet, searchEvaluator, nextAppliedInParent, sourceRoot, hitsByType);
        }
    }

    private Optional<ApplicableHit> evaluateRule(
        DetectorRule rule,
        SearchEnvironment searchEnvironment,
        SearchEvaluator searchEvaluator,
        DetectableEnvironment environment,
        Path sourceRoot
    ) {
        for (EntryPoint entryPoint : rule.getEntryPoints()) {
            DetectorResult searchResult = searchEvaluator.evaluateSearchable(rule.getDetectorType(), entryPoint.getSearchRule(), searchEnvironment);
            if (!searchResult.getPassed()) {
                continue;
            }

            DetectableDefinition definition = entryPoint.getPrimary();
            try {
                Detectable detectable = definition.getDetectableCreatable().createDetectable(environment);
                DetectableResult applicable = detectable.applicable();
                if (applicable.getPassed()) {
                    String triggeredBy = resolveTriggerFileName(applicable);
                    String triggeredByPath = resolveTriggerRelativePath(applicable, sourceRoot);
                    return Optional.of(new ApplicableHit(
                        rule.getDetectorType(),
                        definition.getName(),
                        environment.getDirectory(),
                        triggeredBy,
                        triggeredByPath
                    ));
                }
            } catch (Exception e) {
                // A detectable that throws during applicable() is a bug in that detectable, not here.
                // Log and continue so one bad detector never breaks discovery for the rest.
                logger.debug("Detector {} threw during applicable() evaluation: {}", rule.getDetectorType(), e.getMessage());
            }
        }
        return Optional.empty();
    }

    private String resolveTriggerFileName(DetectableResult applicable) {
        List<File> relevantFiles = applicable.getRelevantFiles();
        if (relevantFiles != null && !relevantFiles.isEmpty() && relevantFiles.get(0) != null) {
            return relevantFiles.get(0).getName();
        }
        return null;
    }

    private String resolveTriggerRelativePath(DetectableResult applicable, Path sourceRoot) {
        List<File> relevantFiles = applicable.getRelevantFiles();
        if (relevantFiles == null || relevantFiles.isEmpty() || relevantFiles.get(0) == null) {
            return null;
        }
        Path filePath = relevantFiles.get(0).toPath();
        try {
            return sourceRoot.relativize(filePath).toString();
        } catch (IllegalArgumentException e) {
            return filePath.toString();
        }
    }

    public static class ApplicableHit {
        public final DetectorType detectorType;
        public final String detectableName;
        public final File directory;
        public final String triggeredBy;
        public final String triggeredByPath;

        public ApplicableHit(DetectorType detectorType, String detectableName, File directory, String triggeredBy, String triggeredByPath) {
            this.detectorType = detectorType;
            this.detectableName = detectableName;
            this.directory = directory;
            this.triggeredBy = triggeredBy;
            this.triggeredByPath = triggeredByPath;
        }
    }

    public static class ScanResult {
        public final List<ApplicableHit> hits;
        public final List<String> notApplicable;

        public ScanResult(List<ApplicableHit> hits, List<String> notApplicable) {
            this.hits = hits;
            this.notApplicable = notApplicable;
        }
    }
}

