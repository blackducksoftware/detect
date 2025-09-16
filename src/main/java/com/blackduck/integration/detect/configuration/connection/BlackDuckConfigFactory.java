package com.blackduck.integration.detect.configuration.connection;

import java.util.Optional;
import java.util.concurrent.Executors;

import com.blackduck.integration.blackduck.configuration.BlackDuckServerConfig;
import com.blackduck.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.blackduck.integration.common.util.ProxyUtil;
import com.blackduck.integration.detect.configuration.DetectInfo;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.log.IntLogger;
import com.blackduck.integration.log.SilentIntLogger;
import com.blackduck.integration.rest.proxy.ProxyInfo;
import com.blackduck.integration.util.NameVersion;

public class BlackDuckConfigFactory {
    private final BlackDuckConnectionDetails blackDuckConnectionDetails;
    private final DetectInfo detectInfo;
    private static final String BLACK_DUCK_SERVER_CONFIG_BUILDER_TIMEOUT_KEY = "blackduck.timeout";

    public BlackDuckConfigFactory(DetectInfo detectInfo, BlackDuckConnectionDetails blackDuckConnectionDetails) {
        this.detectInfo = detectInfo;
        this.blackDuckConnectionDetails = blackDuckConnectionDetails;
    }

    public BlackDuckServerConfig createServerConfig(IntLogger intLogger) throws DetectUserFriendlyException {
        IntLogger logger;
        if (intLogger == null) {
            logger = new SilentIntLogger();
        } else {
            logger = intLogger;
        }
        ConnectionDetails connectionDetails = blackDuckConnectionDetails.getConnectionDetails();

        BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = BlackDuckServerConfig.newApiTokenBuilder()
            .setExecutorService(Executors.newFixedThreadPool(blackDuckConnectionDetails.getParallelProcessors()))
            .setLogger(logger);

        blackDuckServerConfigBuilder.setProperties(blackDuckConnectionDetails.getBlackduckProperties().entrySet());
        blackDuckServerConfigBuilder.setProperty(BLACK_DUCK_SERVER_CONFIG_BUILDER_TIMEOUT_KEY, blackDuckConnectionDetails.getConnectionDetails().getTimeout().toString());
        blackDuckServerConfigBuilder.setSolutionDetails(new NameVersion("detect", detectInfo.getDetectVersion()));
        Optional<Boolean> shouldIgnore = blackDuckConnectionDetails.getBlackDuckUrl()
            .map(blackduckUrl -> ProxyUtil.shouldIgnoreUrl(blackduckUrl, connectionDetails.getIgnoredProxyHostPatterns(), logger));
        if (shouldIgnore.isPresent() && Boolean.TRUE.equals(shouldIgnore.get())) {
            blackDuckServerConfigBuilder.setProxyInfo(ProxyInfo.NO_PROXY_INFO);
        } else {
            blackDuckServerConfigBuilder.setProxyInfo(connectionDetails.getProxyInformation());
        }

        try {
            return blackDuckServerConfigBuilder.build();
        } catch (IllegalArgumentException e) {
            throw new DetectUserFriendlyException("Failed to configure Black Duck SCA server connection: " + e.getMessage(), e, ExitCodeType.FAILURE_CONFIGURATION);
        }
    }
}
