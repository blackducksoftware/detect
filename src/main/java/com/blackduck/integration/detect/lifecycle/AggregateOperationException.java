package com.blackduck.integration.detect.lifecycle;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
public class AggregateOperationException extends OperationException {
    private final List<Exception> exceptions;

    public AggregateOperationException(List<Exception> exceptions) {
        super(new RuntimeException(exceptions.get(0)));
        this.exceptions = new ArrayList<>(exceptions);
    }

    public List<Exception> getExceptions() {
        return Collections.unmodifiableList(exceptions);
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("Multiple exceptions occurred:\n");
        for (int i = 0; i < exceptions.size(); i++) {
            sb.append(String.format("  %d. %s: %s\n", i+1,
                    exceptions.get(i).getClass().getSimpleName(),
                    exceptions.get(i).getMessage()));
        }
        return sb.toString();
    }

}
