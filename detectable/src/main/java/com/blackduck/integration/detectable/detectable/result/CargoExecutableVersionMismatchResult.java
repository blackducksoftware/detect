package com.blackduck.integration.detectable.detectable.result;

public class CargoExecutableVersionMismatchResult extends FailedDetectableResult {
    private final String directoryPath;

    public CargoExecutableVersionMismatchResult(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    @Override
    public String toDescription() {
        return String.format(
            "A Cargo.toml was located in %s, but cargo version is less than 1.44.0",
            directoryPath
        );
    }
}
