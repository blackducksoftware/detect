package com.blackduck.integration.detectable.detectables.yarn;

public class Version implements Comparable<Version> {
        int major, minor, patch;
        
        public Version(int major) {
            this(major, 0, 0);
        }
        public Version(int major, int minor) {
            this(major, minor, 0);
        }
        public Version(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }
        public Version(String[] parts) {
            this(getPartOfVersion(parts, 0), getPartOfVersion(parts, 1), getPartOfVersion(parts, 2));
        }

        private int getPartOfVersion(String[] parts, int index) {
             return parts.length > index && parts[index] != null && !parts[index].trim().isEmpty()? Integer.parseInt(parts[index]) : 0
        }
        
        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }

        @Override
        public int compareTo(Version o) {
            if (this.major == o.major
                    && this.minor == o.minor
                    && this.patch == o.patch) {
                return 0;
            } else if (this.major > o.major
                    || (this.major == o.major && this.minor > o.minor)
                    || (this.major == o.major && this.minor == o.minor && this.patch > o.patch)) {
                return 1;
            }
            return -1;
        }
    }
