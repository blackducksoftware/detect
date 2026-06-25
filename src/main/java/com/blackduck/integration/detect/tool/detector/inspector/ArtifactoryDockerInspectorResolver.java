package com.blackduck.integration.detect.tool.detector.inspector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.tool.cache.InstalledToolLocator;
import com.blackduck.integration.detect.tool.cache.InstalledToolManager;
import com.blackduck.integration.detect.workflow.airgap.AirGapInspectorPaths;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.docker.DockerDetectableOptions;
import com.blackduck.integration.detectable.detectables.docker.DockerInspectorInfo;
import com.blackduck.integration.detectable.detectables.docker.DockerInspectorResolver;
import com.blackduck.integration.exception.IntegrationException;

public class ArtifactoryDockerInspectorResolver implements DockerInspectorResolver {
    private static final String IMAGE_INSPECTOR_FAMILY = "blackduck-imageinspector";
    private static final List<String> inspectorNames = Arrays.asList("ubuntu", "alpine", "centos");
    private static final String INSTALLED_TOOL_JSON_KEY = "docker-inspector";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String DOCKER_SHARED_DIRECTORY_NAME = "docker";

    private final DirectoryManager directoryManager;
    private final AirGapInspectorPaths airGapInspectorPaths;
    private final FileFinder fileFinder;
    private final DockerInspectorInstaller dockerInspectorInstaller;
    private final DockerDetectableOptions dockerDetectableOptions;
    private final InstalledToolManager installedToolManager;
    private final InstalledToolLocator installedToolLocator;

    private DockerInspectorInfo resolvedInfo;

    public ArtifactoryDockerInspectorResolver(
        DirectoryManager directoryManager,
        AirGapInspectorPaths airGapInspectorPaths,
        FileFinder fileFinder,
        DockerInspectorInstaller dockerInspectorInstaller,
        DockerDetectableOptions dockerDetectableOptions,
        InstalledToolManager installedToolManager,
        InstalledToolLocator installedToolLocator
    ) {
        this.directoryManager = directoryManager;
        this.airGapInspectorPaths = airGapInspectorPaths;
        this.fileFinder = fileFinder;
        this.dockerInspectorInstaller = dockerInspectorInstaller;
        this.dockerDetectableOptions = dockerDetectableOptions;
        this.installedToolManager = installedToolManager;
        this.installedToolLocator = installedToolLocator;
    }

    @Override
    public DockerInspectorInfo resolveDockerInspector() throws DetectableException {
        try {
            if (resolvedInfo == null) {
                resolvedInfo = install();
            }
            return resolvedInfo;
        } catch (Exception e) {
            throw new DetectableException(e);
        }
    }

    private DockerInspectorInfo install() throws IntegrationException, IOException, DetectUserFriendlyException {
        Optional<File> airGapDockerFolder = airGapInspectorPaths.getDockerInspectorAirGapFile();
        // TODO: Handle null better.
        Optional<Path> providedJarPath = dockerDetectableOptions.getDockerInspectorPath();
        logger.warn("WARNING: Support for CentOS based Docker images are deprecated as of version 11.5.0 and support will be removed in 12.0.0.");

        if (providedJarPath.isPresent()) {
            logger.info("Docker tool will attempt to use the provided docker inspector.");
            return findProvidedJar(providedJarPath.get());
        } else if (airGapDockerFolder.isPresent()) {
            logger.info("Docker tool will attempt to use the air gapped docker inspector.");
            Optional<DockerInspectorInfo> airGapInspector = findAirGapInspector();
            return airGapInspector.orElse(null);
        } else {
            logger.info("Docker tool will attempt to download or find docker inspector.");
            File dockerDirectory = directoryManager.getPermanentDirectory(DOCKER_SHARED_DIRECTORY_NAME);
            // TODO: Handle null better.
            String dockerVersion = dockerDetectableOptions.getDockerInspectorVersion().orElse("");

            File inspector = null;
            Optional<File> cachedInstall = installedToolLocator.locateTool(INSTALLED_TOOL_JSON_KEY);
            try {
                inspector = dockerInspectorInstaller.installJar(dockerDirectory, Optional.of(dockerVersion));
            } catch (Exception e) {
                if (!cachedInstall.isPresent()) {
                    throw e;
                }
            }
            if (inspector == null) {
                if (cachedInstall.isPresent()) {
                    logger.debug("Using docker inspector from previous install.");
                    return new DockerInspectorInfo(cachedInstall.get());
                }
                return null;
            } else {
                installedToolManager.saveInstalledToolLocation(INSTALLED_TOOL_JSON_KEY, inspector.getAbsolutePath());
                return new DockerInspectorInfo(inspector);
            }
        }
    }

    private Optional<DockerInspectorInfo> findAirGapInspector() {
        return getAirGapJar().map(dockerInspectorJar1 -> new DockerInspectorInfo(dockerInspectorJar1, getAirGapInspectorImageTarfiles()));
    }

    private List<File> getAirGapInspectorImageTarfiles() {
        List<File> airGapInspectorImageTarfiles;
        airGapInspectorImageTarfiles = new ArrayList<>();
        String dockerInspectorAirGapPath = airGapInspectorPaths.getDockerInspectorAirGapPath()
            .map(Path::toString)
            .orElse(null);
        for (String inspectorName : inspectorNames) {
            File osImage = new File(dockerInspectorAirGapPath, IMAGE_INSPECTOR_FAMILY + "-" + inspectorName + ".tar");
            airGapInspectorImageTarfiles.add(osImage);
        }
        return airGapInspectorImageTarfiles;
    }

    private DockerInspectorInfo findProvidedJar(@NotNull Path providedJarPath) throws IntegrationException {
        File providedJar = null;

        logger.debug(String.format("Using user-provided docker inspector jar path: %s", providedJarPath));
        File providedJarCandidate = providedJarPath.toFile();
        if (providedJarCandidate.isFile()) {
            logger.debug(String.format("Found user-specified jar: %s", providedJarCandidate.getAbsolutePath()));
            providedJar = providedJarCandidate;
        } else {
            throw new IntegrationException(String.format("Provided Docker Inspector path (%s) does not exist or is not a file", providedJarCandidate.getAbsolutePath()));
        }

        return new DockerInspectorInfo(providedJar);
    }

    private Optional<File> getAirGapJar() {
        Optional<File> airGapDirPath = airGapInspectorPaths.getDockerInspectorAirGapFile();
        if (!airGapDirPath.isPresent()) {
            return Optional.empty();
        }

        logger.debug(String.format("Checking for air gap docker inspector jar file in: %s", airGapDirPath));
        try {
            List<File> possibleJars = fileFinder.findFiles(airGapDirPath.get(), "*.jar", false, 1);
            if (possibleJars == null || possibleJars.isEmpty()) {
                logger.error("Unable to locate air gap jar.");
                return Optional.empty();
            } else {
                File airGapJarFile = possibleJars.get(0);
                logger.info(String.format("Found air gap docker inspector: %s", airGapJarFile.getAbsolutePath()));
                return Optional.of(airGapJarFile);
            }
        } catch (Exception e) {
            logger.debug(String.format("Did not find a docker inspector jar file in the airgap dir: %s", airGapDirPath));
            return Optional.empty();
        }
    }
}
