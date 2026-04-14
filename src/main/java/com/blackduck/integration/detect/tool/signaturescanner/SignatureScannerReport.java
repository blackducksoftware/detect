package com.blackduck.integration.detect.tool.signaturescanner;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.detect.tool.signaturescanner.enums.SignatureScanStatusType;

public class SignatureScannerReport {
    private final SignatureScanPath signatureScanPath;
    private final SignatureScanStatusType statusType;
    @Nullable
    private final Integer exitCode;
    @Nullable
    private final Exception exception;
    @Nullable
    private final String errorMessage;
    @Nullable
    private final boolean hasOutput;
    @Nullable
    private final String codeLocationName;
    @Nullable
    private final Integer expectedNotificationCount;

    public SignatureScannerReport(
        SignatureScanPath signatureScanPath,
        SignatureScanStatusType statusType,
        @Nullable Integer exitCode,
        @Nullable Exception exception,
        @Nullable String errorMessage,
        boolean hasOutput,
        @Nullable String codeLocationName,
        @Nullable Integer expectedNotificationCount
    ) {
        this.signatureScanPath = signatureScanPath;
        this.statusType = statusType;
        this.exitCode = exitCode;
        this.exception = exception;
        this.errorMessage = errorMessage;
        this.hasOutput = hasOutput;
        this.codeLocationName = codeLocationName;
        this.expectedNotificationCount = expectedNotificationCount;
    }

    public SignatureScanPath getSignatureScanPath() {
        return signatureScanPath;
    }

    public SignatureScanStatusType getStatusType() {
        return statusType;
    }

    public boolean isSuccessful() {
        return SignatureScanStatusType.SUCCESS.equals(statusType);
    }

    public boolean isFailure() {
        return SignatureScanStatusType.FAILURE.equals(statusType);
    }

    public boolean isSkipped() {
        return SignatureScanStatusType.SKIPPED.equals(statusType);
    }
    
    public boolean isScassError() {
        return SignatureScanStatusType.SCASS_ERROR.equals(statusType);
    }    

    public Optional<Integer> getExitCode() {
        return Optional.ofNullable(exitCode);
    }

    public Optional<Exception> getException() {
        return Optional.ofNullable(exception);
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    public boolean hasOutput() {
        return hasOutput;
    }

    public Optional<String> getCodeLocationName() {
        return Optional.ofNullable(codeLocationName);
    }

    public Optional<Integer> getExpectedNotificationCount() {
        return Optional.ofNullable(expectedNotificationCount);
    }
}
