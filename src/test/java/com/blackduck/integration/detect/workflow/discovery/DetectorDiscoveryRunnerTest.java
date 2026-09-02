package com.blackduck.integration.detect.workflow.discovery;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class DetectorDiscoveryRunnerTest {

    @Test
    public void testPopulateScanTypeRecommendationsUsesPublicNamesAndStableOrdering() {
        Map<DetectTool, Set<String>> evidence = new EnumMap<>(DetectTool.class);
        evidence.put(DetectTool.BINARY_SCAN, new HashSet<>(Arrays.asList("/source/z.war", "/source/a.jar")));
        evidence.put(DetectTool.SIGNATURE_SCAN, Collections.singleton("/source"));
        evidence.put(DetectTool.DETECTOR, Collections.singleton("/source"));
        evidence.put(DetectTool.IAC_SCAN, Collections.singleton("/source/main.tf"));
        DetectorDiscoveryOutput output = new DetectorDiscoveryOutput();

        DetectorDiscoveryRunner.populateScanTypeRecommendations(output, evidence);

        Assertions.assertEquals(Arrays.asList("DETECTOR", "SIGNATURE", "BINARY"), output.recommendedScanTypes);
        Assertions.assertEquals(Arrays.asList("/source/a.jar", "/source/z.war"), output.scanTypeSignals.get("BINARY"));
        Assertions.assertEquals(Collections.singletonList("/source"), output.scanTypeSignals.get("DETECTOR"));
        Assertions.assertEquals(Collections.singletonList("/source"), output.scanTypeSignals.get("SIGNATURE"));
        Assertions.assertFalse(output.scanTypeSignals.containsKey("IAC_SCAN"));
    }

    @Test
    public void testEmptyScanTypeCollectionsArePresentInJson() {
        DetectorDiscoveryOutput output = new DetectorDiscoveryOutput();

        JsonObject json = new Gson().toJsonTree(output).getAsJsonObject();

        Assertions.assertTrue(json.has("recommendedScanTypes"));
        Assertions.assertEquals(0, json.getAsJsonArray("recommendedScanTypes").size());
        Assertions.assertTrue(json.has("scanTypeSignals"));
        Assertions.assertEquals(0, json.getAsJsonObject("scanTypeSignals").size());
    }
}
