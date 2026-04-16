package com.blackduck.integration.detect.workflow.blackduck.codelocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.blackduck.integration.blackduck.codelocation.CodeLocationBatchOutput;
import com.blackduck.integration.blackduck.codelocation.CodeLocationCreationData;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;

public class CodeLocationAccumulator {
    private final List<WaitableCodeLocationData> waitableCodeLocationData = new ArrayList<>();
    private final Set<String> nonWaitableCodeLocations = new HashSet<>();
    private final Map<DetectTool, Integer> additionalCountsByTool = new EnumMap<>(DetectTool.class);

    public void addWaitableCodeLocations(DetectTool detectTool, CodeLocationCreationData<? extends CodeLocationBatchOutput<?>> creationData) {
        addWaitableCodeLocations(new WaitableCodeLocationData(detectTool,
            creationData.getOutput().getExpectedNotificationCount(),
            creationData.getOutput().getSuccessfulCodeLocationNames()
        ));
    }

    public void addWaitableCodeLocations(WaitableCodeLocationData codeLocationData) {
        waitableCodeLocationData.add(codeLocationData);
    }

    public void addNonWaitableCodeLocations(Set<String> names) {
        nonWaitableCodeLocations.addAll(names);
    }

    public void addNonWaitableCodeLocation(String name) {
        nonWaitableCodeLocations.add(name);
    }

    /** Increments the count of code locations for the given tool which will later be used by
     *  {@link com.blackduck.integration.detect.workflow.blackduck.integratedmatching.ScanCountsPayloadCreator}
     *  when correlated scanning is enabled. This method must be called whenever a code location is created for a tool
     *  that supports correlated scanning. */
    public void incrementCodeLocationCountForTool(DetectTool tool, int count) {
        additionalCountsByTool.put(tool, additionalCountsByTool.getOrDefault(tool, 0) + count);
    }

    public List<WaitableCodeLocationData> getWaitableCodeLocations() {
        return waitableCodeLocationData;
    }

    public Set<String> getNonWaitableCodeLocations() {
        return nonWaitableCodeLocations;
    }

    public Map<DetectTool, Integer> getAdditionalCountsByTool() {
        return additionalCountsByTool;
    }
}
