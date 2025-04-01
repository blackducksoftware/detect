package com.blackduck.integration.detect.configuration.enumeration;

public enum DetectTool {
    DETECTOR,
    SIGNATURE_SCAN,
    BINARY_SCAN,
    IMPACT_ANALYSIS,
    DOCKER,
    BAZEL,
    IAC_SCAN,
    CONTAINER_SCAN,
    /**
     * @deprecated in v10.4.0, will be removed in v11.0.0
     */
    @Deprecated
    THREAT_INTEL,
    COMPONENT_LOCATION_ANALYSIS
}
