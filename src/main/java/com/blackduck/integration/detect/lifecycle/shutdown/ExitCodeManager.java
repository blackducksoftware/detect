package com.blackduck.integration.detect.lifecycle.shutdown;

import java.util.ArrayList;
import java.util.List;

import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.workflow.event.Event;
import com.blackduck.integration.detect.workflow.event.EventSystem;

public class ExitCodeManager {
    private final List<ExitCodeRequest> exitCodeRequests = new ArrayList<>();
    private final ExceptionUtility exceptionUtility;

    public ExitCodeManager(EventSystem eventSystem, ExceptionUtility exitCodeUtility) {
        this.exceptionUtility = exitCodeUtility;
        eventSystem.registerListener(Event.ExitCode, this::addExitCodeRequest);
    }

    public void requestExitCode(Exception e) {
        requestExitCode(exceptionUtility.getExitCodeFromException(e));
    }

    public void requestExitCode(ExitCodeType exitCodeType) {
        exitCodeRequests.add(new ExitCodeRequest(exitCodeType));
    }

    public void addExitCodeRequest(ExitCodeRequest request) {
        exitCodeRequests.add(request);
    }

    public ExitCodeRequest getWinningExitCodeRequest() {
        ExitCodeType championExitCodeType = ExitCodeType.SUCCESS;
        ExitCodeRequest championExitCodeRequest = null;

        for (ExitCodeRequest exitCodeRequest : exitCodeRequests) {
            ExitCodeType thisRoundsWinner = ExitCodeType.getWinningExitCodeType(championExitCodeType, exitCodeRequest.getExitCodeType());

            if (thisRoundsWinner != championExitCodeType) {
                championExitCodeType = thisRoundsWinner;
                championExitCodeRequest = exitCodeRequest;
            }
        }
        return championExitCodeRequest;
    }
}
