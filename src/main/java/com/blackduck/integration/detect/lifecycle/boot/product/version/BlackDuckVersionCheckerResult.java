package com.blackduck.integration.detect.lifecycle.boot.product.version;

public class BlackDuckVersionCheckerResult {
    private final boolean passed;
    private final String message;

    public static BlackDuckVersionCheckerResult passed() {
        return new BlackDuckVersionCheckerResult(true, "Black Duck SCA version check passed");
    }

    public static BlackDuckVersionCheckerResult failed(String message) {
        return new BlackDuckVersionCheckerResult(false, message);
    }

    private BlackDuckVersionCheckerResult(boolean passed, String message) {
        this.passed = passed;
        this.message = message;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getMessage() {
        return message;
    }
}
