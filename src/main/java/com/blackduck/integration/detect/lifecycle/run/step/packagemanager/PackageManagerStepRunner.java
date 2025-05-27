package com.blackduck.integration.detect.lifecycle.run.step.packagemanager;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.CommonScanResult;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.step.CommonScanStepRunner;
import com.blackduck.integration.detect.workflow.bdio.BdioResult;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;

public class PackageManagerStepRunner {
    
    private CommonScanStepRunner commonScanStepRunner;
    private OperationRunner operationRunner;
    private Gson gson;

    public PackageManagerStepRunner(OperationRunner operationRunner) {
        this.operationRunner = operationRunner;
        commonScanStepRunner = new CommonScanStepRunner();
        this.gson = new Gson();
    }

    public Optional<UUID> invokeContainerScanningWorkflow(NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData, BdioResult bdioResult) {
        // TODO might have more than one bdio? Is this actually the bdio file?
        File uploadFile = bdioResult.getUploadTargets().get(0).getUploadFile();
        
        CommonScanResult scanResult = null;
        try {
            scanResult = commonScanStepRunner.performCommonScan(
                    projectNameVersion, 
                    blackDuckRunData, 
                    Optional.of(uploadFile),
                    operationRunner, 
                    gson, 
                    CommonScanStepRunner.BDIO,
                    bdioResult.getUploadTargets().get(0).getCodeLocationName());
        } catch (OperationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IntegrationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //logger.info("Successfully completed binary scan of file: " + binaryScanFile.get().getAbsolutePath());
        //operationRunner.publishBinarySuccess();
        return Optional.of(scanResult.getScanId());
    }

}
