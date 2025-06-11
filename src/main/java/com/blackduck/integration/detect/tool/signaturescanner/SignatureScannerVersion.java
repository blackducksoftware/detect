package com.blackduck.integration.detect.tool.signaturescanner;

import com.blackduck.integration.blackduck.version.BlackDuckVersion;

public class SignatureScannerVersion extends BlackDuckVersion {

    public SignatureScannerVersion(int major, int minor, int patch) {
        super(major, minor, patch);
    }
    
    public boolean isAtLeast(BlackDuckVersion other) {
        // Determine if this version and the other version are in the old format of year.month.iteration
        boolean thisIsOldFormat = this.getMajor() >= 2000; // Old format has large major values
        boolean otherIsOldFormat = other.getMajor() >= 2000;

        // If both versions are in the same format (either old or new), compare them directly
        if ((thisIsOldFormat && otherIsOldFormat) || (!thisIsOldFormat && !otherIsOldFormat)) {
            if (this.getMajor() > other.getMajor()) {
                return true;
            }
            if (this.getMajor() < other.getMajor()) {
                return false;
            }
            if (this.getMinor() > other.getMinor()) {
                return true;
            }
            if (this.getMinor() < other.getMinor()) {
                return false;
            }
            if (this.getPatch() > other.getPatch()) {
                return true;
            }
            if (this.getPatch() < other.getPatch()) {
                return false;
            }
            return true;
        }

        // Handle comparison between new and old formats
        // Treat new format as always greater than old format
        return !thisIsOldFormat;
    }
}
