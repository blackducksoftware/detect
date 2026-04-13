package com.blackduck.integration.detect.lifecycle.run.data;

import java.util.Optional;

import com.blackduck.integration.blackduck.configuration.BlackDuckServerConfig;
import com.blackduck.integration.blackduck.service.BlackDuckServicesFactory;
import com.blackduck.integration.blackduck.version.BlackDuckVersion;
import com.blackduck.integration.detect.configuration.enumeration.BlackduckScanMode;
import com.blackduck.integration.detect.lifecycle.boot.product.BlackDuckConnectivityResult;
import com.blackduck.integration.detect.lifecycle.boot.product.version.BlackDuckVersionParser;
import com.blackduck.integration.detect.workflow.phonehome.PhoneHomeManager;

public class BlackDuckRunData {
    private final PhoneHomeManager phoneHomeManager;
    private final BlackDuckServerConfig blackDuckServerConfig;
    private final BlackDuckServicesFactory blackDuckServicesFactory;
    private final BlackduckScanMode scanMode;
    private final boolean waitAtScanLevel;
    private Optional<BlackDuckVersion> blackDuckServerVersion;

    protected BlackDuckRunData(
        PhoneHomeManager phoneHomeManager,
        BlackDuckConnectivityResult blackDuckConnectivityResult,
        BlackDuckServicesFactory blackDuckServicesFactory,
        BlackduckScanMode scanMode,
        boolean waitAtScanLevel
    ) {
        this.phoneHomeManager = phoneHomeManager;
        this.blackDuckServerConfig = blackDuckConnectivityResult != null ? blackDuckConnectivityResult.getBlackDuckServerConfig() : null;
        this.blackDuckServicesFactory = blackDuckServicesFactory;
        this.scanMode = scanMode;
        this.waitAtScanLevel = waitAtScanLevel;

        determineBlackDuckServerVersion(blackDuckConnectivityResult);
    }

    public boolean isOnline() {
        return blackDuckServerConfig != null && blackDuckServicesFactory != null;
    }

    public Optional<PhoneHomeManager> getPhoneHomeManager() {
        return Optional.ofNullable(phoneHomeManager);
    }

    public BlackDuckServerConfig getBlackDuckServerConfig() {
        return blackDuckServerConfig;
    }

    public BlackDuckServicesFactory getBlackDuckServicesFactory() {
        return blackDuckServicesFactory;
    }

    public static BlackDuckRunData offline() {
        return new BlackDuckRunData(null, null, null, null, false);
    }

    public static BlackDuckRunData online(
        BlackduckScanMode scanMode,
        BlackDuckServicesFactory blackDuckServicesFactory,
        PhoneHomeManager phoneHomeManager,
        BlackDuckConnectivityResult blackDuckConnectivityResult,
        boolean waitAtScanLevel
    ) {
        return new BlackDuckRunData(phoneHomeManager, blackDuckConnectivityResult, blackDuckServicesFactory, scanMode, waitAtScanLevel);
    }

    public static BlackDuckRunData onlineNoPhoneHome(BlackduckScanMode scanMode, BlackDuckServicesFactory blackDuckServicesFactory, BlackDuckConnectivityResult blackDuckConnectivityResult, boolean waitAtScanLevel) {
        return new BlackDuckRunData(null, blackDuckConnectivityResult, blackDuckServicesFactory, scanMode, waitAtScanLevel);
    }

    public Boolean isNonPersistent() {
        return (scanMode == BlackduckScanMode.STATELESS || scanMode == BlackduckScanMode.RAPID);
    }

    public BlackduckScanMode getScanMode() {
        return scanMode;
    }

    //repeated method
    @Deprecated
    public boolean shouldWaitAtScanLevel() {
        return waitAtScanLevel;
    }

    public Optional<BlackDuckVersion> getBlackDuckServerVersion() {
        return blackDuckServerVersion;
    }

    private void determineBlackDuckServerVersion(BlackDuckConnectivityResult blackDuckConnectivityResult) {
        if (blackDuckConnectivityResult == null || blackDuckConnectivityResult.getContactedServerVersion() == null) {
            blackDuckServerVersion = Optional.empty();
        } else {
            BlackDuckVersionParser parser = new BlackDuckVersionParser();
            blackDuckServerVersion = parser.parse(blackDuckConnectivityResult.getContactedServerVersion());
        }
    }
}
