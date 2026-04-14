package com.blackduck.integration.configuration.config;

import java.util.Map;

public class MaskedRawValueResult {
    private final String aggregatedMessage;
    private final Map<String, String> rawMap;

    public MaskedRawValueResult(String aggregatedMessage, Map<String, String> rawMap) {
        this.aggregatedMessage = aggregatedMessage;
        this.rawMap = rawMap;
    }

    public String getAggregatedMessage() {
        return aggregatedMessage;
    }

    public Map<String, String> getRawMap() {
        return rawMap;
    }
}