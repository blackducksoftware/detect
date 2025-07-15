package com.blackduck.integration.detect.lifecycle.shutdown;

import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;

public class ExitCodeRequest {
    private final ExitCodeType exitCodeType;

    public ExitCodeRequest(ExitCodeType exitCodeType) {
        this.exitCodeType = exitCodeType;
    }

    public ExitCodeType getExitCodeType() {
        return exitCodeType;
    }

    public String getReason() {
        return exitCodeType.getDescription();
    }
}
