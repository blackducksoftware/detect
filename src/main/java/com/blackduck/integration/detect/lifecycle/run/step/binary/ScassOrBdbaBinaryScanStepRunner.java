package com.blackduck.integration.detect.lifecycle.run.step.binary;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.step.CommonScanStepRunner;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;

public class ScassOrBdbaBinaryScanStepRunner extends AbstractBinaryScanStepRunner {
    
    CommonScanStepRunner commonScanStepRunner;

    public ScassOrBdbaBinaryScanStepRunner(OperationRunner operationRunner) {
        super(operationRunner);
        commonScanStepRunner = new CommonScanStepRunner();
    }

    @Override
    public UUID performBlackduckInteractions(NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData,
            Optional<File> binaryScanFile) throws OperationException, IntegrationException {
        try {
            UUID scanId = commonScanStepRunner.performCommonScan(projectNameVersion, blackDuckRunData, binaryScanFile,
                    operationRunner, gson, CommonScanStepRunner.BINARY);

            logger.info("Successfully completed binary scan of file: " + binaryScanFile.get().getAbsolutePath());
            operationRunner.publishBinarySuccess();
            return scanId;
        } catch (IntegrationException | OperationException e) {
            operationRunner.publishBinaryFailure(String.format("Failed to complete binary scan. %s", e.getMessage()));
            return null;
        }
    }
}
