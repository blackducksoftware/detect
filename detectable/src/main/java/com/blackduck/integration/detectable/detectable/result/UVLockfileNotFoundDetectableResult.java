package com.blackduck.integration.detectable.detectable.result;

public class UVLockfileNotFoundDetectableResult extends FailedDetectableResult {
    private final String directoryPath;

    public UVLockfileNotFoundDetectableResult(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    @Override
    public String toDescription() {
        return String.format(
                "A pyproject.toml was located in %s, but the uv.lock or requirements.txt file was NOT located. Please check your configuration and try again.",
                directoryPath
        );
    }
}
