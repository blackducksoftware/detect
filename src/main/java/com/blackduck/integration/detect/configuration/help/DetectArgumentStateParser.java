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

        boolean isVerboseHelp = parser.isArgumentPresent("-hv", "--helpVerbose");
        boolean isDeprecatedHelp = parser.isArgumentPresent("-hd", "--helpDeprecated");

        boolean isDiagnostic = parser.isArgumentPresent("-d", "--diagnostic");

        boolean isGenerateAirGapZip = parser.isArgumentPresent("-z", "--zip");

        boolean isDiscoveryMode = parser.isArgumentPresent("--detect.discovery", "--discover");

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
            isDiscoveryMode
        );
    }

}
