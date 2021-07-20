/*
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.workflow.status;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class OperationSystem {
    private final Map<String, Operation> operationMap = new HashMap<>(); //TODO: May no longer need it to be a map.
    private final StatusEventPublisher statusEventPublisher;

    public OperationSystem(StatusEventPublisher statusEventPublisher) {
        this.statusEventPublisher = statusEventPublisher;
    }

    public void publishOperations() {
        operationMap.values().forEach(this::publishOperation);
    }

    public void publishOperation(Operation operation) {
        if (operation.getErrorMessages().length > 0) {
            statusEventPublisher.publishIssue(new DetectIssue(DetectIssueType.EXCEPTION, operation.getName(), Arrays.asList(operation.getErrorMessages())));
        }
        statusEventPublisher.publishOperation(operation);
    }

    public Operation startOperation(String operationName, OperationType type) {
        return startOperation(operationName, type, null);
    }

    public Operation startOperation(String operationName, OperationType type, @Nullable String phoneHomeKey) {
        Operation currentOperation = operationMap.computeIfAbsent(operationName, key -> new Operation(operationName, type, phoneHomeKey));
        if (currentOperation.getEndTime().isPresent()) {
            publishOperation(currentOperation);
            currentOperation = new Operation(operationName, type, phoneHomeKey);
            operationMap.put(operationName, currentOperation);
        } else {
            statusEventPublisher.publishOperationStart(currentOperation);
        }
        return currentOperation;
    }

}
