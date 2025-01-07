package com.blackduck.integration.detect.lifecycle.run.step.binary;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

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

public class ScassOrBdbaBinaryScanStepRunner extends AbstractBinaryScanStepRunner {
    
    private static final BlackDuckVersion MIN_SCASS_SCAN_VERSION = new BlackDuckVersion(2025, 1, 0);

    public ScassOrBdbaBinaryScanStepRunner(OperationRunner operationRunner) {
        super(operationRunner);
    }

    public static boolean areScassScansPossible(Optional<BlackDuckVersion> blackDuckVersion) {
        return blackDuckVersion.isPresent() && blackDuckVersion.get().isAtLeast(MIN_SCASS_SCAN_VERSION);
    }

    @Override
    protected UUID performBlackduckInteractions(NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData,
            Optional<File> binaryScanFile) throws OperationException, IntegrationException {
        // call BlackDuck to create a scanID and determine where to upload the file
        ScassScanInitiationResult initResult = operationRunner.initiateScan(
            projectNameVersion, binaryScanFile.get(),
            operationRunner.getDirectoryManager().getBinaryOutputDirectory(),
            blackDuckRunData, "BINARY", gson
        );

        ScanCreationResponse scanCreationResponse = initResult.getScanCreationResponse();

        String scanId = scanCreationResponse.getScanId();
        String uploadUrl = scanCreationResponse.getUploadUrl();
        
        if (StringUtils.isNotEmpty(uploadUrl)) {
            // This is a SCASS capable server server and SCASS is enabled.
            ScassScanStepRunner scassScanStepRunner = new ScassScanStepRunner(blackDuckRunData);
            
            scassScanStepRunner.runScassScan(Optional.of(initResult.getZipFile()), scanCreationResponse);       
        } else {
            // This is a SCASS capable server server but SCASS is not enabled.
            BdbaScanStepRunner bdbaScanStepRunner = new BdbaScanStepRunner(operationRunner);
            
            bdbaScanStepRunner.runBdbaScan(projectNameVersion, blackDuckRunData, binaryScanFile, scanId, "BINARY");
        }
        
        return UUID.fromString(scanId);
    }
}
