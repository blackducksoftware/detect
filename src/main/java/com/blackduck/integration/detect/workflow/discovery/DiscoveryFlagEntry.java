package com.blackduck.integration.detect.workflow.discovery;

/**
 * A single configurable Detect flag for an applicable detector, as surfaced by discovery mode.
 * Values are pulled live from {@code DetectProperties} so they always match the running Detect version.
 */
public class DiscoveryFlagEntry {
    public String key;
    public String name;
    public String type;
    public String defaultValue;
    public String description;
    public String helpText;

    public DiscoveryFlagEntry() {
    }

    public DiscoveryFlagEntry(String key, String name, String type, String defaultValue, String description, String helpText) {
        this.key = key;
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.description = description;
        this.helpText = helpText;
    }
}

