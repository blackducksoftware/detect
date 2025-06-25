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

    public ExitCodeType getWinningExitCode() {
        ExitCodeType championExitCodeType = ExitCodeType.SUCCESS;
        // match exitcodetype with its corresponding exitcoderequest which has reason
        ExitCodeRequest winningRequest = null;
        for (ExitCodeRequest exitCodeRequest : exitCodeRequests) { // exitCodeRequest has reason
            ExitCodeType nextContender = exitCodeRequest.getExitCodeType();
            ExitCodeType thisRoundsWinner = ExitCodeType.getWinningExitCodeType(championExitCodeType, nextContender);

            // dethrone
            if (thisRoundsWinner != championExitCodeType) {
                championExitCodeType = thisRoundsWinner;
                winningRequest = exitCodeRequest;
            }
        }

        if (winningRequest != null && winningRequest.getReason() != null) {
            championExitCodeType.setDescription(winningRequest.getReason());
        }
        return championExitCodeType;
    }
}
