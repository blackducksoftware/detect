package com.blackduck.integration.detect.workflow.blackduck.codelocation;

import java.util.Date;
import java.util.Set;

import com.blackduck.integration.blackduck.service.model.NotificationTaskRange;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;

public class WaitableCodeLocationData {
    private final DetectTool detectTool;
    private final int expectedNotificationCount;
    private final Set<String> successfulCodeLocationNames;
    @Deprecated
    private final NotificationTaskRange notificationTaskRange; // note: todo should remove. just need start date passed to it.
//    private final Date startDate;

    @Deprecated
    public WaitableCodeLocationData(DetectTool detectTool, int expectedNotificationCount, Set<String> successfulCodeLocationNames, NotificationTaskRange notificationTaskRange) {
        this.detectTool = detectTool;
        this.expectedNotificationCount = expectedNotificationCount; // doesnt need to come from notification service. comes from scan output. if snippet, then 2. otherwise, just one code location expected.
        this.successfulCodeLocationNames = successfulCodeLocationNames;
        this.notificationTaskRange = notificationTaskRange; // what for???
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

    public NotificationTaskRange getNotificationTaskRange() {
        if (notificationTaskRange != null) {
            System.out.println("Getting notification range from waitable code location SHOULD NOT BE USED. THIS IS DEPRECATED AND SHOULD BE REMOVED. NOTIFICATION RANGE SHOULD BE PASSED TO THE WAIT JOB CONDITION SEPARATELY. THIS IS ONLY HERE FOR LEGACY REASONS. NOTIFICATION RANGE START DATE SHOULD BE PASSED TO THE WAIT JOB CONDITION SEPARATELY.");
        }
        System.out.println("notification task range for this code location wait data is null.");
        return notificationTaskRange;
    }
}
