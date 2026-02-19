package com.blackduck.integration.detect.configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class DetectPropertyUtil {
    private DetectPropertyUtil() {
        // Hiding the implicit public constructor
    }

    private static final Set<String> SENSITIVE_KEYWORDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "password",
                    "api.token",
                    "access.token",
                    "api.key"
            ))
    );

    private static final Predicate<String> PASSWORDS_AND_TOKENS_PREDICATE = propertyKey ->
            SENSITIVE_KEYWORDS.stream()
                    .anyMatch(keyword -> propertyKey.toLowerCase().contains(keyword));

    public static Predicate<String> getPasswordsAndTokensPredicate() {
        return PASSWORDS_AND_TOKENS_PREDICATE;
    }
}
