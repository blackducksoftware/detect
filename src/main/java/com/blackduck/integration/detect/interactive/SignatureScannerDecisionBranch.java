package com.blackduck.integration.detect.interactive;

import static com.blackduck.integration.detect.configuration.DetectProperties.DETECT_BLACKDUCK_SIGNATURE_SCANNER_DRY_RUN;
import static com.blackduck.integration.detect.configuration.DetectProperties.DETECT_BLACKDUCK_SIGNATURE_SCANNER_LOCAL_PATH;

public class SignatureScannerDecisionBranch implements DecisionTree {
    public static final String SHOULD_UPLOAD_TO_BLACK_DUCK = "Would you like to upload signature scan results to the Black Duck SCA server?";
    public static final String SHOULD_USE_CUSTOM_SCANNER = "Would you like to provide a custom signature scanner path?";
    public static final String SET_SCANNER_OFFLINE_LOCAL_PATH = "What is the location of your offline signature scanner?";
    private final boolean connectedToBlackDuck;

    public SignatureScannerDecisionBranch(boolean connectedToBlackDuck) {
        this.connectedToBlackDuck = connectedToBlackDuck;
    }

    @Override
    public void traverse(InteractivePropertySourceBuilder propertySourceBuilder, InteractiveWriter writer) {
        if (connectedToBlackDuck) {
            Boolean upload = writer.askYesOrNo(SHOULD_UPLOAD_TO_BLACK_DUCK);
            if (!upload) {
                propertySourceBuilder.setProperty(DETECT_BLACKDUCK_SIGNATURE_SCANNER_DRY_RUN, Boolean.TRUE.toString());
            }
        }

        Boolean customScanner = writer.askYesOrNo(SHOULD_USE_CUSTOM_SCANNER);
        if (customScanner) {
            propertySourceBuilder.setPropertyFromQuestion(DETECT_BLACKDUCK_SIGNATURE_SCANNER_LOCAL_PATH, SET_SCANNER_OFFLINE_LOCAL_PATH);
        }
    }
}
