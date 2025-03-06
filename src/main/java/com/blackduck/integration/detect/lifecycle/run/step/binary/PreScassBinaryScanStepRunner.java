package com.blackduck.integration.detect.lifecycle.run.step.binary;

import java.io.File;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import com.blackduck.integration.blackduck.version.BlackDuckVersion;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.sca.upload.rest.model.response.BinaryFinishResponseContent;
import com.blackduck.integration.sca.upload.rest.status.BinaryUploadStatus;
import com.blackduck.integration.util.NameVersion;

public class PreScassBinaryScanStepRunner extends AbstractBinaryScanStepRunner {
    
    private static final BlackDuckVersion MIN_MULTIPART_BINARY_VERSION = new BlackDuckVersion(2024, 7, 0);

    public PreScassBinaryScanStepRunner(OperationRunner operationRunner) {
        super(operationRunner);
    }
    
    public UUID extractBinaryScanId(BinaryUploadStatus status) {
        try {
            BinaryFinishResponseContent response = status.getResponseContent().get();

            String location = response.getLocation();
            URI uri = new URI(location);
            String path = uri.getPath();
            String scanId = path.substring(path.lastIndexOf('/') + 1);
            return UUID.fromString(scanId);
        } catch (Exception e) {
            logger.warn("Unexpected response uploading binary, will be unable to wait for scan completion.");
            return null;
        }
    }

    @Override
    protected UUID performBlackduckInteractions(NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData,
            Optional<File> binaryScanFile) throws OperationException, IntegrationException {
        if (isMultipartUploadPossible(blackDuckRunData)) {            
            BinaryUploadStatus status = operationRunner.uploadBinaryScanFile(binaryScanFile.get(), projectNameVersion,
                    blackDuckRunData);
            
            return extractBinaryScanId(status);
        } else {
            codeLocations =
                    Optional.of(operationRunner.uploadLegacyBinaryScanFile(binaryScanFile.get(), projectNameVersion, blackDuckRunData));
            
            return null;
        }
    }

    private boolean isMultipartUploadPossible(BlackDuckRunData blackDuckRunData) {
        Optional<BlackDuckVersion> blackDuckVersion = blackDuckRunData.getBlackDuckServerVersion();
        return blackDuckVersion.isPresent() && blackDuckVersion.get().isAtLeast(MIN_MULTIPART_BINARY_VERSION);
    }
}
