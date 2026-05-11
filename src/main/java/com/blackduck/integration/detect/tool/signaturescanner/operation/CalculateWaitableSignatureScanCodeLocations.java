package com.blackduck.integration.detect.tool.signaturescanner.operation;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.blackduck.integration.common.util.Bds;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerCodeLocationResult;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerReport;
import com.blackduck.integration.detect.workflow.blackduck.codelocation.WaitableCodeLocationData;

public class CalculateWaitableSignatureScanCodeLocations {
    public SignatureScannerCodeLocationResult calculateWaitableCodeLocations(List<SignatureScannerReport> reports) {

        WaitableCodeLocationData waitableCodeLocationData = calculateWaitable(reports);
        Set<String> nonWaitableNames = calculateNonWaitable(reports);

        return new SignatureScannerCodeLocationResult(waitableCodeLocationData, nonWaitableNames);
    }

    private WaitableCodeLocationData calculateWaitable(List<SignatureScannerReport> reports) {
        List<SignatureScannerReport> waitableScans = Bds.of(reports)
            .filter(SignatureScannerReport::isSuccessful)
            .toList();

        int totalExpected = waitableScans.stream()
            .map(SignatureScannerReport::getExpectedNotificationCount)
            .filter(Optional::isPresent)
            .mapToInt(Optional::get)
            .sum();

        Set<String> allNames = waitableScans.stream()
            .map(SignatureScannerReport::getCodeLocationName)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());

        return new WaitableCodeLocationData(DetectTool.SIGNATURE_SCAN, totalExpected, allNames);
    }

    private Set<String> calculateNonWaitable(List<SignatureScannerReport> reports) {
        return Bds.of(reports)
            .filter(report -> report.isSkipped() || report.isFailure())
            .map(SignatureScannerReport::getCodeLocationName)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toSet();
    }
}
