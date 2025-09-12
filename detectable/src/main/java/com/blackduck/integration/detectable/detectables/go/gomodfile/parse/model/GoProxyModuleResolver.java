package com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.model.dependency.Dependency;

public class GoProxyModuleResolver {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private final String GOPROXY_URL = "https://proxy.golang.org";

    public GoProxyModuleResolver() {
        // Default constructor
    }

    private String getGoModFileURL(Dependency dependency) {
        // Construct the URL to fetch the go.mod file for the given dependency
        String modulePath = dependency.getName();
        // Remove quotes around modulePath if present
        if (modulePath.startsWith("\"") && modulePath.endsWith("\"")) {
            modulePath = modulePath.substring(1, modulePath.length() - 1);
        }
        String version = dependency.getVersion();
        return String.format("%s/%s/@v/%s.mod", GOPROXY_URL, modulePath, version);
    }

    public String getGoModFileOfTheDependency(Dependency dependency) {
        String modFileURL = getGoModFileURL(dependency);
        HttpClient client = HttpClient.newHttpClient();
        // Make a HTTP GET request to fetch the go.mod file content
        try {
            logger.debug("Fetching go.mod for dependency {} via URL {}: ", dependency.toString(), modFileURL);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(modFileURL))
                    .GET()
                    .build();
            HttpResponse<String> response  = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                logger.debug("Successfully fetched go.mod file for dependency {}", dependency.toString());
                return response.body();
            } else {
                logger.error("Failed to fetch go.mod file for dependency {}. HTTP request status code: {}. Response text: {}", dependency.toString(), response.statusCode(), response.body());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error fetching go.mod file for dependency {}. Error: {}", dependency.toString(), e.getMessage());
        }
        return null;
    }


}
