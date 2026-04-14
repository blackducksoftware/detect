package com.blackduck.integration.detect.workflow.blackduck.codelocation;

import java.util.Date;
import java.util.Set;

import com.blackduck.integration.blackduck.service.model.NotificationTaskRange;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;

public class WaitableCodeLocationData {
    private final DetectTool detectTool;
    private final int expectedNotificationCount;
    private final Set<String> successfulCodeLocationNames;
    private final NotificationTaskRange notificationTaskRange;

    public WaitableCodeLocationData(DetectTool detectTool, int expectedNotificationCount, Set<String> successfulCodeLocationNames, NotificationTaskRange notificationTaskRange) {
        this.detectTool = detectTool;
        this.expectedNotificationCount = expectedNotificationCount;
        this.successfulCodeLocationNames = successfulCodeLocationNames;
        this.notificationTaskRange = notificationTaskRange;
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
