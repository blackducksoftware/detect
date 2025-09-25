package com.blackduck.integration.detect.tool.signaturescanner.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.codelocation.CodeLocationOutput;
import com.blackduck.integration.blackduck.codelocation.Result;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.ScanCommandOutput;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScanPath;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerReport;
import com.blackduck.integration.detect.tool.signaturescanner.enums.SignatureScanStatusType;

public class CreateSignatureScanReports {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public List<SignatureScannerReport> createReports(List<SignatureScanPath> signatureScanPaths, List<ScanCommandOutput> scanCommandOutputList, Queue<String> failedScans) {
        List<SignatureScannerReport> signatureScannerReports = new ArrayList<>();
        for (SignatureScanPath signatureScanPath : signatureScanPaths) {
            Optional<ScanCommandOutput> scanCommandOutput = scanCommandOutputList.stream()
                .filter(output -> output.getScanTarget().equals(signatureScanPath.getTargetCanonicalPath()))
                .findFirst();
            SignatureScannerReport signatureScannerReport = createReport(signatureScanPath, scanCommandOutput.orElse(null), failedScans);
            signatureScannerReports.add(signatureScannerReport);
        }
        return signatureScannerReports;
    }

    public static SignatureScannerReport createReport(SignatureScanPath signatureScanPath, @Nullable ScanCommandOutput scanCommandOutput, Queue<String> failedScans) {
        SignatureScanStatusType statusType;

        if (scanCommandOutput == null) {
            statusType = SignatureScanStatusType.FAILURE;
        } else if (scanCommandOutput.getScanExitCode().isPresent() && scanCommandOutput.getScanExitCode().get() == 2) {
            statusType = SignatureScanStatusType.SKIPPED;
        } else if (Result.FAILURE.equals(scanCommandOutput.getResult())) {
            statusType = SignatureScanStatusType.FAILURE;
        } else if (failedScans.contains(scanCommandOutput.getCodeLocationName())) {
            statusType = SignatureScanStatusType.SCASS_ERROR; 
        } else {
            statusType = SignatureScanStatusType.SUCCESS;
        }

        Optional<ScanCommandOutput> optionalOutput = Optional.ofNullable(scanCommandOutput);
        boolean hasOutput = optionalOutput.isPresent();
        Integer exitCode = optionalOutput
            .map(ScanCommandOutput::getScanExitCode)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .orElse(null);
        Exception exception = optionalOutput
            .map(ScanCommandOutput::getException)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .orElse(null);
        String errorMessage = optionalOutput
            .map(ScanCommandOutput::getErrorMessage)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .orElse(null);

        String codeLocationName = optionalOutput.map(CodeLocationOutput::getCodeLocationName).orElse(null);
        Integer notificationCounts = optionalOutput.map(CodeLocationOutput::getExpectedNotificationCount).orElse(null);

        return new SignatureScannerReport(signatureScanPath, statusType, exitCode, exception, errorMessage, hasOutput, codeLocationName, notificationCounts);
    }
}
