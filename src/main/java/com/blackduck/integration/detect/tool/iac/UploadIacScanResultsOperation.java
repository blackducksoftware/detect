package com.blackduck.integration.detect.tool.iac;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.service.dataservice.IacScanUploadService;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.response.Response;

public class UploadIacScanResultsOperation {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private final IacScanUploadService iacScanUploadService;

    public UploadIacScanResultsOperation(IacScanUploadService iacScanUploadService) {this.iacScanUploadService = iacScanUploadService;}

    public void uploadResults(File resultsFile, String scanId) throws IntegrationException {
        String resultsFileContent, normalizedResultsFileContent;
        try {
            logger.trace("Reading {} using character encoding {}", resultsFile.getAbsolutePath(), StandardCharsets.UTF_8);
            resultsFileContent = FileUtils.readFileToString(resultsFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IntegrationException("Unable to parse Iac Scan results file: " + resultsFile.getAbsolutePath(), e);
        }
        normalizedResultsFileContent = normalizeFileContentToNFD(resultsFileContent);
        Response response = iacScanUploadService.uploadIacScanResults(normalizedResultsFileContent, scanId);
        if (response.isStatusCodeSuccess()) {
            logger.info("Successfully uploaded Iac Scan results.");
        } else {
            throw new IntegrationException(String.format("Iac Scan upload failed with code %d: %s", response.getStatusCode(), response.getStatusMessage()));
        }
    }


    private String normalizeFileContentToNFD(String originalResultsFileContentAsString) {
        return Normalizer.normalize(originalResultsFileContentAsString, Normalizer.Form.NFD);
    }
}
