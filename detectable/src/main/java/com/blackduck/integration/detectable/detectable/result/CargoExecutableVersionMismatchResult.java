package com.blackduck.integration.detectable.detectable.result;

public class CargoExecutableVersionMismatchResult extends FailedDetectableResult {
    private final String directoryPath;
    private final String minimumVersion;

    public CargoExecutableVersionMismatchResult(String directoryPath, String minimumVersion) {
        this.directoryPath = directoryPath;
        this.minimumVersion = minimumVersion;
    }

    @Override
    public String toDescription() {
        return String.format(
            "A Cargo.toml was located in %s, but cargo version is less than %s",
            directoryPath,
            minimumVersion
        );
    }
}
