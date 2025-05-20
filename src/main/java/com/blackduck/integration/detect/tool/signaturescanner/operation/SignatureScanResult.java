package com.blackduck.integration.detect.tool.signaturescanner.operation;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gson.annotations.SerializedName;

public class SignatureScanResult {
    public static final String OUTPUT_FILE_PATH = "/output/scanOutput.json";

    @SerializedName("version")
    private String version;

    @SerializedName("scanId")
    private String scanId;
    
    @SerializedName("exitStatus")
    private String exitStatus;
    
    @SerializedName("scans")
    private Map<String, String> scans;

    @SerializedName("uploadUrl")
    private String uploadUrl;
    
    // TODO perhaps externalize this class as it is used twice now
    @SerializedName("uploadUrlData")
    private UploadUrlData uploadUrlData;

    public String getVersion() {
        return version;
    }

    public String getScanId() {
        return scanId;
    }

    public String getExitStatus() {
        return exitStatus;
    }

    public Map<String, String> getScans() {
        return scans;
    }
    
    public String getUploadUrl() {
        return uploadUrl;
    }
    
    public UploadUrlData getUploadUrlData() {
        return uploadUrlData;
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

    public Set<String> parseScanIds() {
        HashSet<String> ids = new HashSet<>();
        
        // Attempt to get all scans the signature scanner might have invoked
        if (getScans() != null && !getScans().isEmpty()) {
            for (String scanId : getScans().values()) {
                // This can happen if we get a NOT_EXECUTED scan if the scanner decides not to
                // run the scan
                if (scanId != null && isValidScanID(scanId)) {
                    ids.add(scanId);
                }
            }
        } else if (getScanId() != null && isValidScanID(getScanId()))  {
            // If we are using an older version of the signature scanner, prior to 2023.1.0,
            // the scans field will not exist. Fallback to seeing if we have a high level scan ID
            ids.add(getScanId());
        }
        
        return ids;
    }
    private boolean isValidScanID(String scanId) {
        // if BlackDuck returns an invalid scanID (containing only zeros) or an invalid UUID
        // this method will guard.

        Pattern validUUIDRegex = Pattern
                .compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        Pattern invalidScanIdRegex = Pattern.compile("^0+-0+-0+-0+-0+$");

        return validUUIDRegex.matcher(scanId).matches() && !invalidScanIdRegex.matcher(scanId).matches();
    }
}
