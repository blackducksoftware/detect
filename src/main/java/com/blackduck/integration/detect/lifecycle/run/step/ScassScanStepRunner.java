package com.blackduck.integration.detect.lifecycle.run.step;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.http.BlackDuckRequestBuilder;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.blackduck.service.BlackDuckServicesFactory;
import com.blackduck.integration.blackduck.service.request.BlackDuckResponseRequest;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.ScanCreationResponse;
import com.blackduck.integration.detect.lifecycle.run.step.utility.MultipartUploaderHelper;
import com.blackduck.integration.detect.util.DetectZipUtil;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.sca.upload.client.uploaders.ScassUploader;
import com.blackduck.integration.sca.upload.client.uploaders.UploaderFactory;
import com.blackduck.integration.sca.upload.rest.status.DefaultUploadStatus;
import com.blackduck.integration.sca.upload.rest.status.UploadStatus;

public class ScassScanStepRunner {
    
    private final BlackDuckRunData blackDuckRunData;
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private static final String NOTIFY_URL = "/api/scans/{}/scass-scan-processing";

    public ScassScanStepRunner(BlackDuckRunData blackDuckRunData) {
        this.blackDuckRunData = blackDuckRunData;
    }
    
    public void runScassScan(Optional<File> scanFileOptional, ScanCreationResponse scanCreationResponse, File outputDirectory) throws IntegrationException {
        if (scanFileOptional == null || !scanFileOptional.isPresent()) {
            throw new IntegrationException("File to scan is not provided.");
        }
        if (scanCreationResponse == null) {
            throw new IntegrationException("Scan creation response is not provided.");
        }
        
        File scanFile = scanFileOptional.get();

        ScassUploader scaasScanUploader = createScaasScanUploader();

        // Don't upload a file larger than SCASS's capability indicates
        validateGcpSize(scanFileOptional, scanCreationResponse);
        
        File preparedFile = prepareFileForUpload(scanFile, outputDirectory);

        UploadStatus status;
        try {
            status = scaasScanUploader.upload(
                    scanCreationResponse.getUploadUrlData() != null ? HttpMethod.fromMethod(scanCreationResponse.getUploadUrlData().getMethod()) : HttpMethod.POST,
                    scanCreationResponse.getUploadUrl(),
                    scanCreationResponse.getAllHeaders(),
                    preparedFile.toPath());
        } catch (IOException e) {
            throw new IntegrationException(e);
        }

        if (status.isError()) {
            MultipartUploaderHelper.handleUploadError((DefaultUploadStatus) status);
        }

        // call /scans/{scanId}/scass-scan-processing to notify BlackDuck the file is uploaded
        notifyUploadComplete(scanCreationResponse.getScanId());
    }

    private File prepareFileForUpload(File scanFile, File outputDirectory) throws IntegrationException {
        // If we can open this without exception, it's a zip file already, don't zip it again.
        try (ZipFile zipFile = new ZipFile(scanFile)) {
            return scanFile;
        } catch (IOException e) {
            return zipFile(scanFile, outputDirectory);
        }
    }

    // TODO try to do md5 alongside this
    private File zipFile(File scanFile, File outputDirectory) throws IntegrationException {
        String zipName = "scass-upload.zip";
        File zipFile = new File(outputDirectory, zipName);
        
        Map<String, Path> entries = new HashMap<>();
        entries.put(scanFile.getName(), scanFile.toPath());

        try {
            DetectZipUtil.zip(zipFile, entries);
        } catch (IOException e) {
            throw new IntegrationException("Unable to create scan archive for upload.");
        }
        
        return zipFile;
    }

    private void validateGcpSize(Optional<File> scanFile, ScanCreationResponse scanCreationResponse)
            throws IntegrationException {
        String gcpSizeHeader = scanCreationResponse.getAllHeaders().get("x-goog-content-length-range");
        if (gcpSizeHeader != null) {
            Long gcpSize = Long.valueOf(gcpSizeHeader.split(",")[1]);

            File file = scanFile.get();
            if (file.length() > gcpSize) {
                throw new IntegrationException(String.format("Unable to upload file as it exceeds GCP's size of %s bytes", gcpSize));
            }
        }
    }
    
    private ScassUploader createScaasScanUploader() throws IntegrationException {
        UploaderFactory uploadFactory = MultipartUploaderHelper.getUploaderFactory(blackDuckRunData);
        
        return uploadFactory.createScassUploader();
    }

    private Response notifyUploadComplete(String scanId) throws IntegrationException {
        BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory();
        BlackDuckApiClient blackDuckApiClient = blackDuckServicesFactory.getBlackDuckApiClient();
        
        String notfyUrl = NOTIFY_URL.replace("{}", scanId);

        HttpUrl postUrl = blackDuckRunData.getBlackDuckServerConfig().getBlackDuckUrl().appendRelativeUrl(notfyUrl);

        BlackDuckResponseRequest buildBlackDuckResponseRequest = new BlackDuckRequestBuilder()
            .addHeader("Content-Type", "application/vnd.blackducksoftware.scan-6+json")
            .post()
            .buildBlackDuckResponseRequest(postUrl);

        try (Response response = blackDuckApiClient.execute(buildBlackDuckResponseRequest)) {
            return response;
        } catch (IntegrationException e) {
            logger.trace("Could not execute JSON upload request to storage service.");
            throw new IntegrationException("Could not execute SCASS notification request.", e);
        } catch (IOException e) {
            logger.trace("I/O error occurred during SCASS notification request.");
            throw new IntegrationException("I/O error occurred during SCASS notification request.", e);
        }
    }
}
