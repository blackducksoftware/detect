package com.blackduck.integration.detect.lifecycle.run.step.utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.ScanCreationResponse.UploadUrlData;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.log.Slf4jIntLogger;
import com.blackduck.integration.sca.upload.client.UploaderConfig;
import com.blackduck.integration.sca.upload.client.uploaders.UploaderFactory;
import com.blackduck.integration.sca.upload.rest.status.UploadStatus;
import com.google.gson.Gson;

public class UploaderHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(UploaderHelper.class);

    public static UploaderFactory getUploaderFactory(BlackDuckRunData blackDuckRunData) throws IntegrationException {
        UploaderConfig.Builder uploaderConfigBuilder =  UploaderConfig.createConfigFromEnvironment(blackDuckRunData.getBlackDuckServerConfig().getProxyInfo())
                .setBlackDuckTimeoutInSeconds(blackDuckRunData.getBlackDuckServerConfig().getTimeout())
                .setMultipartUploadTimeoutInMinutes(blackDuckRunData.getBlackDuckServerConfig().getTimeout() /  60)
                .setAlwaysTrustServerCertificate(blackDuckRunData.getBlackDuckServerConfig().isAlwaysTrustServerCertificate())
                .setBlackDuckUrl(blackDuckRunData.getBlackDuckServerConfig().getBlackDuckUrl())
                .setApiToken(blackDuckRunData.getBlackDuckServerConfig().getApiToken().get());
            
            UploaderConfig uploaderConfig = uploaderConfigBuilder.build();
            return new UploaderFactory(uploaderConfig, new Slf4jIntLogger(logger), new Gson());
    }
    
    public static void handleUploadError(UploadStatus status) throws IntegrationException {
        if (status == null) {
            throw new IntegrationException("Unexpected empty response attempting to upload file.");
        } else if (status.getException().isPresent()) {
            throw status.getException().get();      
        } else {
            throw new IntegrationException(String.format("Unable to upload multipart file. Status code: {}. {}", status.getStatusCode(), status.getStatusMessage()));
        }  
    }
    
    public static Map<String, String> getAllHeaders(UploadUrlData uploadUrlData) {
        Map<String, String> allHeaders = new HashMap<>();
        List<Map<String, String>> headers = uploadUrlData.getHeaders();
        
        for (Map<String, String> singleHeader : headers) {
            allHeaders.put(singleHeader.get("name"), singleHeader.get("value"));
        }
        
        return allHeaders;
    }
}
