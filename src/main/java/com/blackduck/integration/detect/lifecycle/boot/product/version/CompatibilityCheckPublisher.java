package com.blackduck.integration.detect.lifecycle.boot.product.version;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.version.BlackDuckVersion;
import com.blackduck.integration.detect.configuration.DetectInfo;
import com.blackduck.integration.detect.workflow.event.EventSystem;
import com.blackduck.integration.detect.workflow.status.DetectIssue;
import com.blackduck.integration.detect.workflow.status.DetectIssueType;

public class CompatibilityCheckPublisher {
    private static final String ISSUE_TITLE = "Version compatibility";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final CompatibilityMatrixLoader compatibilityMatrixLoader = new CompatibilityMatrixLoader();
    private final CompatibilityResolver compatibilityResolver = new CompatibilityResolver();

    public void publish(DetectInfo detectInfo, EventSystem eventSystem, @Nullable BlackDuckVersion serverVersion) {
        if (serverVersion == null) {
            logger.debug("Skipping Detect - Black Duck SCA compatibility check: server version could not be parsed.");
            return;
        }

        Optional<CompatibilityMatrix> matrix = compatibilityMatrixLoader.load();
        if (matrix.isEmpty()) {
            logger.debug("Skipping Detect - Black Duck SCA compatibility check: embedded matrix could not be loaded.");
            return;
        }

        CompatibilityResult result = compatibilityResolver.resolve(matrix.get(), serverVersion, detectInfo.getDetectVersion());

        switch (result.getStatus()) {
            case COMPATIBLE:
                logger.info(result.getMessage());
                break;
            case INCOMPATIBLE:
                logger.warn(result.getMessage());
                DetectIssue.publish(eventSystem, DetectIssueType.COMPATIBILITY, ISSUE_TITLE, result.getMessage());
                break;
            case UNKNOWN:
            default:
                logger.debug(result.getMessage());
                break;
        }
    }
}
