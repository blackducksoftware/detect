package com.blackduck.integration.detect.lifecycle.boot.product;

import com.blackduck.integration.blackduck.configuration.BlackDuckServerConfig;
import com.blackduck.integration.blackduck.service.BlackDuckServicesFactory;

public class BlackDuckConnectivityResult {
    private final boolean successfullyConnected;
    private final String contactedServerVersion;

    //if failure, the following is populated
    private final String failureReason;

    //if success, the following is populated
    private final BlackDuckServicesFactory blackDuckServicesFactory;
    private final BlackDuckServerConfig blackDuckServerConfig;
    private boolean isAdminOperationAllowed;

    private BlackDuckConnectivityResult(
        boolean successfullyConnected,
        String failureReason,
        BlackDuckServicesFactory blackDuckServicesFactory,
        BlackDuckServerConfig blackDuckServerConfig,
        String serverVersion,
        boolean isAdminOperationAllowed
    ) {
        this.successfullyConnected = successfullyConnected;
        this.failureReason = failureReason;
        this.blackDuckServicesFactory = blackDuckServicesFactory;
        this.blackDuckServerConfig = blackDuckServerConfig;
        this.contactedServerVersion = serverVersion.equals("") || serverVersion == null ? "0.0.0" : serverVersion;
        this.isAdminOperationAllowed = isAdminOperationAllowed;
    }

    public static BlackDuckConnectivityResult success(
        BlackDuckServicesFactory blackDuckServicesFactory,
        BlackDuckServerConfig blackDuckServerConfig,
        String serverVersion,
        boolean isAdminOperationAllowed
    ) {
        return new BlackDuckConnectivityResult(true, null, blackDuckServicesFactory, blackDuckServerConfig, serverVersion, isAdminOperationAllowed);
    }

    public static BlackDuckConnectivityResult success(
        BlackDuckServicesFactory blackDuckServicesFactory,
        BlackDuckServerConfig blackDuckServerConfig,
        String serverVersion
    ) {
        return success(blackDuckServicesFactory, blackDuckServerConfig, serverVersion, false);
    }

    public static BlackDuckConnectivityResult failure(String reason) {
        return new BlackDuckConnectivityResult(false, reason, null, null, "no connection", false);
    }

    public boolean isSuccessfullyConnected() {
        return successfullyConnected;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public BlackDuckServicesFactory getBlackDuckServicesFactory() {
        return blackDuckServicesFactory;
    }

    public BlackDuckServerConfig getBlackDuckServerConfig() {
        return blackDuckServerConfig;
    }

    public String getContactedServerVersion() {
        return this.contactedServerVersion;
    }

    public boolean isAdminOperationAllowed() {
        return isAdminOperationAllowed;
    }

    public void setAdminOperationAllowed(boolean adminOperationAllowed) {
        isAdminOperationAllowed = adminOperationAllowed;
    }
}
