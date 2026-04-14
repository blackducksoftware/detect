package com.blackduck.integration.detect.workflow.blackduck.codelocation;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.blackduck.service.model.NotificationTaskRange;

public class CodeLocationWaitData {
    @Nullable
    private final NotificationTaskRange notificationRange;
    private final Set<String> codeLocationNames;
    private final int expectedNotificationCount;

    public CodeLocationWaitData(@Nullable NotificationTaskRange notificationRange, Set<String> codeLocationNames, int expectedNotificationCount) {
        this.notificationRange = notificationRange;
        this.codeLocationNames = codeLocationNames;
        this.expectedNotificationCount = expectedNotificationCount;
    }

    @Nullable
    public NotificationTaskRange getNotificationRange() {
        return notificationRange;
    }

    public Set<String> getCodeLocationNames() {
        return codeLocationNames;
    }

    public int getExpectedNotificationCount() {
        return expectedNotificationCount;
    }

}
