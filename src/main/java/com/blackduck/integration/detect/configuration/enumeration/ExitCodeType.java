package com.blackduck.integration.detect.configuration.enumeration;

public enum ExitCodeType {
    SUCCESS(0, "Detect exited successfully.", false),
    FAILURE_BLACKDUCK_CONNECTIVITY(1, "Detect was unable to connect to Black Duck. Check your configuration and connection.", false),
    FAILURE_TIMEOUT(2, "Detect was unable to wait for actions to be completed on Black Duck. Check your Black Duck server or increase your timeout.", false),
    FAILURE_POLICY_VIOLATION(3, "Detect found policy violations.", false),
    FAILURE_POLICY_NAME_VIOLATION(3, "Detect found policy name violations.", true),
    FAILURE_PROXY_CONNECTIVITY(4, "Detect was unable to use the configured proxy. Check your configuration and connection.", false),
    FAILURE_DETECTOR(5, "Detect had one or more detector failures while extracting dependencies. Check that all projects build and your environment is configured correctly.", false),
    FAILURE_SCAN(6, "Detect was unable to run the signature scanner against your source. Check your configuration.", false),
    FAILURE_CONFIGURATION(7, "Detect was unable to start due to issues with it's configuration. Check and fix your configuration.", false),
    FAILURE_DETECTOR_REQUIRED(9, "Detect did not run all of the required detectors. Fix detector issues or disable required detectors.", false),
    FAILURE_BLACKDUCK_VERSION_NOT_SUPPORTED(
        10,
        "Detect's configuration requires a Black Duck capability that is not supported by your version of Black Duck. Ensure that your Black Duck version is compatible with this version of Detect."
            , false),
    FAILURE_BLACKDUCK_FEATURE_ERROR(
        11,
        "Detect encountered an error while attempting an operation on Black Duck. Ensure that your Black Duck version is compatible with this version of Detect, and that your Black Duck user account has the required roles."
            , false),
    FAILURE_MINIMUM_INTERVAL_NOT_MET(13, "Detect did not wait the minimum required scan interval.", false),
    FAILURE_IAC(
        14,
        "Detect was unable to perform IaC Scan against your source. Please check your configuration, and see logs and IaC Scanner documentation for more information."
            , false),

    FAILURE_ACCURACY_NOT_MET(15, "Detect was unable to meet the required accuracy.", false),
    FAILURE_OUT_OF_MEMORY(16, "Detect encountered an Out of Memory error. Please review memory settings and system resources.", 0.5, false),

    FAILURE_IMAGE_NOT_AVAILABLE(20, "Image scan attempted but no return data available.", false),
    FAILURE_BLACKDUCK_DUPLICATE_PROJECT_ERROR(21, "Project name already exists, unable to create project.", false),

    FAILURE_COMPONENT_LOCATION_ANALYSIS(25, "Component Location Analysis failed.", false),
    FAILURE_BOM_PREPARATION(30, "Black Duck failed to prepare BOM for the scan.", false),
    FAILURE_GENERAL_ERROR(99, "Detect encountered a known error, details of the error are provided.", false),
    FAILURE_UNKNOWN_ERROR(100, "Detect encountered an unknown error.", false);

    private final int exitCode;
    private String description;
    private final double priority;
    private final boolean descriptionIsDynamic;

    ExitCodeType(int exitCode, String description, boolean descriptionIsDynamic) {
        this(exitCode, description, (double) exitCode, descriptionIsDynamic);
    }

    ExitCodeType(int exitCode, String description, double priority, boolean descriptionIsDynamic) {
        this.exitCode = exitCode;
        this.description = description;
        this.priority = priority;
        this.descriptionIsDynamic = descriptionIsDynamic;
    }

    /**
     * A failure always beats a success. Among failures:
     * - The one with a lower priority wins.
     * - If priorities are equal, the one with a lower exit code wins.
     */
    public static ExitCodeType getWinningExitCodeType(ExitCodeType first, ExitCodeType second) {
        if (first.isSuccess()) {
            return second;
        } else if (second.isSuccess()) {
            return first;
        } else {
            return (first.getPriority() < second.getPriority()) ? first : second;
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

    public void setDescription(String description) {
        if (descriptionIsDynamic) {
            this.description = description;
        }
    }

    public double getPriority() { return priority; }
}
