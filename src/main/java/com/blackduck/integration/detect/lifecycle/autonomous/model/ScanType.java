package com.blackduck.integration.detect.lifecycle.autonomous.model;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class ScanType implements Comparable<ScanType> {
    /**
     * Name of the scan tool that is applicable.
     * Example: "SIGNATURE_SCAN", "BINARY_SCAN", "DETECTOR".
     */
    private String scanTypeName;
    /**
     * Sorted property-value hash map for properties that are applicable only to the current scan type.
     * Accepts property name and value as a <code>String</code>.
     * For detector scan type, the scanProperties represent properties shared by all detectorTypes applicable.
     * Example: <code>scanProperties.put("detect.blackduck.signature.scanner.memory", "4096")</code>
     */
    private SortedMap<String, String> scanProperties = new TreeMap<>();
    /**
     * Sorted set of the absolute paths to the target that is being scanned.
     * Accepts a <code>String</code> value for the target's path.
     * For detector/signature scan type, this is the detect.source.path value
     * For binary/container scan type, this is the set of path(s) to the binary/image to be scanned.
     * <br>Example 1: <code>scanTargets.add("/path/to/build.gradle")</code>
     * <br>Example 2: <code>scanTargets.add("/path/to/binary_1.exe")</code>
     */
    private SortedSet<String> scanTargets = new TreeSet<>();

    @Override
    public int compareTo(ScanType o) {
        return scanTypeName.compareTo(o.scanTypeName);
    }

    public ScanType(String scanTypeName, SortedMap<String, String> scanProperties, SortedSet<String> scanTargets) {
        this.scanTypeName = scanTypeName;
        this.scanTargets = scanTargets;
        this.scanProperties = scanProperties;
    }

    public String getScanTypeName() {
        return scanTypeName;
    }

    public void setScanTypeName(final String scanTypeName) {
        this.scanTypeName = scanTypeName;
    }

    public Map<String, String> getScanProperties() {
        return scanProperties;
    }

    public void setScanProperties(final SortedMap<String, String> scanProperties) {
        this.scanProperties = scanProperties;
    }

    public SortedSet<String> getScanTargets() {
        return scanTargets;
    }

    public void setScanTargets(final SortedSet<String> scanTargets) {
        this.scanTargets = scanTargets;
    }
}
