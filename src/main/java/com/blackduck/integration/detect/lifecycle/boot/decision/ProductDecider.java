package com.blackduck.integration.detect.lifecycle.boot.decision;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detect.configuration.connection.BlackDuckConnectionDetails;
import com.blackduck.integration.detect.configuration.enumeration.BlackduckScanMode;

public class ProductDecider {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private boolean autonomousScanEnabled = false;
    private boolean blackduckUrlSpecified = false;
    private boolean blackduckOfflineModeSpecified = false;

    public ProductDecider(boolean autonomousScanEnabled, boolean blackduckUrlSpecified, boolean blackduckOfflineModeSpecified) {
        this.autonomousScanEnabled = autonomousScanEnabled;
        this.blackduckUrlSpecified = blackduckUrlSpecified;
        this.blackduckOfflineModeSpecified = blackduckOfflineModeSpecified;
    }

    public ProductDecider() {}

    public BlackDuckDecision decideBlackDuck(BlackDuckConnectionDetails blackDuckConnectionDetails, BlackduckScanMode scanMode, boolean hasSigScan) {
        boolean offline = blackDuckConnectionDetails.getOffline();
        Optional<String> blackDuckUrl = blackDuckConnectionDetails.getBlackDuckUrl();

        if(autonomousScanEnabled && blackduckUrlSpecified && !blackduckOfflineModeSpecified) {
            logger.debug("Black Duck SCA will run ONLINE: A Black Duck SCA url was found.");
            return BlackDuckDecision.runOnline(scanMode, hasSigScan);
        } else if (offline && (BlackduckScanMode.RAPID.equals(scanMode) || BlackduckScanMode.STATELESS.equals(scanMode))) {
            logger.debug("Black Duck SCA will NOT run: Rapid mode cannot be run offline.");
            return BlackDuckDecision.skip();
        } else if (offline) {
            logger.debug("Black Duck SCA will run: Black Duck SCA offline mode was set to true.");
            return BlackDuckDecision.runOffline();
        } else if (blackDuckUrl.isPresent()) {
            logger.debug("Black Duck SCA will run ONLINE: A Black Duck SCA url was found.");
            return BlackDuckDecision.runOnline(scanMode, hasSigScan);
        } else {
            if(autonomousScanEnabled) {
                logger.debug("Black Duck SCA will run offline as determined by autonomous scan mode.");
                return BlackDuckDecision.runOffline();
            }
            logger.debug("Black Duck SCA will NOT run: The Black Duck SCA url must be provided or offline mode must be set to true.");
            return BlackDuckDecision.skip();
        }
    }

}
