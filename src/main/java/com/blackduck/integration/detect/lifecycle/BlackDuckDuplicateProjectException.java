package com.blackduck.integration.detect.lifecycle;

public class BlackDuckDuplicateProjectException extends OperationException {
    private static final long serialVersionUID = 1L;

    public BlackDuckDuplicateProjectException(Exception exception) {
        super(exception);
    }
}
