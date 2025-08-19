package com.blackduck.integration.detect.lifecycle.run.step.packagemanager;

import java.io.File;
import java.util.List;
import java.util.Optional;

import com.blackduck.integration.blackduck.bdio2.model.BdioFileContent;
import com.blackduck.integration.blackduck.exception.BlackDuckIntegrationException;
import com.blackduck.integration.blackduck.version.BlackDuckVersion;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.CommonScanResult;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.step.CommonScanStepRunner;
import com.blackduck.integration.detect.workflow.bdio.BdioResult;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.blackduck.integration.blackduck.bdio2.util.Bdio2ContentExtractor;

public class PackageManagerStepRunner {
    
    private final CommonScanStepRunner commonScanStepRunner;
    private static final String FILE_NAME_BDIO_HEADER_JSONLD = "bdio-header.jsonld";
    private final OperationRunner operationRunner;
    private final Gson gson;
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final BlackDuckVersion MIN_SCASS_SCAN_VERSION = new BlackDuckVersion(2025, 7, 0);

    public PackageManagerStepRunner(OperationRunner operationRunner) {
        this.operationRunner = operationRunner;
        commonScanStepRunner = new CommonScanStepRunner();
        this.gson = new Gson();
    }

    public static boolean areScassScansPossible(Optional<BlackDuckVersion> blackDuckVersion) {
        return blackDuckVersion.isPresent() && blackDuckVersion.get().isAtLeast(MIN_SCASS_SCAN_VERSION);
    }

    public CommonScanResult invokePackageManagerScanningWorkflow(NameVersion projectNameVersion, BlackDuckRunData blackDuckRunData, BdioResult bdioResult) {
        File uploadFile = bdioResult.getUploadTargets().get(0).getUploadFile();

        BdioFileContent jsonldHeader = extractBdioFileContent(uploadFile);
        CommonScanResult scanResult;
        try {
            scanResult = commonScanStepRunner.performCommonScan(
                    projectNameVersion, 
                    blackDuckRunData, 
                    Optional.of(uploadFile),
                    operationRunner, 
                    gson,
                    CommonScanStepRunner.PACKAGE_MANAGER,
                    bdioResult.getUploadTargets().get(0).getCodeLocationName(),
                    jsonldHeader);

        } catch (OperationException | IntegrationException e) {
            logger.error("Error completing the package manager scan. {}", e.getMessage());
            return null;
        }

        logger.info("Successfully completed package manager scan of file: {}", uploadFile.getAbsolutePath());
        return scanResult;
    }

    private BdioFileContent extractBdioFileContent(File uploadFile) {
        Bdio2ContentExtractor extractor = new Bdio2ContentExtractor();
        try {
            List<BdioFileContent> bdioFiles = extractor.extractContent(uploadFile);
            return bdioFiles.stream()
                    .filter(content -> content.getFileName().equals(FILE_NAME_BDIO_HEADER_JSONLD))
                    .findFirst()
                    .orElseThrow(() -> new BlackDuckIntegrationException("Cannot find BDIO header file " + FILE_NAME_BDIO_HEADER_JSONLD + "."));
        } catch (IntegrationException | IllegalArgumentException e) {
            operationRunner.publishDetectorFailure();
            throw new RuntimeException("Error extracting BDIO header file from bdio" + uploadFile.getAbsolutePath(), e);
        }
    }

}
