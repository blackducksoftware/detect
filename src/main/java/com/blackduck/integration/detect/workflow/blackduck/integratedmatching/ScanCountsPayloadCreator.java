package com.blackduck.integration.detect.workflow.blackduck.integratedmatching;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.workflow.blackduck.codelocation.WaitableCodeLocationData;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.model.ScanCounts;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.model.ScanCountsPayload;

public class ScanCountsPayloadCreator {

    @NotNull
    public ScanCountsPayload createPayloadFromCountsByTool(
        List<WaitableCodeLocationData> createdCodelocations,
        Map<DetectTool, Integer> additionalCounts,
        Set<String> supportedScanTypes
    ) {
        Map<DetectTool, Integer> countsByTool = collectCountsByTool(createdCodelocations, additionalCounts);
        return createPayloadFromCountsByTool(countsByTool, supportedScanTypes);
    }

    @NotNull
    private ScanCountsPayload createPayloadFromCountsByTool(final Map<DetectTool, Integer> countsByTool, Set<String> supportedScanTypes) {
        // Filter counts based on supported scan types
        int packageManagerScanCount = 0;
        if (supportedScanTypes.contains("PACKAGE_MANAGER")) {
            packageManagerScanCount = countsByTool.getOrDefault(DetectTool.DETECTOR, 0)
                + countsByTool.getOrDefault(DetectTool.BAZEL, 0)
                + countsByTool.getOrDefault(DetectTool.DOCKER, 0);
        }

        int signatureScanCount = 0;
        if (supportedScanTypes.contains("SIGNATURE")) {
            signatureScanCount = countsByTool.getOrDefault(DetectTool.SIGNATURE_SCAN, 0);
        }

        int binaryScanCount = 0;
        if (supportedScanTypes.contains("BINARY")) {
            binaryScanCount = countsByTool.getOrDefault(DetectTool.BINARY_SCAN, 0);
        }

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
