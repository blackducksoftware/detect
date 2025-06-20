package com.blackduck.integration.detect.lifecycle.run.step.container;

import java.util.Optional;
import java.util.UUID;

import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.CommonScanResult;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.step.CommonScanStepRunner;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;

public class ScassOrBdbaContainerScanStepRunner extends AbstractContainerScanStepRunner {

    private String codeLocationName;
    private CommonScanStepRunner commonScanStepRunner;
    
    public ScassOrBdbaContainerScanStepRunner(OperationRunner operationRunner, NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData, Gson gson) throws IntegrationException, OperationException {
        super(operationRunner, projectNameVersion, blackDuckRunData, gson);
        commonScanStepRunner = new CommonScanStepRunner();
    }

    @Override
    protected UUID performBlackduckInteractions() throws IntegrationException, OperationException {        
        CommonScanResult scanResult = commonScanStepRunner.performCommonScan(
                projectNameVersion, 
                blackDuckRunData, 
                Optional.of(containerImage), 
                operationRunner, 
                gson, 
                SCAN_TYPE,
                "",
                null);
        
        codeLocationName = scanResult.getCodeLocationName();
        
        return scanResult.getScanId();
    }

    @Override
    public String getCodeLocationName() {
        return codeLocationName;
    }
}
