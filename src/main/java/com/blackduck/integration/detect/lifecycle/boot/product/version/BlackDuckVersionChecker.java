package com.blackduck.integration.detect.lifecycle.boot.product.version;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.version.BlackDuckVersion;

public class BlackDuckVersionChecker {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final BlackDuckVersionParser parser;
    private final BlackDuckMinimumVersionChecks blackDuckMinimumVersionChecks;
    private final BlackDuckVersionSensitiveOptions blackDuckVersionSensitiveOptions;

    public BlackDuckVersionChecker(
        BlackDuckVersionParser parser,
        BlackDuckMinimumVersionChecks blackDuckMinimumVersionChecks,
        BlackDuckVersionSensitiveOptions blackDuckVersionSensitiveOptions
    ) {
        this.parser = parser;
        this.blackDuckMinimumVersionChecks = blackDuckMinimumVersionChecks;
        this.blackDuckVersionSensitiveOptions = blackDuckVersionSensitiveOptions;
    }

    public BlackDuckVersionCheckerResult check(String actualBlackDuckVersionString) {
        Optional<BlackDuckVersion> actualBlackDuckVersion = parser.parse(actualBlackDuckVersionString);
        if (!actualBlackDuckVersion.isPresent()) {
            logger.debug("Unable to parse Black SCA Duck version string {}, so unable to perform version compatibility check", actualBlackDuckVersion);
            return BlackDuckVersionCheckerResult.passed();
        }
        for (BlackDuckMinimumVersionCheck blackDuckMinimumVersionCheck : blackDuckMinimumVersionChecks.get()) {
            if (blackDuckMinimumVersionCheck.getTest().test(blackDuckVersionSensitiveOptions) && !actualBlackDuckVersion.get()
                .isAtLeast(blackDuckMinimumVersionCheck.getMinimumBlackDuckVersion())) {
                String msg = String.format("%s requires at least Black Duck SCA version %s but the connected server is %s.",
                    blackDuckMinimumVersionCheck.getDescription(), blackDuckMinimumVersionCheck.getMinimumBlackDuckVersion(),
                    actualBlackDuckVersion.get()
                );
                logger.debug(msg);
                return BlackDuckVersionCheckerResult.failed(msg);
            }
        }
        logger.trace("Version compatibility check passed");
        return BlackDuckVersionCheckerResult.passed();
    }
}
