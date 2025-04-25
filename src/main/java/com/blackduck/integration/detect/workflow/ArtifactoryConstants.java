package com.blackduck.integration.detect.workflow;

public class ArtifactoryConstants {
    public static final String ARTIFACTORY_URL = "https://repo.blackduck.com/";
    public static final String VERSION_PLACEHOLDER = "<VERSION>";

    public static final String NUGET_INSPECTOR_PROPERTY_REPO = "bds-integrations-release/com/blackduck/integration/detect";
    public static final String NUGET_INSPECTOR_MAC_PROPERTY = "NUGET_INSPECTOR_MAC_LATEST_1";
    public static final String NUGET_INSPECTOR_LINUX_PROPERTY = "NUGET_INSPECTOR_LINUX_LATEST_1";
    public static final String NUGET_INSPECTOR_WINDOWS_PROPERTY = "NUGET_INSPECTOR_WINDOWS_LATEST_1";

    public static final String DOCKER_INSPECTOR_REPO = "bds-integrations-release/com/blackduck/integration/detect-docker-inspector";
    public static final String DOCKER_INSPECTOR_PROPERTY = "DOCKER_INSPECTOR_LATEST_11";
    public static final String DOCKER_INSPECTOR_AIR_GAP_PROPERTY = "DOCKER_INSPECTOR_AIR_GAP_LATEST_11";
    public static final String DOCKER_INSPECTOR_VERSION_OVERRIDE =
        "/" + ArtifactoryConstants.VERSION_PLACEHOLDER + "/detect-docker-inspector-" + ArtifactoryConstants.VERSION_PLACEHOLDER + ".jar";

    public static final String PROJECT_INSPECTOR_PROPERTY_REPO = "bds-integrations-release/com/blackduck/integration/detect";
    public static final String PROJECT_INSPECTOR_MAC_PROPERTY = "PROJECT_INSPECTOR_MAC_LATEST_2";
    public static final String PROJECT_INSPECTOR_LINUX_PROPERTY = "PROJECT_INSPECTOR_LINUX_LATEST_2";
    public static final String PROJECT_INSPECTOR_WINDOWS_PROPERTY = "PROJECT_INSPECTOR_WINDOWS_LATEST_2";

    public static final String FONTS_REPO = "bds-integrations-release/com/blackduck/integration/detect";
    public static final String FONTS_PROPERTY = "DETECT_FONT_BUNDLE_LATEST_7";
}
