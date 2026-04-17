package com.blackduck.integration.detect.configuration.help;

import org.jetbrains.annotations.Nullable;

public class DetectArgumentState {
    private final boolean isHelp;
    private final boolean isHelpJsonDocument;

    private final boolean isHelpYamlDocument;
    private final boolean isInteractive;

    private final boolean isVerboseHelp;
    private final boolean isDeprecatedHelp;
    @Nullable
    private final String parsedValue;

    private final boolean isDiagnostic;

    private final boolean isGenerateAirGapZip;

    private final boolean isAiAssistance;

    private final boolean isQuackStartExpress;

    private final boolean isCacheConfig;

    public DetectArgumentState(
        boolean isHelp,
        boolean isHelpJsonDocument,
        boolean isHelpYamlDocument,
        boolean isInteractive,
        boolean isVerboseHelp,
        boolean isDeprecatedHelp,
        @Nullable String parsedValue,
        boolean isDiagnostic,
        boolean isGenerateAirGapZip
    ) {
        this(isHelp, isHelpJsonDocument, isHelpYamlDocument, isInteractive,
            isVerboseHelp, isDeprecatedHelp, parsedValue, isDiagnostic, isGenerateAirGapZip, false, false, false);
    }

    public DetectArgumentState(
        boolean isHelp,
        boolean isHelpJsonDocument,
        boolean isHelpYamlDocument,
        boolean isInteractive,
        boolean isVerboseHelp,
        boolean isDeprecatedHelp,
        @Nullable String parsedValue,
        boolean isDiagnostic,
        boolean isGenerateAirGapZip,
        boolean isAiAssistance
    ) {
        this(isHelp, isHelpJsonDocument, isHelpYamlDocument, isInteractive,
            isVerboseHelp, isDeprecatedHelp, parsedValue, isDiagnostic, isGenerateAirGapZip, isAiAssistance, false, false);
    }

    public DetectArgumentState(
        boolean isHelp,
        boolean isHelpJsonDocument,
        boolean isHelpYamlDocument,
        boolean isInteractive,
        boolean isVerboseHelp,
        boolean isDeprecatedHelp,
        @Nullable String parsedValue,
        boolean isDiagnostic,
        boolean isGenerateAirGapZip,
        boolean isAiAssistance,
        boolean isQuackStartExpress
    ) {
        this(isHelp, isHelpJsonDocument, isHelpYamlDocument, isInteractive,
            isVerboseHelp, isDeprecatedHelp, parsedValue, isDiagnostic, isGenerateAirGapZip, isAiAssistance, isQuackStartExpress, false);
    }

    public DetectArgumentState(
        boolean isHelp,
        boolean isHelpJsonDocument,
        boolean isHelpYamlDocument,
        boolean isInteractive,
        boolean isVerboseHelp,
        boolean isDeprecatedHelp,
        @Nullable String parsedValue,
        boolean isDiagnostic,
        boolean isGenerateAirGapZip,
        boolean isAiAssistance,
        boolean isQuackStartExpress,
        boolean isCacheConfig
    ) {
        this.isHelp = isHelp;
        this.isHelpJsonDocument = isHelpJsonDocument;
        this.isHelpYamlDocument = isHelpYamlDocument;
        this.isInteractive = isInteractive;
        this.isVerboseHelp = isVerboseHelp;
        this.isDeprecatedHelp = isDeprecatedHelp;
        this.parsedValue = parsedValue;
        this.isDiagnostic = isDiagnostic;
        this.isGenerateAirGapZip = isGenerateAirGapZip;
        this.isAiAssistance = isAiAssistance;
        this.isQuackStartExpress = isQuackStartExpress;
        this.isCacheConfig = isCacheConfig;
    }

    public boolean isHelp() {
        return isHelp;
    }

    public boolean isHelpJsonDocument() {
        return isHelpJsonDocument;
    }

    public boolean isHelpYamlDocument() {
        return isHelpYamlDocument;
    }

    public boolean isInteractive() {
        return isInteractive;
    }

    public boolean isVerboseHelp() {
        return isVerboseHelp;
    }

    public boolean isDeprecatedHelp() {
        return isDeprecatedHelp;
    }

    public boolean isDiagnostic() {
        return isDiagnostic;
    }

    @Nullable
    public String getParsedValue() {
        return parsedValue;
    }

    public boolean isGenerateAirGapZip() {
        return isGenerateAirGapZip;
    }

    public boolean isAiAssistance() {
        return isAiAssistance;
    }

    public boolean isQuackStartExpress() {
        return isQuackStartExpress;
    }

    public boolean isCacheConfig() {
        return isCacheConfig;
    }
}
