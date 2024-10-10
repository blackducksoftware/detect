package com.synopsys.integration.detect.workflow;

import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactoryConstantsHelper {
    private static final Logger logger = LoggerFactory.getLogger(ArtifactoryConstantsHelper.class);
    private static Boolean isBlackDuckUrlAccessible;
    
    public static String getArtifactoryUrl() {
        if (isBlackDuckUrlAccessible == null) {
            isBlackDuckUrlAccessible = isBlackDuckUrlAccessible(ArtifactoryConstants.ARTIFACTORY_URL);
        }
        return isBlackDuckUrlAccessible ? ArtifactoryConstants.ARTIFACTORY_URL : ArtifactoryConstants.ARTIFACTORY_FALLBACK_URL;
    }
    
    public static boolean isBlackDuckUrlAccessible(String targetUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(targetUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            if (200 <= responseCode && responseCode <= 399) {
            	return true;
            } else {
            	logger.warn(String.format("{} responded with unanticipated code {}."), targetUrl, responseCode);
            	return false;
            }
        } catch (Exception e) {
        	logger.warn(String.format("{} is inaccessible from this machine. Please allow access through your firewall."), targetUrl);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
