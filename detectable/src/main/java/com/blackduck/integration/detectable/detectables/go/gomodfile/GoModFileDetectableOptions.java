package com.blackduck.integration.detectable.detectables.go.gomodfile;

public class GoModFileDetectableOptions {
    private final String goProxyUrl;
    
    public GoModFileDetectableOptions(String goProxyUrl) {
        this.goProxyUrl = goProxyUrl;
    }

    public String getGoProxyUrl() {
        return goProxyUrl;
    }
}
