package com.blackduck.integration.detect.configuration.help;

public class DetectArgumentStateParser {

    public DetectArgumentState parseArgs(String[] args) {
        return parseArgs(new ArgumentParser(args));
    }

    public DetectArgumentState parseArgs(ArgumentParser parser) {
        boolean isHelp = parser.isArgumentPresent("-h", "--help");
        boolean isHelpJsonDocument = parser.isArgumentPresent("-hjson", "--helpjson");
        boolean isHelpYamlDocument = parser.isArgumentPresent("-hyaml", "--helpyaml");
        boolean isInteractive = parser.isArgumentPresent("-i", "--interactive");
        boolean isAiAssistance = parser.isArgumentPresent("--quackstart", "--quackstart-assist");

        boolean isVerboseHelp = parser.isArgumentPresent("-hv", "--helpVerbose");
        boolean isDeprecatedHelp = parser.isArgumentPresent("-hd", "--helpDeprecated");

        boolean isDiagnostic = parser.isArgumentPresent("-d", "--diagnostic");

        boolean isGenerateAirGapZip = parser.isArgumentPresent("-z", "--zip");

        String parsedValue = null;
        if (isHelp) {
            parsedValue = parser.findValueForCommand("-h", "--help");
        } else if (isGenerateAirGapZip) {
            parsedValue = parser.findValueForCommand("-z", "--zip");
        }

        return new DetectArgumentState(
            isHelp,
            isHelpJsonDocument,
            isHelpYamlDocument,
            isInteractive,
            isVerboseHelp,
            isDeprecatedHelp,
            parsedValue,
            isDiagnostic,
            isGenerateAirGapZip,
            isAiAssistance
        );
    }

}
