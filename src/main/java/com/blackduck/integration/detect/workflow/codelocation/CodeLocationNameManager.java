package com.blackduck.integration.detect.workflow.codelocation;

import java.io.File;

import org.springframework.lang.Nullable;

import com.blackduck.integration.util.NameVersion;

public class CodeLocationNameManager {
    private final CodeLocationNameGenerator codeLocationNameGenerator;

    public CodeLocationNameManager(CodeLocationNameGenerator codeLocationNameGenerator) {
        this.codeLocationNameGenerator = codeLocationNameGenerator;
    }

    public String createAggregateCodeLocationName(NameVersion projectNameVersion) {
        String aggregateCodeLocationName;
        if (codeLocationNameGenerator.useCodeLocationOverride()) {
            // The aggregate is exclusively used for the bdio and not the scans
            aggregateCodeLocationName = codeLocationNameGenerator.getNextCodeLocationOverrideNameUnSourced(CodeLocationNameType.BOM);
        } else {
            aggregateCodeLocationName = codeLocationNameGenerator.createAggregateStandardCodeLocationName(projectNameVersion);
        }
        return aggregateCodeLocationName;
    }

    public String createScanCodeLocationName(
        File sourcePath,
        File scanTargetPath,
        @Nullable File dockerTar,
        String projectName,
        String projectVersionName
    ) {
        String scanCodeLocationName;
        if (codeLocationNameGenerator.useCodeLocationOverride()) {
            scanCodeLocationName = codeLocationNameGenerator.getNextCodeLocationOverrideNameUnSourced(CodeLocationNameType.SIGNATURE);
        } else if (dockerTar != null) {
            scanCodeLocationName = codeLocationNameGenerator.createDockerScanCodeLocationName(dockerTar, projectName, projectVersionName);
        } else {
            scanCodeLocationName = codeLocationNameGenerator.createScanCodeLocationName(sourcePath, scanTargetPath, projectName, projectVersionName);
        }
        return scanCodeLocationName;
    }

    public String createBinaryScanCodeLocationName(File targetFile, String projectName, String projectVersionName) {
        String scanCodeLocationName;

        if (codeLocationNameGenerator.useCodeLocationOverride()) {
            scanCodeLocationName = codeLocationNameGenerator.getNextCodeLocationOverrideNameUnSourced(CodeLocationNameType.BINARY);
        } else {
            scanCodeLocationName = codeLocationNameGenerator.createBinaryScanCodeLocationName(targetFile, projectName, projectVersionName);
        }
        return scanCodeLocationName;
    }

    public String createContainerScanCodeLocationName(File targetFile, String projectName, String projectVersionName) {
        String scanCodeLocationName;

        if (codeLocationNameGenerator.useCodeLocationOverride()) {
            scanCodeLocationName = codeLocationNameGenerator.getNextCodeLocationOverrideNameUnSourced(CodeLocationNameType.CONTAINER);
        } else {
            scanCodeLocationName = codeLocationNameGenerator.createContainerScanCodeLocationName(targetFile, projectName, projectVersionName);
        }
        return scanCodeLocationName;
    }

    public String createImpactAnalysisCodeLocationName(File sourceDirectory, String projectName, String projectVersionName) {
        String scanCodeLocationName;

        if (codeLocationNameGenerator.useCodeLocationOverride()) {
            scanCodeLocationName = codeLocationNameGenerator.getNextCodeLocationOverrideNameUnSourced(CodeLocationNameType.IMPACT_ANALYSIS);
        } else {
            scanCodeLocationName = codeLocationNameGenerator.createImpactAnalysisCodeLocationName(sourceDirectory, projectName, projectVersionName);
        }
        return scanCodeLocationName;
    }

    public String createIacScanCodeLocationName(
        File targetFile, String projectName, String projectVersionName, @Nullable String prefix,
        @Nullable String suffix
    ) {
        if (codeLocationNameGenerator.useCodeLocationOverride()) {
            return codeLocationNameGenerator.getNextCodeLocationOverrideNameUnSourced(CodeLocationNameType.IAC);
        } else {
            return codeLocationNameGenerator.createIacScanCodeLocationName(targetFile, projectName, projectVersionName, prefix, suffix);
        }
    }
}
