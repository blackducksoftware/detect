package com.blackduck.integration.detect;

import org.springframework.context.ApplicationListener;

import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;

public class SpringConfigErrorListener implements ApplicationListener<ApplicationFailedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(SpringConfigErrorListener.class);

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        logger.error("An exception of type {} was encountered during application configuration.", event.getException().getClass());
        logger.error("The exception message was: {}", event.getException().getMessage());
        logger.error("A likely cause is an unparseable configuration file.");
        logger.error("Please check https://documentation.blackduck.com/bundle/detect/page/configuring/overview.html for possible configuration sources and values.");

        // issue an exit call to prevent a long confusing stacktrace from being printed by Spring
        System.exit(ExitCodeType.FAILURE_CONFIGURATION.getExitCode());
    }
}