package com.blackduck.integration.detect.lifecycle.run.operation.blackduck;

import java.io.File;

import com.blackduck.integration.detect.lifecycle.run.data.ScanCreationResponse;

public class ScassScanInitiationResult {
    private File zipFile;
    private String zipMd5;
    private ScanCreationResponse scanCreationResponse;

    public void setZipFile(File zipFile) {
        this.zipFile = zipFile;
    }

    public File getZipFile() {
        return zipFile;
    }

    public void setZipMd5(String md5) {
        this.zipMd5 = md5;
    }

    public String getZipMd5() {
        return zipMd5;
    }

    public void setScanCreationResponse(ScanCreationResponse scanCreationResponse) {
        this.scanCreationResponse = scanCreationResponse;
    }

    public ScanCreationResponse getScanCreationResponse() {
        return scanCreationResponse;
    }
}
