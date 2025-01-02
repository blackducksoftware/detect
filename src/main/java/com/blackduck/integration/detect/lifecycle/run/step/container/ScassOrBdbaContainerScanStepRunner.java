package com.blackduck.integration.detect.lifecycle.run.step.container;

import java.util.Optional;
import java.util.UUID;

import com.blackduck.integration.blackduck.version.BlackDuckVersion;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.ScanCreationResponse;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.operation.blackduck.ScassScanInitiationResult;
import com.blackduck.integration.detect.lifecycle.run.step.BdbaScanStepRunner;
import com.blackduck.integration.detect.lifecycle.run.step.ScassScanStepRunner;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;

public class ScassOrBdbaContainerScanStepRunner extends AbstractContainerScanStepRunner {
    private static final BlackDuckVersion MIN_SCASS_SCAN_VERSION = new BlackDuckVersion(2025, 1, 0);

    public ScassOrBdbaContainerScanStepRunner(OperationRunner operationRunner, NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData, Gson gson) throws IntegrationException, OperationException {
        super(operationRunner, projectNameVersion, blackDuckRunData, gson);
    }

    @Override
    protected UUID performBlackduckInteractions() throws IntegrationException, OperationException {
        ScassScanInitiationResult initResult = operationRunner.initiateScan(
            projectNameVersion,
            containerImage,
            operationRunner.getDirectoryManager().getContainerOutputDirectory(),
            blackDuckRunData,
            scanType,
            gson
        );

        ScanCreationResponse scanCreationResponse = initResult.getScanCreationResponse();
        String scanId = scanCreationResponse.getScanId();
        String uploadUrl = scanCreationResponse.getUploadUrl();

        if (uploadUrl != null && !uploadUrl.isEmpty()) {
            ScassScanStepRunner scassScanStepRunner = new ScassScanStepRunner(blackDuckRunData);
            scassScanStepRunner.runScassScan(Optional.of(initResult.getZipFile()), scanCreationResponse);
        } else {
            BdbaScanStepRunner bdbaScanStepRunner = new BdbaScanStepRunner(operationRunner);
            bdbaScanStepRunner.runBdbaScan(projectNameVersion, blackDuckRunData, Optional.of(containerImage), scanId, scanType);
        }
        return UUID.fromString(scanId);
    }

    public static boolean areScassScansPossible(Optional<BlackDuckVersion> blackDuckVersion) {
        return blackDuckVersion.isPresent() && blackDuckVersion.get().isAtLeast(MIN_SCASS_SCAN_VERSION);
    }
}
