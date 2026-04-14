package com.blackduck.integration.detect.tool.signaturescanner;

import com.blackduck.integration.blackduck.version.BlackDuckVersion;

public class SignatureScannerVersion extends BlackDuckVersion {

    public SignatureScannerVersion(int major, int minor, int patch) {
        super(major, minor, patch);
    }
    
    @Override
    public boolean isAtLeast(BlackDuckVersion other) {
        boolean thisIsOldFormat = isOldFormat(this);
        boolean otherIsOldFormat = isOldFormat(other);

        if (thisIsOldFormat == otherIsOldFormat) {
            return compareVersions(this, other);
        }

        // Treat new format as always greater than old format
        return !thisIsOldFormat;
    }

    private boolean isOldFormat(BlackDuckVersion version) {
        return version.getMajor() >= 2000;
    }

    private boolean compareVersions(BlackDuckVersion version1, BlackDuckVersion version2) {
        if (version1.getMajor() != version2.getMajor()) {
            return version1.getMajor() > version2.getMajor();
        }
        if (version1.getMinor() != version2.getMinor()) {
            return version1.getMinor() > version2.getMinor();
        }
        return version1.getPatch() >= version2.getPatch();
    }
}
