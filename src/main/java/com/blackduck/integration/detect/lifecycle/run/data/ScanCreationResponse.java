package com.blackduck.integration.detect.lifecycle.run.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScanCreationResponse {

    private String scanId;
    private String uploadUrl;
    private UploadUrlData uploadUrlData;

    public String getScanId() {
        return scanId;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public UploadUrlData getUploadUrlData() {
        return uploadUrlData;
    }

    public Map<String, String> getAllHeaders() {
        Map<String, String> allHeaders = new HashMap<>();
        List<Map<String, String>> headers = uploadUrlData.getHeaders();
        
        for (Map<String, String> singleHeader : headers) {
            allHeaders.put(singleHeader.get("name"), singleHeader.get("value"));
        }
        
        return allHeaders;
    }

    public static class UploadUrlData {
        private String method;
        private List<Map<String, String>> headers;

        public String getMethod() {
            return method;
        }

        public List<Map<String, String>> getHeaders() {
            return headers;
        }
    }
}