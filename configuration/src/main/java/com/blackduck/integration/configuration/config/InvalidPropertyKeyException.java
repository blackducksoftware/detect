package com.blackduck.integration.configuration.config;

import com.blackduck.integration.configuration.parse.ValueParseException;

public class InvalidPropertyKeyException extends RuntimeException {
    public InvalidPropertyKeyException(String message) {
        super(message);
    }
}