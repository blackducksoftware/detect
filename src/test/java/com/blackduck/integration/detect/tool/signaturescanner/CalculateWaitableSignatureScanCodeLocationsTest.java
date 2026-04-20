package com.blackduck.integration.detect.tool.signaturescanner;

import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerCodeLocationResult;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerReport;
import static com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerReportTestUtil.skippedReport;
import static com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerReportTestUtil.successfulReport;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.SetUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import com.blackduck.integration.detect.tool.signaturescanner.operation.CalculateWaitableSignatureScanCodeLocations;
import com.blackduck.integration.detect.workflow.blackduck.codelocation.WaitableCodeLocationData;

public class CalculateWaitableSignatureScanCodeLocationsTest {
    @Test
    public void successfulScanWaits() {
        List<SignatureScannerReport> reports = Arrays.asList(
            successfulReport("name 1", 2),
            successfulReport("name 2", 3)
        );

        SignatureScannerCodeLocationResult result = calculate(reports);

        assertWaited(result, Sets.newSet("name 1", "name 2"), 5);
    }

    @Test
    public void skippedScanDoesNotWait() {
        List<SignatureScannerReport> reports = Collections.singletonList(
            skippedReport("name", 2)
        );

        SignatureScannerCodeLocationResult result = calculate(reports);

        assertWaited(result, Collections.emptySet(), 0);
        assertNonWaited(result, Sets.newSet("name"));
    }

    @Test
    public void combinationWaitsOnlyForSuccess() {
        List<SignatureScannerReport> reports = Arrays.asList(
            skippedReport("skipped", 2),
            successfulReport("success", 1)
        );

        SignatureScannerCodeLocationResult result = calculate(reports);

        assertWaited(result, Sets.newSet("success"), 1);
        assertNonWaited(result, Sets.newSet("skipped"));
    }

    public SignatureScannerCodeLocationResult calculate(List<SignatureScannerReport> reports) {
        return new CalculateWaitableSignatureScanCodeLocations().calculateWaitableCodeLocations(reports);
    }

    public void assertWaited(SignatureScannerCodeLocationResult result, Set<String> waited, int notificationCount) {
        WaitableCodeLocationData waitable = result.getWaitableCodeLocationData();
        Assertions.assertTrue(SetUtils.isEqualSet(waitable.getSuccessfulCodeLocationNames(), waited));
        Assertions.assertEquals(waitable.getExpectedNotificationCount(), notificationCount);
    }

    public void assertNonWaited(SignatureScannerCodeLocationResult result, Set<String> nonWaited) {
        Assertions.assertTrue(SetUtils.isEqualSet(result.getNonWaitableCodeLocationData(), nonWaited));
    }
}
