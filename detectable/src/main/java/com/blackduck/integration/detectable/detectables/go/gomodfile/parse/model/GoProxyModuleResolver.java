package com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model;

import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.detectable.detectables.go.gomodfile.GoModFileDetectableOptions;

public class GoProxyModuleResolver {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public final GoModFileDetectableOptions options;
    private final HttpClient client;

    public GoProxyModuleResolver(GoModFileDetectableOptions options) {
        this.options = options;
        // initialize the http client with a default connection timeout of 30s and configure it to follow redirects
        logger.debug("Initializing Go proxy HTTP client with connection timeout of {} seconds and read timeout of {} seconds", options.getConnectionTimeout(), options.getReadTimeout());
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(options.getConnectionTimeout()))
            .followRedirects(Redirect.ALWAYS)
            .build();
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
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(testURL))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(options.getReadTimeout()))
                    .build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            logger.debug("Go proxy connectivity check to URL '{}' returned status code: {}", options.getGoProxyUrl(), response.statusCode());
            return response.statusCode() == 200;
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
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(modFileURL))
                    .GET()
                    .timeout(Duration.ofSeconds(options.getReadTimeout()))
                    .build();
            HttpResponse<String> response  = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                logger.debug("Successfully fetched go.mod file for dependency {}", dependency);
                return response.body();
            } else {
                String responseBody = response.body();
                logger.error("Failed to fetch go.mod file for dependency {}. HTTP request status code: {}. Response text: {}", dependency, response.statusCode(), responseBody);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error fetching go.mod file for dependency {}. Error: {}", dependency, e.getMessage());
        }
        return null;
    }


}
