package com.blackduck.integration.detect.lifecycle.run.operation.blackduck;

import java.io.File;

import com.blackduck.integration.detect.lifecycle.run.data.ScanCreationResponse;

public class ScassScanInitiationResult {
    private File fileToUpload;
    private String md5Hash;
    private ScanCreationResponse scanCreationResponse;

    public void setFileToUpload(File fileToUpload) {
        this.fileToUpload = fileToUpload;
    }

    public File getFileToUpload() {
        return fileToUpload;
    }

    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public void setScanCreationResponse(ScanCreationResponse scanCreationResponse) {
        this.scanCreationResponse = scanCreationResponse;
    }

    public ScanCreationResponse getScanCreationResponse() {
        return scanCreationResponse;
    }
}
