package com.blackduck.integration.detectable.detectables.go.gomodfile;

public class GoModFileDetectableOptions {
    private final String goProxyUrl;
    private final int connectionTimeout;
    private final int readTimeout;

    public GoModFileDetectableOptions(String goProxyUrl, int connectionTimeout, int readTimeout) {
        this.goProxyUrl = goProxyUrl;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    public String getGoProxyUrl() {
        return goProxyUrl;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }
}
