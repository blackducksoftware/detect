/*
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.interactive;

import static com.synopsys.integration.detect.configuration.DetectProperties.BLACKDUCK_OFFLINE_MODE;
import static com.synopsys.integration.detect.configuration.DetectProperties.DETECT_PROJECT_NAME;
import static com.synopsys.integration.detect.configuration.DetectProperties.DETECT_PROJECT_VERSION_NAME;
import static com.synopsys.integration.detect.configuration.DetectProperties.DETECT_TOOLS_EXCLUDED;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.synopsys.integration.configuration.source.PropertySource;
import com.synopsys.integration.detect.configuration.DetectInfo;
import com.synopsys.integration.detect.configuration.enumeration.DetectTool;
import com.synopsys.integration.detect.lifecycle.boot.product.BlackDuckConnectivityChecker;

public class InteractiveModeDecisionTree implements DecisionTree {
    public static final String SHOULD_SAVE_TO_APPLICATION_PROPERTIES = "Would you like to save these settings to an application.properties file?";
    public static final String SHOULD_SAVE_TO_PROFILE = "Would you like save these settings to a profile?";
    public static final String SET_PROFILE_NAME = "What is the profile name?";
    public static final String SHOULD_CONNECT_TO_BLACKDUCK = "Would you like to connect to a Black Duck server?";
    public static final String SHOULD_SET_PROJECT_NAME_VERSION = "Would you like to provide a project name and version to use?";
    public static final String SET_PROJECT_NAME = "What is the project name?";
    public static final String SET_PROJECT_VERSION = "What is the project version?";
    public static final String SHOULD_RUN_SIGNATURE_SCAN = "Would you like to run a signature scan?";

    private final DetectInfo detectInfo;
    private final BlackDuckConnectivityChecker blackDuckConnectivityChecker;
    private final List<PropertySource> existingPropertySources;
    private final Gson gson;

    public InteractiveModeDecisionTree(DetectInfo detectInfo, BlackDuckConnectivityChecker blackDuckConnectivityChecker, List<PropertySource> existingPropertySources, Gson gson) {
        this.detectInfo = detectInfo;
        this.blackDuckConnectivityChecker = blackDuckConnectivityChecker;
        this.existingPropertySources = new ArrayList<>(existingPropertySources);
        this.gson = gson;
    }

    @Override
    public void traverse(InteractivePropertySourceBuilder propertySourceBuilder, InteractiveWriter writer) {
        writer.println("***** Welcome to Detect Interactive Mode *****");
        writer.println();

        Boolean connectToHub = writer.askYesOrNo(SHOULD_CONNECT_TO_BLACKDUCK);
        if (connectToHub) {
            BlackDuckConnectionDecisionBranch blackDuckConnectionDecisionBranch = new BlackDuckConnectionDecisionBranch(detectInfo, blackDuckConnectivityChecker, existingPropertySources, gson);
            blackDuckConnectionDecisionBranch.traverse(propertySourceBuilder, writer);

            Boolean customDetails = writer.askYesOrNo(SHOULD_SET_PROJECT_NAME_VERSION);
            if (customDetails) {
                propertySourceBuilder.setPropertyFromQuestion(DETECT_PROJECT_NAME, SET_PROJECT_NAME);
                propertySourceBuilder.setPropertyFromQuestion(DETECT_PROJECT_VERSION_NAME, SET_PROJECT_VERSION);
            }
        } else {
            propertySourceBuilder.setProperty(BLACKDUCK_OFFLINE_MODE, Boolean.TRUE.toString());
        }

        Boolean scan = writer.askYesOrNo(SHOULD_RUN_SIGNATURE_SCAN);
        if (scan) {
            SignatureScannerDecisionBranch signatureScannerDecisionBranch = new SignatureScannerDecisionBranch(connectToHub);
            signatureScannerDecisionBranch.traverse(propertySourceBuilder, writer);
        } else {
            propertySourceBuilder.setProperty(DETECT_TOOLS_EXCLUDED, DetectTool.SIGNATURE_SCAN.name());
        }

        writer.println("Interactive Mode Successful!");
        writer.println();

        Boolean saveSettings = writer.askYesOrNo(SHOULD_SAVE_TO_APPLICATION_PROPERTIES);
        if (saveSettings) {
            Boolean customName = writer.askYesOrNo(SHOULD_SAVE_TO_PROFILE);
            if (customName) {
                String profileName = writer.askQuestion(SET_PROFILE_NAME);

                propertySourceBuilder.saveToApplicationProperties(profileName);

                writer.println();
                writer.println("In the future, to use this profile add the following option:");
                writer.println();
                writer.println("--spring.profiles.active=" + profileName);
            } else {
                propertySourceBuilder.saveToApplicationProperties();
            }
        }

        writer.promptToStartDetect();
    }

}
