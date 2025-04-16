package com.blackduck.integration.detect.lifecycle.boot.product;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.blackduck.integration.blackduck.configuration.BlackDuckServerConfig;
import com.blackduck.integration.blackduck.phonehome.BlackDuckPhoneHomeHelper;
import com.blackduck.integration.blackduck.service.BlackDuckServicesFactory;
import com.blackduck.integration.detect.configuration.DetectConfigurationFactory;
import com.blackduck.integration.detect.configuration.DetectInfo;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.connection.BlackDuckConfigFactory;
import com.blackduck.integration.detect.configuration.connection.BlackDuckConnectionDetails;
import com.blackduck.integration.detect.workflow.event.EventSystem;
import com.blackduck.integration.detect.workflow.phonehome.OnlinePhoneHomeManager;
import com.blackduck.integration.detect.workflow.phonehome.PhoneHomeManager;
import com.blackduck.integration.detect.workflow.phonehome.PhoneHomeCredentials;
import com.blackduck.integration.log.SilentIntLogger;

public class ProductBootFactory {
    private final DetectInfo detectInfo;
    private final EventSystem eventSystem;
    private final DetectConfigurationFactory detectConfigurationFactory;

    public ProductBootFactory(DetectInfo detectInfo, EventSystem eventSystem, DetectConfigurationFactory detectConfigurationFactory) {
        this.detectInfo = detectInfo;
        this.eventSystem = eventSystem;
        this.detectConfigurationFactory = detectConfigurationFactory;
    }

    private PhoneHomeManager createPhoneHomeManagerInternal(BlackDuckServicesFactory blackDuckServicesFactory, PhoneHomeCredentials phoneHomeCredentials, Boolean isAdminOperationAllowed) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        BlackDuckPhoneHomeHelper blackDuckPhoneHomeHelper = BlackDuckPhoneHomeHelper.createAsynchronousPhoneHomeHelper(
            blackDuckServicesFactory, phoneHomeCredentials.getApiSecret(), phoneHomeCredentials.getMeasurementId(), executorService);

        if (isAdminOperationAllowed != null) {
            return new OnlinePhoneHomeManager(
                detectConfigurationFactory.createPhoneHomeOptions().getPassthrough(),
                detectInfo,
                eventSystem,
                blackDuckPhoneHomeHelper,
                isAdminOperationAllowed
            );
        } else {
            return new OnlinePhoneHomeManager(
                detectConfigurationFactory.createPhoneHomeOptions().getPassthrough(),
                detectInfo,
                eventSystem,
                blackDuckPhoneHomeHelper
            );
        }
    }

    public PhoneHomeManager createPhoneHomeManager(BlackDuckServicesFactory blackDuckServicesFactory, PhoneHomeCredentials phoneHomeCredentials) {
        return createPhoneHomeManagerInternal(blackDuckServicesFactory, phoneHomeCredentials, null);
    }

    public PhoneHomeManager createPhoneHomeManager(BlackDuckServicesFactory blackDuckServicesFactory, PhoneHomeCredentials phoneHomeCredentials, boolean isAdminOperationAllowed) {
        return createPhoneHomeManagerInternal(blackDuckServicesFactory, phoneHomeCredentials, isAdminOperationAllowed);
    }

    public BlackDuckServerConfig createBlackDuckServerConfig() throws DetectUserFriendlyException {
        BlackDuckConnectionDetails connectionDetails = detectConfigurationFactory.createBlackDuckConnectionDetails();
        BlackDuckConfigFactory blackDuckConfigFactory = new BlackDuckConfigFactory(detectInfo, connectionDetails);
        return blackDuckConfigFactory.createServerConfig(new SilentIntLogger());
    }
}
