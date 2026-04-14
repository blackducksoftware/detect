package com.blackduck.integration.detect.workflow.blackduck.report;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

public class ReportData {
    @SerializedName("projectName")
    private String projectName;

    @SerializedName("projectURL")
    private String projectURL;

    @SerializedName("projectVersion")
    private String projectVersion;

    @SerializedName("projectVersionURL")
    private String projectVersionURL;

    @SerializedName("phase")
    private String phase;

    @SerializedName("distribution")
    private String distribution;

    @SerializedName("components")
    private List<BomComponent> components;

    @SerializedName("totalComponents")
    private int totalComponents;

    @SerializedName("dateTimeOfLatestScan")
    private LocalDateTime dateTimeOfLatestScan;

    @SerializedName("securityRiskCounts")
    private final BomRiskCounts securityRiskCounts = new BomRiskCounts();

    @SerializedName("licenseRiskCounts")
    private final BomRiskCounts licenseRiskCounts = new BomRiskCounts();

    @SerializedName("operationalRiskCounts")
    private final BomRiskCounts operationalRiskCounts = new BomRiskCounts();

    @SerializedName("vulnerabilityRiskNoneCount")
    private int vulnerabilityRiskNoneCount;

    @SerializedName("licenseRiskNoneCount")
    private int licenseRiskNoneCount;

    @SerializedName("operationalRiskNoneCount")
    private int operationalRiskNoneCount;

    public String htmlEscape(String valueToEscape) {
        if (StringUtils.isBlank(valueToEscape)) {
            return null;
        }
        return StringEscapeUtils.escapeHtml4(valueToEscape);
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectURL() {
        return projectURL;
    }

    public void setProjectURL(String projectURL) {
        this.projectURL = projectURL;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }

    public String getProjectVersionURL() {
        return projectVersionURL;
    }

    public void setProjectVersionURL(String projectVersionURL) {
        this.projectVersionURL = projectVersionURL;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getDistribution() {
        return distribution;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    public int getTotalComponents() {
        return totalComponents;
    }

    public Optional<LocalDateTime> getDateTimeOfLatestScan() {
        return Optional.ofNullable(dateTimeOfLatestScan);
    }

    public void setDateTimeOfLatestScan(LocalDateTime dateTimeOfLatestScan) {
        this.dateTimeOfLatestScan = dateTimeOfLatestScan;
    }

    public int getVulnerabilityRiskCriticalCount() {
        return securityRiskCounts.getCritical();
    }

    public int getVulnerabilityRiskHighCount() {
        return securityRiskCounts.getHigh();
    }

    public int getVulnerabilityRiskMediumCount() {
        return securityRiskCounts.getMedium();
    }

    public int getVulnerabilityRiskLowCount() {
        return securityRiskCounts.getLow();
    }

    public int getVulnerabilityRiskNoneCount() {
        return vulnerabilityRiskNoneCount;
    }

    public int getLicenseRiskHighCount() {
        return licenseRiskCounts.getHigh();
    }

    public int getLicenseRiskMediumCount() {
        return licenseRiskCounts.getMedium();
    }

    public int getLicenseRiskLowCount() {
        return licenseRiskCounts.getLow();
    }

    public int getLicenseRiskNoneCount() {
        return licenseRiskNoneCount;
    }

    public int getOperationalRiskHighCount() {
        return operationalRiskCounts.getHigh();
    }

    public int getOperationalRiskMediumCount() {
        return operationalRiskCounts.getMedium();
    }

    public int getOperationalRiskLowCount() {
        return operationalRiskCounts.getLow();
    }

    public int getOperationalRiskNoneCount() {
        return operationalRiskNoneCount;
    }

    public List<BomComponent> getComponents() {
        return components;
    }

    public void setComponents(List<BomComponent> components) {
        this.components = components;

        for (BomComponent component : components) {
            securityRiskCounts.add(component.getSecurityRiskCounts());
            licenseRiskCounts.add(component.getLicenseRiskCounts());
            operationalRiskCounts.add(component.getOperationalRiskCounts());
        }

        totalComponents = components.size();

        vulnerabilityRiskNoneCount =
            totalComponents - getVulnerabilityRiskCriticalCount() - getVulnerabilityRiskHighCount() - getVulnerabilityRiskMediumCount() - getVulnerabilityRiskLowCount();
        licenseRiskNoneCount = totalComponents - getLicenseRiskHighCount() - getLicenseRiskMediumCount() - getLicenseRiskLowCount();
        operationalRiskNoneCount = totalComponents - getOperationalRiskHighCount() - getOperationalRiskMediumCount() - getOperationalRiskLowCount();
    }

}
