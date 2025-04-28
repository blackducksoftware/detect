package com.blackduck.integration.configuration.config;

import java.util.HashSet;
import java.util.Set;

public class ExternalProperties {

    public enum DockerPassthroughProperties {
        BDIO_PROJECT_NAME("bdio.project.name"),
        BDIO_PROJECT_VERSION("bdio.project.version"),
        BDIO_CODELOCATION_PREFIX("bdio.codelocation.prefix"),
        BDIO_CODELOCATION_NAME("bdio.codelocation.name"),
        WORKING_DIR_PATH("working.dir.path"),
        SYSTEM_PROPERTIES_PATH("system.properties.path"),
        CLEANUP_WORKING_DIR("cleanup.working.dir"),
        TARGET_IMAGE_LINUX_DISTRO_OVERRIDE("linux.distro"),
        COMMAND_TIMEOUT("command.timeout"),
        SERVICE_TIMEOUT("service.timeout"),
        LOGGING_LEVEL("logging.level.detect"),
        OUTPUT_PATH("output.path"),
        OUTPUT_INCLUDE_CONTAINERFILESYSTEM("output.include.containerfilesystem"),
        OUTPUT_INCLUDE_SQUASHEDIMAGE("output.include.squashedimage"),
        CONTAINER_FILESYSTEM_EXCLUDED_PATHS("output.containerfilesystem.excluded.paths"),
        USE_PLATFORM_DEFAULT_DOCKER_HOST("use.platform.default.docker.host"),
        DOCKER_IMAGE("docker.image"),
        DOCKER_IMAGE_PLATFORM("docker.image.platform"),
        DOCKER_TAR("docker.tar"),
        DOCKER_IMAGE_ID("docker.image.id"),
        DOCKER_IMAGE_REPO("docker.image.repo"),
        DOCKER_PLATFORM_TOP_LAYER_ID("docker.platform.top.layer.id"),
        DOCKER_IMAGE_TAG("docker.image.tag"),
        CALLER_NAME("caller.name"),
        CALLER_VERSION("caller.version"),
        INSPECTOR_REPOSITORY("inspector.repository"),
        INSPECTOR_IMAGE_FAMILY("inspector.image.family"),
        INSPECTOR_IMAGE_VERSION("inspector.image.version"),
        CLEANUP_TARGET_IMAGE("cleanup.target.image"),
        CLEANUP_INSPECTOR_CONTAINER("cleanup.inspector.container"),
        CLEANUP_INSPECTOR_IMAGE("cleanup.inspector.image"),
        ORGANIZE_COMPONENTS_BY_LAYER("bdio.organize.components.by.layer"),
        INCLUDE_REMOVED_COMPONENTS("bdio.include.removed.components"),
        SHARED_DIR_PATH_LOCAL("shared.dir.path.local"),
        SHARED_DIR_PATH_IMAGEINSPECTOR("shared.dir.path.imageinspector"),
        IMAGEINSPECTOR_SERVICE_URL("imageinspector.service.url"),
        IMAGEINSPECTOR_SERVICE_START("imageinspector.service.start"),
        IMAGEINSPECTOR_CONTAINER_PORT_ALPINE("imageinspector.service.container.port.alpine"),
        IMAGEINSPECTOR_CONTAINER_PORT_CENTOS("imageinspector.service.container.port.centos"),
        IMAGEINSPECTOR_CONTAINER_PORT_UBUNTU("imageinspector.service.container.port.ubuntu"),
        IMAGEINSPECTOR_HOST_PORT_ALPINE("imageinspector.service.port.alpine"),
        IMAGEINSPECTOR_HOST_PORT_CENTOS("imageinspector.service.port.centos"),
        IMAGEINSPECTOR_HOST_PORT_UBUNTU("imageinspector.service.port.ubuntu"),
        IMAGEINSPECTOR_DEFAULT_DISTRO("imageinspector.service.distro.default"),
        IMAGEINSPECTOR_SERVICE_LOG_LENGTH("imageinspector.service.log.length"),
        OFFLINE_MODE("offline.mode"),
        HELP_OUTPUT_PATH("help.output.path"),
        HELP_INPUT_PATH("help.input.path");

        private static final String PREFIX = "detect.docker.passthrough.";
        private final String key;

        DockerPassthroughProperties(String key) {
            this.key = PREFIX + key;
        }

        public String getKey() {
            return key;
        }
    }

    public enum PhoneHomePassthroughProperties {
        DESKTOP_PLATFORM_NAME("desktop.platformname"),
        DESKTOP_PLATFORM_RELEASE("desktop.platformrelease"),
        DESKTOP_VERSION("desktop.version");

        private static final String PREFIX = "detect.phone.home.passthrough.";
        private final String key;

        PhoneHomePassthroughProperties(String key) {
            this.key = PREFIX + key;
        }

        public String getKey() {
            return key;
        }
    }

    public static Set<String> getAllExternalPropertyKeys() {
        Set<String> keys = new HashSet<>();
        for (DockerPassthroughProperties dockerPassthroughProperty : DockerPassthroughProperties.values()) {
            keys.add(dockerPassthroughProperty.getKey());
        }
        for (PhoneHomePassthroughProperties phoneHomePassthroughProperty : PhoneHomePassthroughProperties.values()) {
            keys.add(phoneHomePassthroughProperty.getKey());
        }
        return keys;
    }
}