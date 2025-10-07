package com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.detectable.detectables.go.gomodfile.GoModFileDetectableOptions;

public class GoProxyModuleResolver {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public final GoModFileDetectableOptions options;

    public GoProxyModuleResolver(GoModFileDetectableOptions options) {
        this.options = options;
        // initialize the http client with a default connection timeout of 30s and configure it to follow redirects
        logger.debug("Initializing Go proxy HTTP client with connection timeout of {} seconds and read timeout of {} seconds", options.getConnectionTimeout(), options.getReadTimeout());
    }

    private String getGoModFileURL(Dependency dependency) {
        // Construct the URL to fetch the go.mod file for the given dependency
        String modulePath = dependency.getName();
        // Remove quotes around modulePath if present
        if (modulePath.startsWith("\"") && modulePath.endsWith("\"")) {
            modulePath = modulePath.substring(1, modulePath.length() - 1);
        }
        String version = dependency.getVersion();
        return String.format("%s/%s/@v/%s.mod", options.getGoProxyUrl(), modulePath, version);
        // Example: https://proxy.golang.org/github.com/fsnotify/fsnotify/@v/v1.8.0.mod
    }

    public Boolean checkConnectivity() {
        String testURL = String.format("%s/", options.getGoProxyUrl());
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(testURL).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(options.getConnectionTimeout() * 1000);
            conn.setReadTimeout(options.getReadTimeout() * 1000);
            int responseCode = conn.getResponseCode();
            logger.debug("Go proxy connectivity check to URL '{}' returned status code: {}", options.getGoProxyUrl(), responseCode);
            return responseCode == 200;
        } catch (ConnectException e) {
            logger.error("The Go proxy URL '{}' could not be resolved. Please check the URL and your network connection.", options.getGoProxyUrl());
            return false;
        } catch (Exception e) {
            logger.error("Error checking connectivity to Go proxy: {}", e.getMessage());
            return false;
        }
    }

    public String getGoModFileOfTheDependency(Dependency dependency) {
        String modFileURL = getGoModFileURL(dependency);
        // Make a HTTP GET request to fetch the go.mod file content
        try {
            logger.debug("Fetching go.mod for dependency {} via URL {}: ", dependency, modFileURL);
            HttpURLConnection conn = (HttpURLConnection) new URL(modFileURL).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(options.getConnectionTimeout() * 1000);
            conn.setReadTimeout(options.getReadTimeout() * 1000);
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine).append(System.lineSeparator());
                    }
                    logger.debug("Successfully fetched go.mod file for dependency {}", dependency);
                    return content.toString();
                }
            } else {
                String responseMessage = conn.getResponseMessage();
                logger.error("Failed to fetch go.mod file for dependency {}. HTTP request status code: {}. Response text: {}", dependency, conn.getResponseCode(), responseMessage);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error fetching go.mod file for dependency {}. Error: {}", dependency, e.getMessage());
        }
        return null;
    }


}
