package com.blackduck.integration.detect.workflow.blackduck.codelocation;

import java.util.Date;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import com.blackduck.integration.blackduck.service.model.NotificationTaskRange;

public class CodeLocationWaitCalculator {
    public CodeLocationWaitData calculateWaitData(Queue<WaitableCodeLocationData> codeLocationCreationDatas) {
        int expectedNotificationCount = 0;
        NotificationTaskRange notificationRange = null;
        Set<String> codeLocationNames = new HashSet<>();

        for (WaitableCodeLocationData codeLocationCreationData : codeLocationCreationDatas) {
            expectedNotificationCount += codeLocationCreationData.getExpectedNotificationCount();
            codeLocationNames.addAll(codeLocationCreationData.getSuccessfulCodeLocationNames());

            if (null == notificationRange) {
                notificationRange = codeLocationCreationData.getNotificationTaskRange();
            } else {
                NotificationTaskRange rangeToAdd = codeLocationCreationData.getNotificationTaskRange();
                long earliestTaskTime = Math.min(notificationRange.getTaskStartTime(), rangeToAdd.getTaskStartTime());
                Date earliestStartDate = earliestDate(notificationRange.getStartDate(), rangeToAdd.getStartDate());
                Date latestEndDate = latestDate(notificationRange.getEndDate(), rangeToAdd.getEndDate());
                notificationRange = new NotificationTaskRange(earliestTaskTime, earliestStartDate, latestEndDate);
            }
        }

        return new CodeLocationWaitData(notificationRange, codeLocationNames, expectedNotificationCount);
    }

    private Date earliestDate(Date d1, Date d2) {
        if (d1.before(d2)) {
            return d1;
        }
        return d2;
    }

    private Date latestDate(Date d1, Date d2) {
        if (d1.after(d2)) {
            return d1;
        }
        return d2;
    }
}
