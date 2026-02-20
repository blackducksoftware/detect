package com.blackduck.integration.detectable.detectable.result;

public class IvyDependencyTreeNotFoundDetectableResult extends FailedDetectableResult {
    @Override
    public String toDescription() {
        return "build.xml does not contain an ivy:dependencytree task. Please add a target with <ivy:dependencytree log=\"download-only\" /> task.";
    }
}