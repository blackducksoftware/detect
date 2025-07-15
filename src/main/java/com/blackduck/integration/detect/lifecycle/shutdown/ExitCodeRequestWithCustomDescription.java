package com.blackduck.integration.detect.lifecycle.shutdown;

import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;

public class ExitCodeRequestWithCustomDescription extends ExitCodeRequest {

    private String customDescription;
    public ExitCodeRequestWithCustomDescription(ExitCodeType exitCodeType, String reason) {
        super(exitCodeType);
        this.customDescription = reason;
    }

    @Override
    public String getReason() {
        return customDescription;
    }
}
