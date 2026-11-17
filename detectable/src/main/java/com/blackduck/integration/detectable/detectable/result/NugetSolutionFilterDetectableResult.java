package com.blackduck.integration.detectable.detectable.result;

public class NugetSolutionFilterDetectableResult extends FailedDetectableResult {
    private final String reason;

    public NugetSolutionFilterDetectableResult(String reason) {
        this.reason = reason;
    }

    @Override
    public String toDescription() {
        return "Not applicable because a NuGet solution filter (.slnf) was provided: " + reason;
    }
}
