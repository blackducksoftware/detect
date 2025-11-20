package com.blackduck.integration.detect.tool.cdxgen;

import java.io.File;

import org.jetbrains.annotations.Nullable;

/**
 * Result of a cdxgen execution.
 */
public class CdxgenResult {
    private final boolean successful;
    private final String errorMessage;
    private final File sbomFile;

    private CdxgenResult(boolean successful, @Nullable String errorMessage, @Nullable File sbomFile) {
        this.successful = successful;
        this.errorMessage = errorMessage;
        this.sbomFile = sbomFile;
    }

    public static CdxgenResult success(File sbomFile) {
        return new CdxgenResult(true, null, sbomFile);
    }

    public static CdxgenResult failure(String errorMessage) {
        return new CdxgenResult(false, errorMessage, null);
    }

    public boolean isSuccessful() {
        return successful;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @Nullable
    public File getSbomFile() {
        return sbomFile;
    }
}
