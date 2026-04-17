package com.blackduck.integration.detect.workflow.blackduck.codelocation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.blackduck.integration.blackduck.service.model.NotificationTaskRange;

public class CodeLocationWaitCalculator {
    public CodeLocationWaitData calculateWaitData(List<WaitableCodeLocationData> codeLocationCreationDatas, long codeLocationsUploadStartTime) {
        int expectedNotificationCount = 0;
        NotificationTaskRange notificationRange = getNotificationRangeGivenUploadStartTime(codeLocationsUploadStartTime);
        Set<String> codeLocationNames = new HashSet<>();

        for (WaitableCodeLocationData codeLocationCreationData : codeLocationCreationDatas) {
            expectedNotificationCount += codeLocationCreationData.getExpectedNotificationCount();
            codeLocationNames.addAll(codeLocationCreationData.getSuccessfulCodeLocationNames());
        }

        return new CodeLocationWaitData(notificationRange, codeLocationNames, expectedNotificationCount);
    }

    public NotificationTaskRange getNotificationRangeGivenUploadStartTime(long codeLocationProcessingStartTime) {
        LocalDateTime localStartTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(codeLocationProcessingStartTime), ZoneOffset.UTC);
        LocalDateTime threeDaysLater = localStartTime.plusDays(3L);
        Date startDate = Date.from(localStartTime.atZone(ZoneOffset.UTC).toInstant());
        Date endDate = Date.from(threeDaysLater.atZone(ZoneOffset.UTC).toInstant());
        return new NotificationTaskRange(0, startDate, endDate);
    }
}
