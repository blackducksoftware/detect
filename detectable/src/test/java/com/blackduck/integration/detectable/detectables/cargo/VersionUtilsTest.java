package com.blackduck.integration.detectable.detectables.cargo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionUtilsTest {

    // Caret (^) tests — the exact scenario from IDETECT-5056
    @Test
    void caretVersionMatchesCompatible() {
        assertTrue(VersionUtils.versionMatches("^6.0", "6.0.0"));
        assertTrue(VersionUtils.versionMatches("^6.0", "6.1.0"));
        assertTrue(VersionUtils.versionMatches("^6.0", "6.0.5"));
    }

    @Test
    void caretVersionRejectsIncompatibleMajor() {
        assertFalse(VersionUtils.versionMatches("^6.0", "7.0.0"));
        assertFalse(VersionUtils.versionMatches("^6.0", "5.9.9"));
    }

    @Test
    void caretVersionWithFullSemver() {
        assertTrue(VersionUtils.versionMatches("^1.2.3", "1.2.3"));
        assertTrue(VersionUtils.versionMatches("^1.2.3", "1.3.0"));
        assertFalse(VersionUtils.versionMatches("^1.2.3", "1.2.2"));
        assertFalse(VersionUtils.versionMatches("^1.2.3", "2.0.0"));
    }

    // Tilde (~) tests
    @Test
    void tildeVersionMatchesSameMinor() {
        assertTrue(VersionUtils.versionMatches("~1.2.3", "1.2.3"));
        assertTrue(VersionUtils.versionMatches("~1.2.3", "1.2.5"));
    }

    @Test
    void tildeVersionRejectsDifferentMinor() {
        assertFalse(VersionUtils.versionMatches("~1.2.3", "1.3.0"));
        assertFalse(VersionUtils.versionMatches("~1.2.3", "2.0.0"));
    }

    @Test
    void tildeVersionWithMajorMinorOnly() {
        assertTrue(VersionUtils.versionMatches("~1.2", "1.2.0"));
        assertTrue(VersionUtils.versionMatches("~1.2", "1.2.9"));
        assertFalse(VersionUtils.versionMatches("~1.2", "1.3.0"));
    }

    // Bare version (no prefix) — existing behavior should still work
    @Test
    void bareVersionMatchesCompatible() {
        assertTrue(VersionUtils.versionMatches("1.0", "1.0.0"));
        assertTrue(VersionUtils.versionMatches("1.0", "1.5.0"));
        assertFalse(VersionUtils.versionMatches("1.0", "2.0.0"));
    }

    // Comparison operators — existing behavior
    @Test
    void greaterThanOrEqualOperator() {
        assertTrue(VersionUtils.versionMatches(">=1.0.0", "1.0.0"));
        assertTrue(VersionUtils.versionMatches(">=1.0.0", "2.0.0"));
        assertFalse(VersionUtils.versionMatches(">=1.0.0", "0.9.0"));
    }

    @Test
    void lessThanOperator() {
        assertTrue(VersionUtils.versionMatches("<2.0.0", "1.9.9"));
        assertFalse(VersionUtils.versionMatches("<2.0.0", "2.0.0"));
    }

    @Test
    void exactEqualOperator() {
        assertTrue(VersionUtils.versionMatches("=1.0.0", "1.0.0"));
        assertFalse(VersionUtils.versionMatches("=1.0.0", "1.0.1"));
    }

    @Test
    void wildcardMatchesAll() {
        assertTrue(VersionUtils.versionMatches("*", "1.0.0"));
        assertTrue(VersionUtils.versionMatches("*", "99.99.99"));
    }

    @Test
    void nullInputsReturnFalse() {
        assertFalse(VersionUtils.versionMatches(null, "1.0.0"));
        assertFalse(VersionUtils.versionMatches("1.0", null));
    }
}
