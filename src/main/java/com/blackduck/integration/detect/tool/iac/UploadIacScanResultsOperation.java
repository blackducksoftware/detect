package com.blackduck.integration.detect.tool.iac;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

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

    public void uploadResults(File resultsFile, String scanId) throws IntegrationException, IOException {
        String resultsFileContentAsString, normalizedNFDFileContent;
        try {
            printSomeJavaAndSystemProperties();
            // Manually feed in "problematic" results file generated on other machines
//            File pablosFile = new File("/Users/shanty/Desktop/vs-compare/pablos.json");
//            File seansFile = new File("/Users/shanty/Desktop/vs-compare/sigma-results-sean.json");
//            resultsFile = pablosFile;

            // IAC folder in scan output
            String iacDir = resultsFile.getParent();

            // dump raw bytes before reading JSON file
            byte[] rawResultsFileBytes = Files.readAllBytes(resultsFile.toPath());
            try (FileOutputStream fos = new FileOutputStream(iacDir + "/rawResultsFileBytes.bin")) {
                fos.write(rawResultsFileBytes);
                logger.debug("Saved raw bytes of results.json from IAC scan to: " + iacDir + "/rawResultsFileBytes.bin");
            }

            // dump raw bytes after reading file into a string
            // the following debug statements will already confirm that at the byte level the contents are not the same.
            // 1. write to a file the exact bytes before normalization
            // add a file labeled after-normalization. this represents string content (at byte level) before transmission
            // 2. write to file the exact bytes after normalization (in my case they should be identical)
            resultsFileContentAsString = readFileToStringUTF8(resultsFile);

            // dump raw bytes of string in memory after reading it as UTF-8
            byte[] stringBytes = resultsFileContentAsString.getBytes(StandardCharsets.UTF_8);
            try (FileOutputStream fos = new FileOutputStream(iacDir + "/utf8StringBytesBEFORENormalization.bin")) {
                fos.write(stringBytes);
            }

            logger.debug("Checking results file content as a string (BEFORE normalization)...");
            checkPrecomposedOrDecomposed(resultsFileContentAsString);

            normalizedNFDFileContent = normalizeFileContent(resultsFileContentAsString);

            logger.debug("Checking results file content as a string (AFTER normalization)...");
            checkPrecomposedOrDecomposed(normalizedNFDFileContent);

            // dump raw bytes of string in memory after reading it as UTF-8
            byte[] stringBytesAFTERNormalization = normalizedNFDFileContent.getBytes(StandardCharsets.UTF_8);
            try (FileOutputStream fos = new FileOutputStream(iacDir + "/utf8StringBytesAFTERNormalization.bin")) {
                fos.write(stringBytesAFTERNormalization);
            }
        } catch (IOException e) {
            throw new IntegrationException("Unable to parse Iac Scan results file: " + resultsFile.getAbsolutePath(), e);
        }
        logger.debug("Uploading IAC Scan results ...");
        Response response = iacScanUploadService.uploadIacScanResults(normalizedNFDFileContent, scanId);
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

    private void  checkPrecomposedOrDecomposed(String resultsFileContentAsString) {
        boolean isNFC = Normalizer.isNormalized(resultsFileContentAsString, Normalizer.Form.NFC); // Canonical decomposition, followed by canonical composition.U+00F6
        boolean isNFD = Normalizer.isNormalized(resultsFileContentAsString, Normalizer.Form.NFD); // Canonical decomposition. U+006F

        if (isNFC) { logger.debug("Content is precomposed."); }
        else if (isNFD) { logger.debug("Content is decomposed."); }
    }

    private String normalizeFileContent(String originalResultsFileContentAsString) {
        logger.debug("Normalizing string to NFD...");
        return Normalizer.normalize(originalResultsFileContentAsString, Normalizer.Form.NFD); // server seems to accept this and reject NFC
    }

    private void printSomeJavaAndSystemProperties() {
        // some relavnt java properties
        logger.debug("file.encoding= " + System.getProperty("file.encoding"));

        logger.debug("java.version= " + System.getProperty("java.version"));

        logger.debug("sun.jnu.encoding= " + System.getProperty("sun.jnu.encoding"));

        logger.debug("java.runtime.version= " + System.getProperty("java.runtime.version"));

        logger.debug("java.nio.charset.Charset.defaultCharset= " + java.nio.charset.Charset.defaultCharset());

        // some relevant env variables
        Map<String, String> env = System.getenv();
        logger.debug("LANG= " + env.get("LANG"));
        logger.debug("LC_ALL= " + env.get("LC_ALL"));
        logger.debug("LC_CTYPE= " + env.get("LC_CTYPE"));

        logger.debug("user.language= " + System.getProperty("user.language"));
        logger.debug("user.country= " + System.getProperty("user.country"));
        logger.debug("user.variant= " + System.getProperty("user.variant"));

        // locale info
        Locale systemLocale = Locale.getDefault();
        logger.debug("System Locale: " + systemLocale);
        logger.debug("Language: " + systemLocale.getLanguage());
        logger.debug("Country: " + systemLocale.getCountry());
    }
}
