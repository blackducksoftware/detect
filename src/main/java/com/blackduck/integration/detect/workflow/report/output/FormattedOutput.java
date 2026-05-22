package com.blackduck.integration.detect.workflow.report.output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;
import com.blackduck.integration.detect.workflow.status.Status;

public class FormattedOutput {
    @SerializedName("formatVersion")
    public String formatVersion = "";

    @SerializedName("detectVersion")
    public String detectVersion = "";

    @SerializedName("projectName")
    public String projectName = "";

    @SerializedName("projectVersion")
    public String projectVersion = "";

    @SerializedName("detectors")
    public List<FormattedDetectorOutput> detectors = new ArrayList<>();

    @SerializedName("status")
    public List<FormattedStatusOutput> status = new ArrayList<>();

    @SerializedName("issues")
    public List<FormattedIssueOutput> issues = new ArrayList<>();

    @SerializedName("overallStatus")
    public List<FormattedStatusOutput> overallStatus = new ArrayList<>();

    @SerializedName("results")
    public List<FormattedResultOutput> results = new ArrayList<>();

    @SerializedName("unrecognizedPaths")
    public Map<String, List<String>> unrecognizedPaths = new HashMap<>();

    @SerializedName("codeLocations")
    public List<FormattedCodeLocationOutput> codeLocations = new ArrayList<>();

    @SerializedName("propertyValues")
    public Map<String, String> propertyValues = new HashMap<>();

    @SerializedName("operations")
    public List<FormattedOperationOutput> operations = new ArrayList();

    @SerializedName("transitiveUpgradeGuidance")
    public List<FormattedResultOutput> transitiveGuidance;

    @SerializedName("blackDuckServerProperties")
    public Map<String, String> blackDuckServerProperties;

}

