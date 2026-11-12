package com.blackduck.integration.detect.workflow.discovery;

import java.util.List;

/**
 * Root DTO for the discovery mode output JSON ({@code detect-discovery-output.json}).
 */
public class DetectorDiscoveryOutput {
    public String detectVersion;
    public String sourcePath;
    public String outputFilePath;
    public String timestamp;
    public boolean includedDocs;
    public List<DiscoveryDetectorEntry> applicableDetectors;
    public List<String> notApplicableDetectors;
}

