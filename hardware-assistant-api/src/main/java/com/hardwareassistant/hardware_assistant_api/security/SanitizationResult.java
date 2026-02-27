package com.hardwareassistant.hardware_assistant_api.security;

/**
 * Replaces the old "throw SecurityException" approach.
 * InputSanitizer now returns a typed result the caller can act on.
 */
public record SanitizationResult(
        Status status,
        String sanitizedInput,
        String matchedPattern,
        String fieldName,
        String message
) {
    public enum Status {
        CLEAN,    // Input is fine, use sanitizedInput
        WARNING,  // Suspicious input — first/second offence, warn the user
        BLOCKED   // Repeat offender or severe pattern — reject the request
    }

    // ── Factory helpers ────────────────────────────────────────────────────

    public static SanitizationResult clean(String input) {
        return new SanitizationResult(Status.CLEAN, input, null, null, null);
    }

    public static SanitizationResult warning(String field, String pattern, String rawInput) {
        return new SanitizationResult(
                Status.WARNING,
                "",       // Don't use the malicious input
                pattern,
                field,
                "Suspicious input detected in your " + field + " field. " +
                "Attempting to manipulate the AI violates our terms of use. " +
                "Continued attempts may result in account suspension."
        );
    }

    public static SanitizationResult blocked(String field, String pattern) {
        return new SanitizationResult(
                Status.BLOCKED,
                "",
                pattern,
                field,
                "Your account has been flagged for repeated policy violations. " +
                "This incident has been logged. If you believe this is a mistake, " +
                "please contact support."
        );
    }

    public boolean isClean()   { return status == Status.CLEAN;   }
    public boolean isWarning() { return status == Status.WARNING; }
    public boolean isBlocked() { return status == Status.BLOCKED; }
}