package com.blackduck.integration.detect.lifecycle.shutdown;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.exception.BlackDuckApiException;
import com.blackduck.integration.blackduck.exception.BlackDuckTimeoutExceededException;
import com.blackduck.integration.configuration.config.InvalidPropertyException;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.exception.IntegrationTimeoutException;
import com.blackduck.integration.rest.exception.IntegrationRestException;

public class ExceptionUtility {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String BLACKDUCK_ERROR_MESSAGE = "An unrecoverable error occurred which may be due to your environment and/or configuration. Please double check the Detect documentation: https://documentation.blackduck.com/bundle/integrations-detect/page/introduction.html";
    private static final String BLACKDUCK_TIMEOUT_ERROR_MESSAGE = "The Black Duck server did not respond within the timeout period.";

    public void logException(Exception e) {
        if (e instanceof OperationException) {
            OperationException operationException = (OperationException) e;
            logException(operationException.getException());
        } else if (e instanceof DetectUserFriendlyException) {
            if (e.getCause() != null) {
                logger.debug(e.getCause().getMessage(), e.getCause());
            }
            logger.error(e.getMessage());
        } else if (e instanceof BlackDuckTimeoutExceededException) {
            logger.error(BLACKDUCK_TIMEOUT_ERROR_MESSAGE);
            logger.error(e.getMessage());
        } else if (e instanceof BlackDuckApiException) {
            BlackDuckApiException be = (BlackDuckApiException) e;

            logger.error(BLACKDUCK_ERROR_MESSAGE);
            logger.error(be.getMessage());
            logger.debug(be.getBlackDuckErrorCode());
            logger.error(be.getOriginalIntegrationRestException().getMessage());
        } else if (e instanceof IntegrationRestException) {
            logger.error(BLACKDUCK_ERROR_MESSAGE);
            logger.debug(e.getMessage(), e);
        } else if (e instanceof IntegrationException) {
            logger.error(BLACKDUCK_ERROR_MESSAGE);
            logger.debug(e.getMessage(), e);
        } else if (e instanceof InvalidPropertyException) {
            logger.error("A configuration error occured");
            logger.debug(e.getMessage(), e);
        } else {
            logUnrecognizedException(e);
        }
    }

    public ExitCodeType getExitCodeFromException(Exception e) {
        if (e instanceof OperationException) {
            return getExitCodeFromException(((OperationException) e).getException());
        } else if (e instanceof DetectUserFriendlyException) {
            DetectUserFriendlyException friendlyException = (DetectUserFriendlyException) e;
            return friendlyException.getExitCodeType();
        } else if (e instanceof BlackDuckTimeoutExceededException || e instanceof IntegrationTimeoutException) {
            return ExitCodeType.FAILURE_TIMEOUT;
        } else if (e instanceof BlackDuckApiException) {
            return ExitCodeType.FAILURE_BLACKDUCK_DUPLICATE_PROJECT_ERROR;
        } else if (e instanceof IntegrationRestException) {
            return ExitCodeType.FAILURE_BLACKDUCK_FEATURE_ERROR;
        } else if (e instanceof IntegrationException) {
            return ExitCodeType.FAILURE_GENERAL_ERROR;
        } else if (e instanceof InvalidPropertyException) {
            return ExitCodeType.FAILURE_CONFIGURATION;
        } else {
            return ExitCodeType.FAILURE_UNKNOWN_ERROR;
        }
    }
    
    public static String summarizeException(Exception e) {
        if (e instanceof OperationException) {
            return summarizeException(((OperationException) e).getException());
        } else {
            return e.getMessage();
        }
    }

    private void logUnrecognizedException(Exception e) {
        logger.error("An unknown/unexpected error occurred");
        if (e.getMessage() != null) {
            logger.error(e.getMessage());
        } else if (e instanceof NullPointerException) {
            logger.error("Null Pointer Exception");
        } else {
            logger.error(e.getClass().getSimpleName());
        }
        if (e.getStackTrace().length >= 1) {
            logger.error("Thrown at " + e.getStackTrace()[0].toString());
        }
    }
}
