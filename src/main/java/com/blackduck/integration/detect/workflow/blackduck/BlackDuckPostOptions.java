package com.blackduck.integration.detect.workflow.blackduck;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.blackduck.api.generated.enumeration.PolicyRuleSeverityType;

public class BlackDuckPostOptions {
    private final boolean waitForResults;

    private final boolean generateRiskReport;
    private final boolean generateNoticesReport;
    private final @Nullable Path riskReportPdfPath;
    private final @Nullable Path noticesReportPath;
    private final List<PolicyRuleSeverityType> severitiesToFailPolicyCheck;
    private final List<String> policyNamesToFailPolicyCheck;
    private final boolean correlatedScanningEnabled;

    public BlackDuckPostOptions(
        boolean waitForResults,
        boolean generateRiskReport,
        boolean generateNoticesReport,
        @Nullable Path riskReportPdfPath,
        @Nullable Path noticesReportPath,
        List<PolicyRuleSeverityType> severitiesToFailPolicyCheck,
        List<String> policyNamesToFailPolicyCheck,
        boolean correlatedScanningEnabled
    ) {
        this.waitForResults = waitForResults;
        this.generateRiskReport = generateRiskReport;
        this.generateNoticesReport = generateNoticesReport;
        this.riskReportPdfPath = riskReportPdfPath;
        this.noticesReportPath = noticesReportPath;
        this.severitiesToFailPolicyCheck = severitiesToFailPolicyCheck;
        this.policyNamesToFailPolicyCheck = policyNamesToFailPolicyCheck;
        this.correlatedScanningEnabled = correlatedScanningEnabled;
    }

    public boolean shouldWaitForResults() {
        return waitForResults || shouldGenerateAnyReport() || shouldPerformAnyPolicyCheck();
    }

    public boolean shouldGenerateRiskReport() {
        return generateRiskReport;
    }

    public boolean shouldGenerateNoticesReport() {
        return generateNoticesReport;
    }

    public boolean shouldGenerateAnyReport() {
        return shouldGenerateNoticesReport() || shouldGenerateRiskReport();
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
}
