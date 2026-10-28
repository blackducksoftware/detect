package com.blackduck.integration.detect.workflow.discovery;

import java.util.List;

/**
 * One applicable detector in the discovery output, including which file triggered it
 * and the full catalog of Detect flags that apply to it.
 */
public class DiscoveryDetectorEntry {
    public String detectorType;
    public String detectableName;
    public String triggeredBy;
    public String triggeredByPath;
    public String foundInDirectory;
    public List<DiscoveryFlagEntry> flags;
    public String docContent;
}

