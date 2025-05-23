package com.blackduck.integration.detect.workflow.phonehome;

import java.util.HashMap;
import java.util.Map;

import com.blackduck.integration.blackduck.phonehome.BlackDuckPhoneHomeHelper;
import com.blackduck.integration.detect.configuration.DetectInfo;
import com.blackduck.integration.detect.workflow.event.EventSystem;
import com.blackduck.integration.phonehome.PhoneHomeResponse;

public class OnlinePhoneHomeManager extends PhoneHomeManager {
    private final BlackDuckPhoneHomeHelper blackDuckPhoneHomeHelper;
    private final boolean isAdminOperationAllowed;

    public OnlinePhoneHomeManager(Map<String, String> additionalMetaData, DetectInfo detectInfo, EventSystem eventSystem, BlackDuckPhoneHomeHelper blackDuckPhoneHomeHelper, boolean isAdminOperationAllowed) {
        super(additionalMetaData, detectInfo, eventSystem);
        this.blackDuckPhoneHomeHelper = blackDuckPhoneHomeHelper;
        this.isAdminOperationAllowed = isAdminOperationAllowed;
    }

    @Override
    public PhoneHomeResponse phoneHome(Map<String, String> metadata, String... artifactModules) {
        Map<String, String> metaDataToSend = new HashMap<>();
        metaDataToSend.putAll(metadata);
        metaDataToSend.putAll(detectorTypesMetadata);
        metaDataToSend.putAll(additionalMetaData);
        return blackDuckPhoneHomeHelper.handlePhoneHome("detect", detectInfo.getDetectVersion(), metaDataToSend, isAdminOperationAllowed, artifactModules);
    }

}
