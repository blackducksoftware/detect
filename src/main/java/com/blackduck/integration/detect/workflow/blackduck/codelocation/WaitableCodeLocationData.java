package com.blackduck.integration.detect.workflow.blackduck.codelocation;

import java.util.Set;

import com.blackduck.integration.detect.configuration.enumeration.DetectTool;

public class WaitableCodeLocationData {
    private final DetectTool detectTool;
    private final int expectedNotificationCount;
    private final Set<String> successfulCodeLocationNames;

    public WaitableCodeLocationData(DetectTool detectTool, int expectedNotificationCount, Set<String> successfulCodeLocationNames ) {
        this.detectTool = detectTool;
        this.expectedNotificationCount = expectedNotificationCount;
        this.successfulCodeLocationNames = successfulCodeLocationNames;
    }

    public DetectTool getDetectTool() {
        return detectTool;
    }

    public int getExpectedNotificationCount() {
        return expectedNotificationCount;
    }


    public Set<String> getSuccessfulCodeLocationNames() {
        return successfulCodeLocationNames;
    }
}
