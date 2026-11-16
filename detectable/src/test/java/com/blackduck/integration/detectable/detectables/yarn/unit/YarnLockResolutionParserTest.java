package com.blackduck.integration.detectable.detectables.yarn.unit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockResolutionParser;

public class YarnLockResolutionParserTest {

    @Test
    void npmResolutionNamesTheRealPackage() {
        assertIdentity("string-width/4.2.3", "string-width@npm:4.2.3", "4.2.3");
        assertIdentity("@scope/real/1.0.0", "@scope/real@npm:1.0.0", "1.0.0");
    }

    @Test
    void patchResolutionNamesThePatchedPackage() {
        assertIdentity("ms/2.1.3", "patched-ms@patch:ms@npm%3A2.1.3#../../.yarn/patches/ms-npm-2.1.3.patch::version=2.1.3&hash=fixture", "2.1.3");
    }

    @Test
    void nonRegistryOrMalformedResolutionsHaveNoIdentity() {
        assertIdentity(null, "file-dep@file:../../vendor/file-dep::locator=root%40workspace%3A.", "1.0.0");
        assertIdentity(null, "protocol-parent@workspace:packages/protocol-parent", "0.0.0-use.local");
        assertIdentity(null, "pkg@patch:pkg@file%3A../pkg#./p.patch::version=1.0.0&hash=abc", "1.0.0");
        assertIdentity(null, "no-at-sign", "1.0.0");
        assertIdentity(null, null, "1.0.0");
    }

    private void assertIdentity(String expectedNameSlashVersion, String resolution, String entryVersion) {
        String actual = YarnLockResolutionParser.parseRegistryIdentity(resolution, entryVersion)
            .map(identity -> identity.getName() + "/" + identity.getVersion())
            .orElse(null);
        Assertions.assertEquals(expectedNameSlashVersion, actual);
    }
}
