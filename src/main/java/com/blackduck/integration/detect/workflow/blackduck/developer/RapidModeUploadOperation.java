package com.blackduck.integration.detect.workflow.blackduck.developer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.codelocation.upload.UploadBatch;
import com.blackduck.integration.blackduck.codelocation.upload.UploadTarget;
import com.blackduck.integration.detect.workflow.bdio.BdioResult;
import com.blackduck.integration.detect.workflow.blackduck.developer.blackduck.DetectRapidScanService;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.HttpUrl;

public class RapidModeUploadOperation {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DetectRapidScanService rapidScanService;

    public RapidModeUploadOperation(DetectRapidScanService rapidScanService) {
        this.rapidScanService = rapidScanService;
    }

    public List<HttpUrl> run(BdioResult bdioResult, RapidScanOptions rapidScanOptions, @Nullable File rapidScanConfig)
        throws IntegrationException, IOException, InterruptedException {
        String scanModeString = rapidScanOptions.getScanMode().displayName();
        logger.info("Begin " + scanModeString + " Mode Scan");
        UploadBatch uploadBatch = new UploadBatch();
        for (UploadTarget uploadTarget : bdioResult.getUploadTargets()) {
            logger.debug(String.format("Uploading %s", uploadTarget.getUploadFile().getName()));
            uploadBatch.addUploadTarget(uploadTarget);
        }
        List<HttpUrl> results = rapidScanService.performUpload(uploadBatch, rapidScanOptions, rapidScanConfig);
        logger.debug("Rapid scan url count: {}", results.size());
        return results;
    }
}
