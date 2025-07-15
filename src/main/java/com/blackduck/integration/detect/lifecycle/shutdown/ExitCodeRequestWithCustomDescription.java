package com.blackduck.integration.detect.lifecycle.shutdown;

import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;

public class ExitCodeRequestWithCustomDescription extends ExitCodeRequest {

    public ExitCodeRequestWithCustomDescription(ExitCodeType exitCodeType, String reason) {
        super(exitCodeType, reason);
    }
}
