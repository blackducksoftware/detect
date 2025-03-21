package com.blackduck.integration.configuration.config;

public class InvalidPropertyKeyException extends RuntimeException {
    public InvalidPropertyKeyException(String propertyKeys) {
        super(String.format(
                "Invalid property key(s): %s",
                propertyKeys
        ));
    }
}