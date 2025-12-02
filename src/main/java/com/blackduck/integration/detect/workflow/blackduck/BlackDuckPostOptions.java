package com.blackduck.integration.detect.workflow.blackduck;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;

public class BlackDuckPostOptions {
    private final boolean waitForResults;

    private final boolean generateRiskReportPdf;
    private final boolean generateRiskReportJson;
    private final boolean generateNoticesReport;
    private final boolean generateSbomReport;
    private final @Nullable Path riskReportPdfPath;
    private final @Nullable Path riskReportJsonPath;
    private final @Nullable Path noticesReportPath;
    private final List<PolicyRuleSeverityType> severitiesToFailPolicyCheck;
    private final List<String> policyNamesToFailPolicyCheck;
    private final boolean correlatedScanningEnabled;
    private final List<String> sbomProjectTypes;
    private final List<String> sbomExcludeTypes;

    public BlackDuckPostOptions(
        boolean waitForResults,
        boolean generateRiskReportPdf,
        boolean generateNoticesReport,
        @Nullable Path riskReportPdfPath,
        @Nullable Path noticesReportPath,
        List<PolicyRuleSeverityType> severitiesToFailPolicyCheck,
        List<String> policyNamesToFailPolicyCheck,
        boolean correlatedScanningEnabled,
        boolean generateRiskReportJson,
        @Nullable Path riskReportJsonPath,
        boolean generateSbomReport,
        List<String> sbomProjectTypes,
        List<String> sbomExcludeTypes
    ) {
        this.waitForResults = waitForResults;
        this.generateRiskReportPdf = generateRiskReportPdf;
        this.generateNoticesReport = generateNoticesReport;
        this.riskReportPdfPath = riskReportPdfPath;
        this.noticesReportPath = noticesReportPath;
        this.severitiesToFailPolicyCheck = severitiesToFailPolicyCheck;
        this.policyNamesToFailPolicyCheck = policyNamesToFailPolicyCheck;
        this.correlatedScanningEnabled = correlatedScanningEnabled;
        this.generateRiskReportJson = generateRiskReportJson;
        this.riskReportJsonPath = riskReportJsonPath;
        this.generateSbomReport = generateSbomReport;
        this.sbomProjectTypes = sbomProjectTypes;
        this.sbomExcludeTypes = sbomExcludeTypes;
    }

    public boolean shouldWaitForResults() {
        return waitForResults || shouldGenerateAnyReport() || shouldPerformAnyPolicyCheck();
    }

    public boolean shouldGenerateRiskReportPdf() {
        return generateRiskReportPdf;
    }

    public boolean shouldGenerateRiskReportJson() {
        return generateRiskReportJson;
    }

    public boolean shouldGenerateNoticesReport() {
        return generateNoticesReport;
    }

    public boolean shouldGenerateSbomReport() {
        return generateSbomReport;
    }

    public boolean shouldGenerateAnyReport() {
        return shouldGenerateNoticesReport() || shouldGenerateRiskReportPdf() || shouldGenerateRiskReportJson();
    }

    public boolean shouldPerformSeverityPolicyCheck() {
        return CollectionUtils.isNotEmpty(getSeveritiesToFailPolicyCheck());
    }

    public boolean shouldPerformNamePolicyCheck() {
        return CollectionUtils.isNotEmpty(getPolicyNamesToFailPolicyCheck());
    }

    public boolean shouldPerformAnyPolicyCheck() {
        return shouldPerformSeverityPolicyCheck() || shouldPerformNamePolicyCheck();
    }

    public Optional<Path> getRiskReportPdfPath() {
        return Optional.ofNullable(riskReportPdfPath);
    }

    public Optional<Path> getRiskReportJsonPath() {
        return Optional.ofNullable(riskReportJsonPath);
    }

    public Optional<Path> getNoticesReportPath() {
        return Optional.ofNullable(noticesReportPath);
    }

    public List<PolicyRuleSeverityType> getSeveritiesToFailPolicyCheck() {
        return severitiesToFailPolicyCheck;
    }

    public List<String> getPolicyNamesToFailPolicyCheck() {
        return policyNamesToFailPolicyCheck;
    }

    public boolean isCorrelatedScanningEnabled() {
        return correlatedScanningEnabled;
    }

    public List<String> getSbomProjectTypes() {
        return sbomProjectTypes;
    }

    public List<String> getSbomExcludeTypes() {
        return sbomExcludeTypes;
    }
}
