package com.blackduck.integration.detect.lifecycle.run.step.container;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.version.BlackDuckVersion;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.step.CommonScanStepRunner;
import com.blackduck.integration.detect.workflow.codelocation.CodeLocationNameManager;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.exception.IntegrationTimeoutException;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;

public abstract class AbstractContainerScanStepRunner {

    protected final OperationRunner operationRunner;
    protected final String scanType = CommonScanStepRunner.CONTAINER;
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final NameVersion projectNameVersion;
    protected final BlackDuckRunData blackDuckRunData;
    protected final File containerRunDirectory;
    protected final File containerImage;
    protected final Gson gson;
    protected String codeLocationName;
    private static final BlackDuckVersion MIN_BLACK_DUCK_VERSION = new BlackDuckVersion(2023, 10, 0);

    public AbstractContainerScanStepRunner(OperationRunner operationRunner, NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData, Gson gson)
        throws IntegrationException, OperationException {
        this.operationRunner = operationRunner;
        this.projectNameVersion = projectNameVersion;
        this.blackDuckRunData = blackDuckRunData;
        containerRunDirectory = operationRunner.getDirectoryManager().getContainerOutputDirectory();
        if (containerRunDirectory == null || !containerRunDirectory.exists()) {
            throw new IntegrationException("Container run directory does not exist.");
        }
        containerImage = operationRunner.getContainerScanImage(gson, containerRunDirectory);
        this.gson = gson;
    }

    public Optional<UUID> invokeContainerScanningWorkflow() {
        try {
            logger.debug("Determining if configuration is valid to run a container scan.");
            if (!isContainerScanEligible()) {
                logger.info("No {} property was provided. Skipping container scan.", DetectProperties.DETECT_CONTAINER_SCAN_FILE.getKey());
                return Optional.empty();
            }

            if (!isBlackDuckVersionValid()) {
                String minBlackDuckVersion = String.join(".",
                    Integer.toString(MIN_BLACK_DUCK_VERSION.getMajor()),
                    Integer.toString(MIN_BLACK_DUCK_VERSION.getMinor()),
                    Integer.toString(MIN_BLACK_DUCK_VERSION.getPatch())
                );
                throw new IntegrationException("Container scan is only supported with BlackDuck version " + minBlackDuckVersion + " or greater. Container scan could not be run.");
            }
            if (!isContainerImageResolved()) {
                throw new IOException("Container image file path not resolved or file could not be downloaded. Container scan could not be run.");
            }

            UUID scanId = performBlackduckInteractions();

            operationRunner.publishContainerSuccess();
            logger.info("Container scan image uploaded successfully.");
            return Optional.of(scanId);
        } catch (IntegrationTimeoutException e) {
            operationRunner.publishContainerTimeout(e);
            return Optional.empty();
        } catch (IntegrationException | IOException | OperationException e) {
            operationRunner.publishContainerFailure(e);
            return Optional.empty();
        }
    }

    protected abstract UUID performBlackduckInteractions() throws IOException, IntegrationException, OperationException;

    public String getCodeLocationName() {
        if (codeLocationName == null) {
            codeLocationName = createContainerScanCodeLocationName();
        }
        return codeLocationName;
    }

    private boolean isContainerImageResolved() {
        return containerImage != null && containerImage.exists();
    }

    private boolean isContainerScanEligible() {
        return operationRunner.getContainerScanFilePath().isPresent();
    }

    private boolean isBlackDuckVersionValid() {
        Optional<BlackDuckVersion> blackDuckVersion = blackDuckRunData.getBlackDuckServerVersion();
        return blackDuckVersion.isPresent() && blackDuckVersion.get().isAtLeast(MIN_BLACK_DUCK_VERSION);
    }

    private String createContainerScanCodeLocationName() {
        if (!isContainerImageResolved()) {
            return null;
        }

        CodeLocationNameManager codeLocationNameManager = operationRunner.getCodeLocationNameManager();
        return codeLocationNameManager.createContainerScanCodeLocationName(containerImage, projectNameVersion.getName(), projectNameVersion.getVersion());
    }
}
