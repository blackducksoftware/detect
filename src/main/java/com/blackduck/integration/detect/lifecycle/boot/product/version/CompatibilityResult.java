package com.blackduck.integration.detect.lifecycle.boot.product.version;

public class CompatibilityResult {

    public enum Status {
        COMPATIBLE,
        INCOMPATIBLE,
        UNKNOWN
    }

    private final Status status;
    private final String message;

    private CompatibilityResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static CompatibilityResult compatible(String message) {
        return new CompatibilityResult(Status.COMPATIBLE, message);
    }

    public static CompatibilityResult incompatible(String message) {
        return new CompatibilityResult(Status.INCOMPATIBLE, message);
    }

    public static CompatibilityResult unknown(String message) {
        return new CompatibilityResult(Status.UNKNOWN, message);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
