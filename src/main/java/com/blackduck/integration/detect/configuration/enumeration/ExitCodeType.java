package com.blackduck.integration.detect.configuration.enumeration;

public enum ExitCodeType {
    FAILURE_OUT_OF_MEMORY(-1, "Detect encountered an Out of Memory error. Please review memory settings and system resources."),
    SUCCESS(0, "Detect exited successfully."),
    FAILURE_BLACKDUCK_CONNECTIVITY(1, "Detect was unable to connect to Black Duck. Check your configuration and connection."),
    FAILURE_TIMEOUT(2, "Detect was unable to wait for actions to be completed on Black Duck. Check your Black Duck server or increase your timeout."),
    FAILURE_POLICY_VIOLATION(3, "Detect found policy violations."),
    FAILURE_PROXY_CONNECTIVITY(4, "Detect was unable to use the configured proxy. Check your configuration and connection."),
    FAILURE_DETECTOR(5, "Detect had one or more detector failures while extracting dependencies. Check that all projects build and your environment is configured correctly."),
    FAILURE_SCAN(6, "Detect was unable to run the signature scanner against your source. Check your configuration."),
    FAILURE_CONFIGURATION(7, "Detect was unable to start due to issues with it's configuration. Check and fix your configuration."),
    FAILURE_DETECTOR_REQUIRED(9, "Detect did not run all of the required detectors. Fix detector issues or disable required detectors."),
    FAILURE_BLACKDUCK_VERSION_NOT_SUPPORTED(
        10,
        "Detect's configuration requires a Black Duck capability that is not supported by your version of Black Duck. Ensure that your Black Duck version is compatible with this version of Detect."
    ),
    FAILURE_BLACKDUCK_FEATURE_ERROR(
        11,
        "Detect encountered an error while attempting an operation on Black Duck. Ensure that your Black Duck version is compatible with this version of Detect, and that your Black Duck user account has the required roles."
    ),
    FAILURE_MINIMUM_INTERVAL_NOT_MET(13, "Detect did not wait the minimum required scan interval."),
    FAILURE_IAC(
        14,
        "Detect was unable to perform IaC Scan against your source. Please check your configuration, and see logs and IaC Scanner documentation for more information."
    ),

    FAILURE_ACCURACY_NOT_MET(15, "Detect was unable to meet the required accuracy."),

    FAILURE_IMAGE_NOT_AVAILABLE(20, "Image scan attempted but no return data available."),

    FAILURE_COMPONENT_LOCATION_ANALYSIS(25, "Component Location Analysis failed."),

    FAILURE_GENERAL_ERROR(99, "Detect encountered a known error, details of the error are provided."),
    FAILURE_UNKNOWN_ERROR(100, "Detect encountered an unknown error.");

    private final int exitCode;
    private final String description;

    ExitCodeType(int exitCode, String description) {
        this.exitCode = exitCode;
        this.description = description;
    }

    /**
     * A failure always beats a success and a failure with a lower exit code beats a failure with a higher exit code.
     */
    public static ExitCodeType getWinningExitCodeType(ExitCodeType first, ExitCodeType second) {
        if (first.isSuccess()) {
            return second;
        } else if (second.isSuccess()) {
            return first;
        } else {
            if (first.getExitCode() < second.getExitCode()) {
                return first;
            } else {
                return second;
            }
        }
    }

    public int getExitCode() {
        return exitCode;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public String getDescription() {
        return description;
    }
}
