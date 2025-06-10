package com.blackduck.integration.detect.tool.signaturescanner;

public class SignatureScanReportStatus {
    
    private boolean success;
    
    public SignatureScanReportStatus() {
        this.success = true;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
