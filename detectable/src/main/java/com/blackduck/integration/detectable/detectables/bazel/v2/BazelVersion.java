package com.blackduck.integration.detectable.detectables.bazel.v2;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a parsed Bazel version (major.minor.patch).
 * Supports comparison to gate features that require a minimum Bazel version.
 */
public class BazelVersion {
    // Matches "bazel X.Y.Z" with optional suffix (e.g., "-rc1", "-pre.20231010.1")
    private static final Pattern VERSION_PATTERN = Pattern.compile("(?i)bazel\\s+(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private final int major;
    private final int minor;
    private final int patch;

    public BazelVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    /**
     * Returns true if this version is at least the given major.minor.
     * For example, isAtLeast(7, 1) returns true for 7.1.0, 7.2.0, 8.0.0, etc.
     */
    public boolean isAtLeast(int requiredMajor, int requiredMinor) {
        if (major != requiredMajor) {
            return major > requiredMajor;
        }
        return minor >= requiredMinor;
    }

    /**
     * Parses the output of `bazel --version` (e.g., "bazel 7.4.1") into a BazelVersion.
     * Returns empty if the output cannot be parsed.
     */
    public static Optional<BazelVersion> parse(String versionOutput) {
        if (versionOutput == null || versionOutput.trim().isEmpty()) {
            return Optional.empty();
        }
        Matcher matcher = VERSION_PATTERN.matcher(versionOutput);
        if (!matcher.find()) {
            return Optional.empty();
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        return Optional.of(new BazelVersion(major, minor, patch));
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BazelVersion that = (BazelVersion) o;
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }
}

