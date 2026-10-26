package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.bazel.v2.BazelLabel;

class BazelLabelTest {

    @Test
    void canonicalLabelWithSuffixAndPath() {
        BazelLabel label = BazelLabel.parse("@@abseil-cpp~//absl:strings");
        assertTrue(label.isRepoLabel());
        assertTrue(label.isCanonical());
        assertTrue(label.hasPath());
        assertEquals("abseil-cpp~", label.getRepoName());
        assertFalse(label.isModuleExtensionSubRepo());
    }

    @Test
    void apparentLabelWithPath() {
        BazelLabel label = BazelLabel.parse("@com_google_protobuf//google/protobuf:lib");
        assertTrue(label.isRepoLabel());
        assertFalse(label.isCanonical());
        assertTrue(label.hasPath());
        assertEquals("com_google_protobuf", label.getRepoName());
    }

    @Test
    void canonicalLabelWithoutPath() {
        BazelLabel label = BazelLabel.parse("@@abseil-cpp~");
        assertTrue(label.isRepoLabel());
        assertTrue(label.isCanonical());
        assertFalse(label.hasPath());
        assertEquals("abseil-cpp~", label.getRepoName());
    }

    @Test
    void moduleExtensionSubRepoDetected() {
        BazelLabel label = BazelLabel.parse("@@rules_jvm_external++maven+guava//:guava");
        assertTrue(label.isCanonical());
        assertTrue(label.isModuleExtensionSubRepo());
        assertEquals("rules_jvm_external++maven+guava", label.getRepoName());
    }

    @Test
    void plusSuffixPreserved() {
        BazelLabel label = BazelLabel.parse("@@protobuf+//:protobuf");
        assertEquals("protobuf+", label.getRepoName());
    }

    @Test
    void localTargetIsNotRepoLabel() {
        BazelLabel label = BazelLabel.parse("//src/main:app");
        assertFalse(label.isRepoLabel());
        assertEquals("", label.getRepoName());
        assertTrue(label.hasPath());
    }

    @Test
    void nullTolerated() {
        BazelLabel label = BazelLabel.parse(null);
        assertFalse(label.isRepoLabel());
        assertEquals("", label.getRepoName());
        assertEquals("", label.getRaw());
    }

    @Test
    void emptyTolerated() {
        BazelLabel label = BazelLabel.parse("");
        assertFalse(label.isRepoLabel());
        assertEquals("", label.getRepoName());
    }

    @Test
    void apparentPrefixWithImmediatePathYieldsEmptyRepoName() {
        // mirrors historical resolveLabel: "@//foo:bar" -> apparent repo name ""
        BazelLabel label = BazelLabel.parse("@//foo:bar");
        assertTrue(label.isRepoLabel());
        assertFalse(label.isCanonical());
        assertTrue(label.hasPath());
        assertEquals("", label.getRepoName());
    }
}


