package com.blackduck.integration.detect.lifecycle.shutdown;

import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.workflow.event.Event;
import com.blackduck.integration.detect.workflow.event.EventSystem;

public class ExitCodePublisher {
    private final EventSystem eventSystem;

    public ExitCodePublisher(EventSystem eventSystem) {
        this.eventSystem = eventSystem;
    }

    public void publishExitCode(ExitCodeRequest exitCodeRequest) {
        eventSystem.publishEvent(Event.ExitCode, exitCodeRequest);
    }

    public void publishExitCode(ExitCodeType exitCodeType, String reason) {
        publishExitCode(new ExitCodeRequest(exitCodeType, reason));
    }
}
