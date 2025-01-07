package com.blackduck.integration.detect.lifecycle.run.step.binary;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.codelocation.CodeLocationCreationData;
import com.blackduck.integration.blackduck.codelocation.binaryscanner.BinaryScanBatchOutput;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.DockerTargetData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.tool.binaryscanner.BinaryScanOptions;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;

public abstract class AbstractBinaryScanStepRunner {
    protected final OperationRunner operationRunner;
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected Gson gson;
    protected Optional<CodeLocationCreationData<BinaryScanBatchOutput>> codeLocations;

    public AbstractBinaryScanStepRunner(OperationRunner operationRunner) {
        this.operationRunner = operationRunner;
        this.gson = new Gson();
        codeLocations = null;
    }
    
    protected abstract UUID performBlackduckInteractions(NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData, Optional<File> binaryScanFile) throws OperationException, IntegrationException;
    
    public Optional<UUID> invokeBinaryScanningWorkflow(
        DockerTargetData dockerTargetData,
        NameVersion projectNameVersion,
        BlackDuckRunData blackDuckRunData,
        Set<String> binaryTargets        
    ) throws OperationException, IntegrationException {
        Optional<File> binaryScanFile = determineBinaryScanFileTarget(dockerTargetData, binaryTargets);
        if (binaryScanFile.isPresent()) { 
            UUID scanId = performBlackduckInteractions(projectNameVersion, blackDuckRunData, binaryScanFile);
            
            return scanId != null ? Optional.of(scanId) : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    public Optional<File> determineBinaryScanFileTarget(DockerTargetData dockerTargetData, Set<String> binaryTargets) throws OperationException {
        BinaryScanOptions binaryScanOptions = operationRunner.calculateBinaryScanOptions();
        File binaryUpload = null;
        if (binaryScanOptions.getSingleTargetFilePath().isPresent()) {
            logger.info("Binary upload will upload single file.");
            binaryUpload = binaryScanOptions.getSingleTargetFilePath().get().toFile();
            operationRunner.updateBinaryUserTargets(binaryUpload);
        } else if (binaryScanOptions.getFileFilter().isPresent()) {
            Optional<File> multipleUploadTarget = operationRunner.searchForBinaryTargets(
                binaryScanOptions.getFileFilter().get(),
                binaryScanOptions.getSearchDepth(),
                binaryScanOptions.isFollowSymLinks()
            );
            if (multipleUploadTarget.isPresent()) {
                binaryUpload = multipleUploadTarget.get();
                List<File> multiTargets = operationRunner.getMultiBinaryTargets();
                multiTargets.forEach(operationRunner::updateBinaryUserTargets);
            } else {
                operationRunner.publishBinaryFailure("Binary scanner did not find any files matching any pattern.");
            }
        } else if (dockerTargetData != null && dockerTargetData.getContainerFilesystem().isPresent()) {
            logger.info("Binary Scanner will upload docker container file system.");
            binaryUpload = dockerTargetData.getContainerFilesystem()
                .get();// Very important not to binary scan the same Docker output that we sig scanned (=codelocation name collision)
        }

        if (binaryTargets != null && !binaryTargets.isEmpty()) {
            binaryUpload = operationRunner.collectBinaryTargets(binaryTargets).get();
        }

        if (binaryUpload == null) {
            logger.info("Binary scanner found nothing to upload.");
            return Optional.empty();
        } else if (binaryUpload.isFile() && binaryUpload.canRead()) {
            return Optional.of(binaryUpload);
        } else {
            operationRunner.publishBinaryFailure("Binary scan file did not exist, is not a file or can't be read.");
            return Optional.empty();
        }
    }
    
    public Optional<CodeLocationCreationData<BinaryScanBatchOutput>> getCodeLocations() {
        if (codeLocations == null) {
            return Optional.empty();
        }
        return codeLocations;
    }
}
