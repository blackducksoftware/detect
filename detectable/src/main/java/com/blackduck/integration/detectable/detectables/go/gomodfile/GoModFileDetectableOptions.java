package com.blackduck.integration.detectable.detectables.go.gomodfile;

public class GoModFileDetectableOptions {
    private final String goProxyUrl;
    private final long connectionTimeout;
    private final long readTimeout;

    public GoModFileDetectableOptions(String goProxyUrl, long connectionTimeout, long readTimeout) {
        this.goProxyUrl = goProxyUrl;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    public String getGoProxyUrl() {
        return goProxyUrl;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public long getReadTimeout() {
        return readTimeout;
    }
}
