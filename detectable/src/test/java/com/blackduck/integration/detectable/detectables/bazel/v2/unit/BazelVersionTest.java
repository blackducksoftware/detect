package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.bazel.v2.BazelVersion;

public class BazelVersionTest {

    // --- parse ---

    @Test
    public void parse_standardVersionString_returnsCorrectVersion() {
        Optional<BazelVersion> result = BazelVersion.parse("bazel 7.4.1");
        assertTrue(result.isPresent());
        assertEquals(new BazelVersion(7, 4, 1), result.get());
    }

    @Test
    public void parse_bazel9_returnsCorrectVersion() {
        Optional<BazelVersion> result = BazelVersion.parse("bazel 9.0.0");
        assertTrue(result.isPresent());
        assertEquals(new BazelVersion(9, 0, 0), result.get());
    }

    @Test
    public void parse_versionWithoutPatch_defaultsPatchToZero() {
        Optional<BazelVersion> result = BazelVersion.parse("bazel 7.1");
        assertTrue(result.isPresent());
        assertEquals(new BazelVersion(7, 1, 0), result.get());
    }

    @Test
    public void parse_versionWithPreReleaseSuffix_returnsVersion() {
        Optional<BazelVersion> result = BazelVersion.parse("bazel 7.1.0-rc1");
        assertTrue(result.isPresent());
        assertEquals(new BazelVersion(7, 1, 0), result.get());
    }

    @Test
    public void parse_nullInput_returnsEmpty() {
        assertFalse(BazelVersion.parse(null).isPresent());
    }

    @Test
    public void parse_emptyInput_returnsEmpty() {
        assertFalse(BazelVersion.parse("").isPresent());
    }

    @Test
    public void parse_unparsableInput_returnsEmpty() {
        assertFalse(BazelVersion.parse("not a version").isPresent());
    }

    // --- isAtLeast ---

    @Test
    public void isAtLeast_exactMatch_returnsTrue() {
        assertTrue(new BazelVersion(7, 1, 0).isAtLeast(7, 1));
    }

    @Test
    public void isAtLeast_higherMinor_returnsTrue() {
        assertTrue(new BazelVersion(7, 2, 0).isAtLeast(7, 1));
    }

    @Test
    public void isAtLeast_higherMajor_returnsTrue() {
        assertTrue(new BazelVersion(8, 0, 0).isAtLeast(7, 1));
    }

    @Test
    public void isAtLeast_lowerMinor_returnsFalse() {
        assertFalse(new BazelVersion(7, 0, 0).isAtLeast(7, 1));
    }

    @Test
    public void isAtLeast_lowerMajor_returnsFalse() {
        assertFalse(new BazelVersion(6, 5, 0).isAtLeast(7, 1));
    }

    // --- toString / equality ---

    @Test
    public void toString_returnsReadableVersion() {
        assertEquals("7.4.1", new BazelVersion(7, 4, 1).toString());
    }

    @Test
    public void equals_sameValues_areEqual() {
        assertEquals(new BazelVersion(7, 1, 0), new BazelVersion(7, 1, 0));
    }

    @Test
    public void equals_differentPatch_areNotEqual() {
        assertNotEquals(new BazelVersion(7, 1, 0), new BazelVersion(7, 1, 1));
    }
}
