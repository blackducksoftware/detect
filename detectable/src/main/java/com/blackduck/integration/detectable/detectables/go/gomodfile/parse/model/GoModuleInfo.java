package com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model;

import java.util.Objects;

/**
 * Represents a Go module with name and version information.
 */
public class GoModuleInfo {
    private final String name;
    private final String version;
    private final boolean isIndirect;

    public GoModuleInfo(String name, String version, boolean isIndirect) {
        this.name = name;
        this.version = version;
        this.isIndirect = isIndirect;
    }

    public GoModuleInfo(String name, String version) {
        this(name, version, false);
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public boolean isIndirect() {
        return isIndirect;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoModuleInfo that = (GoModuleInfo) o;
        return Objects.equals(name, that.name) && Objects.equals(version, that.version) && isIndirect == that.isIndirect;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, isIndirect);
    }

    @Override
    public String toString() {
        return "GoModuleInfo{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", isIndirect=" + isIndirect +
                '}';
    }
}
