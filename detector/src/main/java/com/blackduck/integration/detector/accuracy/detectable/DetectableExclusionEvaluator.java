package com.blackduck.integration.detector.accuracy.detectable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detector.rule.DetectableDefinition;

public class DetectableExclusionEvaluator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Set<String> excludedDetectables = new HashSet<>();

    public DetectableExclusionEvaluator(List<String> excludedDetectables) {
        excludedDetectables.forEach(detectableName -> {
            this.excludedDetectables.add(matchableName(detectableName));
        });
    }

    public boolean isDetectableExcluded(DetectableDefinition detectableDefinition) {
        if (excludedDetectables.isEmpty()) {
            return false;
        }
        
        if (excludedDetectables.contains(matchableName(detectableDefinition.getName()))) {
            logger.debug("Excluding Detectable {} as per configuration", detectableDefinition.getName());
            return true;
        }
        return false;
    }

    private String matchableName(String detectableName) {
        return detectableName.toLowerCase().replace(" ", "");
    }
}