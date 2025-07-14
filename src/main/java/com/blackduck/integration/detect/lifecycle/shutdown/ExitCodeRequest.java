package com.blackduck.integration.detect.lifecycle.shutdown;

import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;

public class ExitCodeRequest {
    private final ExitCodeType exitCodeType;
    private final String reason;

    public ExitCodeRequest(ExitCodeType exitCodeType, String reason) {
        this.exitCodeType = exitCodeType;
        this.reason = reason;
    }

    public ExitCodeRequest(ExitCodeType exitCodeType) {
        this(exitCodeType, null);
    }

    public ExitCodeType getExitCodeType() {
        return exitCodeType;
    }

    public String getReason() {
        if (reason != null) {
            return reason;
        }
        return exitCodeType.getDescription();
    }
}
