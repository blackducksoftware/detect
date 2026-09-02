package com.blackduck.integration.detect.workflow.discovery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Root DTO for the discovery mode output JSON ({@code detect-discovery-output.json}).
 */
public class DetectorDiscoveryOutput {
    public String detectVersion;
    public String sourcePath;
    public String outputFilePath;
    public String timestamp;
    public boolean includedDocs;
    public List<String> recommendedScanTypes = new ArrayList<>();
    public Map<String, List<String>> scanTypeSignals = new LinkedHashMap<>();
    public List<DiscoveryDetectorEntry> applicableDetectors;
    public List<String> notApplicableDetectors;
}

