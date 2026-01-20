package com.blackduck.integration.detect.configuration;

import java.util.function.Predicate;

public class DetectPropertyUtil {
    private DetectPropertyUtil() {
        // Hiding the implicit public constructor
    }

    private static final Predicate<String> PASSWORDS_AND_TOKENS_PREDICATE = propertyKey ->
        propertyKey.toLowerCase().contains("password")
            || propertyKey.toLowerCase().contains("api.token")
            || propertyKey.toLowerCase().contains("access.token")
            || propertyKey.toLowerCase().contains("api.key");

    public static Predicate<String> getPasswordsAndTokensPredicate() {
        return PASSWORDS_AND_TOKENS_PREDICATE;
    }
}
