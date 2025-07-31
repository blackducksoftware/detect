package com.blackduck.integration.detector.base;

public enum DetectorStatusCode {
    ATTEMPTED("Detector attempted to run but did not succeed."),
    CARGO_LOCKFILE_NOT_FOUND("A Cargo.toml was located in the target project, but the Cargo.lock file was NOT located."),
    CARTFILE_RESOLVED_FILE_NOT_FOUND("A Cartfile was located in the target project, but the Cartfile.resolved file was NOT located."),
    EXCEPTION("An exception occurred."),
    EXCLUDED("Detector type was excluded."),
    EXECUTABLE_FAILED("During extraction, one or more executables did not execute successfully."),
    EXTRACTION_FAILED("During extraction, one or more exceptions were encountered."),
    EXECUTABLE_VERSION_MISMATCH("Unexpected executable version was encountered."),
    EXECUTABLE_TERMINATED_LIKELY_OUT_OF_MEMORY("An executable was terminated, potentially due to a runtime environment low memory condition."),
    EXECUTABLE_NOT_FOUND("The necessary executable was not found."),
    EXECUTABLES_NOT_FOUND("The necessary executables were not found."),
    FAILED("Detector failed."),
    FILES_NOT_FOUND("Necessary files were not found within the target project."),
    FILE_NOT_FOUND("A file was not found within the target project."),
    FORCED_NESTED_PASSED("Forced to pass because nested forced by user."),
    GO_PKG_LOCKFILE_NOT_FOUND("A Gopkg.toml was located in the target project, but the Gopkg.lock file was NOT located."),
    INSPECTOR_NOT_FOUND("The necessary inspector was not found"),
    MAX_DEPTH_EXCEEDED("Max depth was exceeded."),
    NOT_NESTABLE("Not nestable and a detector already applied in parent directory."),
    NOT_NESTABLE_BENEATH("Nestable but another detector prevented nesting."),
    NPM_NODE_MODULES_NOT_FOUND("A package.json was located in the target project, but the node_modules folder was NOT located."),
    PASSED("Detector passed."),
    PIPFILE_LOCK_NOT_FOUND("A Pipfile was located in the target project, but a Pipfile.lock was NOT located."),
    POETRY_LOCKFILE_NOT_FOUND("A pyproject.toml was located in the target project, but the Poetry.lock file was NOT located."),
    POORLY_FORMATTED_JSON("Attempted to parse a JSON file but the contents did not conform to the JSON specification."),
    PROPERTY_INSUFFICIENT("The properties are insufficient to run."),
    PUBSPEC_LOCK_NOT_FOUND("A pubspec.yaml file was found, but a pubspec.lock file was NOT found."),
    SECTION_NOT_FOUND("A necessary section was not found within a file within the target project."),
    SETUP_TOOLS_REQUIRES_NOT_FOUND("The necessary requires setuptools statement is missing from the pyproject.toml."),
    SETUP_TOOLS_NO_DEPENDENCIES("No dependencies found in pyproject.toml, setup.cfg, or setup.py."),
    SBT_PLUGIN_MISSING("A dependency graph plugin must be installed for the SBT detector to run."),
    UNKNOWN_DETECTOR_RESULT("There was an unknown result."),
    WRONG_OPERATING_SYSTEM_RESULT("Cannot run on the used operating system."),
    YIELDED("Yielded to other detectors."),
    UV_LOCKFILE_NOT_FOUND("A pyproject.toml was found in the root directory, but a uv.lock file or requirements.txt were NOT found.");

    private final String description;

    DetectorStatusCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
