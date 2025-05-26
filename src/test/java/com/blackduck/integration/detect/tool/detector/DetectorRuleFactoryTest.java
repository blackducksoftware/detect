package com.blackduck.integration.detect.tool.detector;

import com.blackduck.integration.detect.tool.detector.factory.DetectDetectableFactory;
import com.blackduck.integration.detector.rule.DetectorRule;
import com.blackduck.integration.detector.rule.DetectorRuleSet;
import com.blackduck.integration.detector.base.DetectorType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DetectorRuleFactoryTest {

    @Test
    void testCreateRules() {
        // Arrange
        DetectDetectableFactory mockDetectableFactory = Mockito.mock(DetectDetectableFactory.class);
        DetectorRuleFactory detectorRuleFactory = new DetectorRuleFactory();

        // Act
        DetectorRuleSet detectorRuleSet = detectorRuleFactory.createRules(mockDetectableFactory);

        // Assert
        assertNotNull(detectorRuleSet, "DetectorRuleSet should not be null");

        List<DetectorRule> detectorRules = detectorRuleSet.getDetectorRules();
        assertNotNull(detectorRules, "DetectorRules should not be null");

        Set<DetectorType> declaredTypes = new HashSet<>();
        for (DetectorRule rule : detectorRules) {
            DetectorType currentType = rule.getDetectorType();
            assertNotNull(currentType, "DetectorType should not be null");

            rule.getEntryPoints().forEach(entryPoint -> {
                entryPoint.getSearchRule().getYieldsTo().forEach(yieldedType -> {
                    assertTrue(declaredTypes.contains(yieldedType),
                        String.format("DetectorType %s yields to %s, which is not declared earlier.", currentType, yieldedType));
                });
            });
            declaredTypes.add(currentType);
        }
    }
}
