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
            this(   parts.length > 0 && !parts[0].isBlank()? Integer.parseInt(parts[0]) : 0,
                    parts.length > 1 && !parts[1].isBlank()? Integer.parseInt(parts[1]) : 0, 
                    parts.length > 2 && !parts[2].isBlank()? Integer.parseInt(parts[2]) : 0);
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