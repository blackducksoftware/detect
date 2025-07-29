package com.blackduck.integration.detect.workflow.blackduck.integratedmatching;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.workflow.blackduck.codelocation.WaitableCodeLocationData;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.model.ScanCounts;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.model.ScanCountsPayload;

public class ScanCountsPayloadCreator {

    public ScanCountsPayload create(List<WaitableCodeLocationData> createdCodelocations, Map<DetectTool, Integer> additionalCounts) {
        Map<DetectTool, Integer> countsByTool = collectCountsByTool(createdCodelocations, additionalCounts);
        return createPayloadFromCountsByTool(countsByTool);
    }

    @NotNull
    private ScanCountsPayload createPayloadFromCountsByTool(final Map<DetectTool, Integer> countsByTool) {
        int packageManagerScanCount = countsByTool.getOrDefault(DetectTool.DETECTOR, 0)
            + countsByTool.getOrDefault(DetectTool.BAZEL, 0)
            + countsByTool.getOrDefault(DetectTool.DOCKER, 0);
        int signatureScanCount = countsByTool.getOrDefault(DetectTool.SIGNATURE_SCAN, 0);
        int binaryScanCount = countsByTool.getOrDefault(DetectTool.BINARY_SCAN, 0);
        ScanCounts scanCounts = new ScanCounts(packageManagerScanCount, signatureScanCount, binaryScanCount);
        return new ScanCountsPayload(scanCounts);
    }

    @NotNull
    private Map<DetectTool, Integer> collectCountsByTool(final List<WaitableCodeLocationData> createdCodelocations, Map<DetectTool, Integer> additionalCounts) {
        Map<DetectTool, Integer> countsByTool = new HashMap<>(additionalCounts);
        for (WaitableCodeLocationData waitableCodeLocationData : createdCodelocations) {
            int oldCount = countsByTool.getOrDefault(waitableCodeLocationData.getDetectTool(), 0);
            int newCount = oldCount + waitableCodeLocationData.getSuccessfulCodeLocationNames().size();
            countsByTool.put(waitableCodeLocationData.getDetectTool(), newCount);
        }
        return countsByTool;
    }
}
