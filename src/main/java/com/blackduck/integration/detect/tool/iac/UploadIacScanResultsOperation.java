package com.blackduck.integration.detect.tool.iac;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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
        String resultsFileContent, normalizedNFDFileContent;
        try {
            printJavaSystemEncodingProperty();
//            File pablosFile = new File("/Users/shanty/Desktop/vs-compare/pablos.json");
//            File seansFile = new File("/Users/shanty/Desktop/vs-compare/sigma-results-sean.json");

//            resultsFile = pablosFile;

            resultsFileContent = readFileToStringUTF8(resultsFile);
            checkPrecomposedOrDecomposed(resultsFileContent);

            normalizedNFDFileContent = normalizeFileContent(resultsFileContent);
            checkPrecomposedOrDecomposed(normalizedNFDFileContent);
        } catch (IOException e) {
            throw new IntegrationException("Unable to parse Iac Scan results file: " + resultsFile.getAbsolutePath(), e);
        }
        Response response = iacScanUploadService.uploadIacScanResults(resultsFileContent, scanId);
        if (response.isStatusCodeSuccess()) {
            logger.info("Successfully uploaded Iac Scan results.");
        } else {
            throw new IntegrationException(String.format("Iac Scan upload failed with code %d: %s", response.getStatusCode(), response.getStatusMessage()));
        }
    }

    private String readFileToStringUTF8(File resultsFile) throws IOException {
        logger.debug("Reading {} using character encoding {}", resultsFile.getAbsolutePath(), StandardCharsets.UTF_8);
        return FileUtils.readFileToString(resultsFile, StandardCharsets.UTF_8);
    }

    private String readFileToStringWin1252(File resultsFile) throws IOException {
        logger.debug("Reading {} using character encoding {}", resultsFile.getAbsolutePath(), Charset.forName("windows-1252"));
        return FileUtils.readFileToString(resultsFile, Charset.forName("windows-1252"));
    }

    private void printJavaSystemEncodingProperty() {
        String encoding = System.getProperty("file.encoding");
        logger.debug("System default file encoding: " + encoding);

    }

    private void  checkPrecomposedOrDecomposed(String resultsFileContentAsString) {
        boolean isNFC = Normalizer.isNormalized(resultsFileContentAsString, Normalizer.Form.NFC); // Canonical decomposition, followed by canonical composition.U+00F6
        boolean isNFD = Normalizer.isNormalized(resultsFileContentAsString, Normalizer.Form.NFD); // Canonical decomposition. U+006F

        if (isNFC) { logger.debug("Results File content is precomposed."); }
        else if (isNFD) { logger.debug("Results file content is decomposed."); }
    }

    private String normalizeFileContent(String originalResultsFileContentAsString) {
        logger.debug("Normalizing to NFD before sending to hub...");
        return Normalizer.normalize(originalResultsFileContentAsString, Normalizer.Form.NFD); // server seems to accept this and reject NFC
    }
}
