package com.blackduck.integration.detectable.detectables.yarn.unit;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.yarn.YarnResolutionParser;
import com.blackduck.integration.util.NameVersion;

public class YarnResolutionParserTest {

    @Test
    void npmResolutionNamesTheRealPackage() {
        assertIdentity("string-width", "4.2.3", "string-width@npm:4.2.3", "4.2.3");
        assertIdentity("@scope/real", "1.0.0", "@scope/real@npm:1.0.0", "1.0.0");
    }

    @Test
    void patchResolutionNamesThePatchedPackage() {
        assertIdentity("ms", "2.1.3", "patched-ms@patch:ms@npm%3A2.1.3#../../.yarn/patches/ms-npm-2.1.3.patch::version=2.1.3&hash=fixture", "2.1.3");
    }

    @Test
    void nonRegistryResolutionsHaveNoIdentity() {
        assertNoIdentity("file-dep@file:../../vendor/file-dep::locator=root%40workspace%3A.", "1.0.0");
        assertNoIdentity("protocol-parent@workspace:packages/protocol-parent", "0.0.0-use.local");
        assertNoIdentity("pkg@patch:pkg@file%3A../pkg#./p.patch::version=1.0.0&hash=abc", "1.0.0");
    }

    @Test
    void missingOrMalformedInputHasNoIdentity() {
        assertNoIdentity(null, "1.0.0");
        assertNoIdentity("no-at-sign", "1.0.0");
        assertNoIdentity("string-width@npm:4.2.3", null);
    }

    private void assertIdentity(String expectedName, String expectedVersion, String resolution, String entryVersion) {
        Optional<NameVersion> identity = YarnResolutionParser.parseRegistryIdentity(resolution, entryVersion);
        Assertions.assertTrue(identity.isPresent(), "Expected an identity for " + resolution);
        Assertions.assertEquals(expectedName, identity.get().getName());
        Assertions.assertEquals(expectedVersion, identity.get().getVersion());
    }

    private void assertNoIdentity(String resolution, String entryVersion) {
        Assertions.assertFalse(YarnResolutionParser.parseRegistryIdentity(resolution, entryVersion).isPresent(), "Expected no identity for " + resolution);
    }
}
