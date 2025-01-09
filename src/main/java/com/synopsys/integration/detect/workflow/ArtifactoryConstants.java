package com.synopsys.integration.detect.workflow;

public class ArtifactoryConstants {
    public static final String ARTIFACTORY_URL = "https://dev.repo.blackduck.com/";
    public static final String ARTIFACTORY_FALLBACK_URL = "https://sig-repo.synopsys.com/";
    public static final String VERSION_PLACEHOLDER = "<VERSION>";

    public static final String GRADLE_INSPECTOR_MAVEN_REPO = "bds-integration-public-cache/";

    public static final String NUGET_INSPECTOR_PROPERTY_REPO = "bds-integrations-release/com/synopsys/integration/synopsys-detect";
    public static final String NUGET_INSPECTOR_MAC_PROPERTY = "NUGET_INSPECTOR_MAC_LATEST_1";
    public static final String NUGET_INSPECTOR_LINUX_PROPERTY = "NUGET_INSPECTOR_LINUX_LATEST_1";
    public static final String NUGET_INSPECTOR_WINDOWS_PROPERTY = "NUGET_INSPECTOR_WINDOWS_LATEST_1";

    public static final String DOCKER_INSPECTOR_REPO = "bds-integrations-release/com/synopsys/integration/detect-docker-inspector";
    public static final String DOCKER_INSPECTOR_PROPERTY = "DOCKER_INSPECTOR_LATEST_10";
    public static final String DOCKER_INSPECTOR_AIR_GAP_PROPERTY = "DOCKER_INSPECTOR_AIR_GAP_LATEST_10";
    public static final String DOCKER_INSPECTOR_VERSION_OVERRIDE =
        "/" + ArtifactoryConstants.VERSION_PLACEHOLDER + "/detect-docker-inspector-" + ArtifactoryConstants.VERSION_PLACEHOLDER + ".jar";

    public static final String PROJECT_INSPECTOR_PROPERTY_REPO = "bds-integrations-release/com/synopsys/integration/synopsys-detect";
    public static final String PROJECT_INSPECTOR_MAC_PROPERTY = "PROJECT_INSPECTOR_MAC_LATEST_2";
    public static final String PROJECT_INSPECTOR_LINUX_PROPERTY = "PROJECT_INSPECTOR_LINUX_LATEST_2";
    public static final String PROJECT_INSPECTOR_WINDOWS_PROPERTY = "PROJECT_INSPECTOR_WINDOWS_LATEST_2";

    public static final String FONTS_REPO = "bds-integrations-release/com/synopsys/integration/synopsys-detect";
    public static final String FONTS_PROPERTY = "DETECT_FONT_BUNDLE_LATEST_7";
}
