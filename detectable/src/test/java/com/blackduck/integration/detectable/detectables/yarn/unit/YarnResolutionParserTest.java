package com.blackduck.integration.detectable.detectables.yarn.unit;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.yarn.YarnResolutionParser;
import com.blackduck.integration.util.NameVersion;

public class YarnResolutionParserTest {

    @Test
    void npmResolution() {
        assertIdentity("string-width", "4.2.3", "string-width@npm:4.2.3", "4.2.3");
    }

    @Test
    void npmResolutionOfAnAliasNamesTheRealPackage() {
        // the descriptor was "left-pad-alias@npm:left-pad@", the resolution names what it really is
        assertIdentity("left-pad", "1.3.0", "left-pad@npm:1.3.0", "1.3.0");
    }

    @Test
    void scopedNpmResolution() {
        assertIdentity("@scope/real", "1.0.0", "@scope/real@npm:1.0.0", "1.0.0");
    }

    @Test
    void patchResolutionNamesThePatchedPackage() {
        assertIdentity("ms", "2.1.3", "patched-ms@patch:ms@npm%3A2.1.3#../../.yarn/patches/ms-npm-2.1.3.patch::version=2.1.3&hash=fixture", "2.1.3");
    }

    @Test
    void scopedPatchResolution() {
        assertIdentity("@scope/real", "1.0.0", "alias@patch:@scope/real@npm%3A1.0.0#./p.patch::version=1.0.0&hash=abc", "1.0.0");
    }

    @Test
    void builtinPatchOfRangeUsesTheEntryVersion() {
        assertIdentity("typescript", "5.4.5", "typescript@patch:typescript@npm%3A^5.0.0#~builtin<compat/typescript>::version=5.4.5&hash=abc", "5.4.5");
    }

    @Test
    void fileResolutionHasNoRegistryIdentity() {
        assertNoIdentity("file-dep@file:../../vendor/file-dep::locator=protocol-parent%40workspace%3Apackages%2Fprotocol-parent", "1.0.0");
    }

    @Test
    void otherNonRegistryResolutionsHaveNoRegistryIdentity() {
        assertNoIdentity("protocol-parent@workspace:packages/protocol-parent", "0.0.0-use.local");
        assertNoIdentity("pkg@link:../pkg::locator=root%40workspace%3A.", "0.0.0-use.local");
        assertNoIdentity("pkg@portal:../pkg::locator=root%40workspace%3A.", "0.0.0-use.local");
        assertNoIdentity("pkg@https://example.com/pkg.tgz", "1.0.0");
        assertNoIdentity("pkg@git@github.com:org/pkg.git#commit=abc", "1.0.0");
    }

    @Test
    void patchOfANonRegistryPackageHasNoRegistryIdentity() {
        assertNoIdentity("pkg@patch:pkg@file%3A../pkg#./p.patch::version=1.0.0&hash=abc", "1.0.0");
    }

    @Test
    void missingOrMalformedInputHasNoIdentity() {
        assertNoIdentity(null, "1.0.0");
        assertNoIdentity("", "1.0.0");
        assertNoIdentity("no-at-sign", "1.0.0");
        assertNoIdentity("@scope-only", "1.0.0");
        assertNoIdentity("string-width@npm:4.2.3", null);
        assertNoIdentity("string-width@npm:4.2.3", "");
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
