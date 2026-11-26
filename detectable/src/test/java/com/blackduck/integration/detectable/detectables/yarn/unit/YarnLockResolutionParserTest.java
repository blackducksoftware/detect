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

    @Test
    void yarn1ResolvedUrlNamesTheRealPackage() {
        assertResolved("left-pad/1.3.0", "https://registry.yarnpkg.com/left-pad/-/left-pad-1.3.0.tgz#5b8a3a77", "1.3.0");
        assertResolved("@types/react-select/3.0.23", "https://registry.yarnpkg.com/@types/react-select/-/react-select-3.0.23.tgz#67fa25e3", "3.0.23");
        // registries with a path prefix
        assertResolved("@babylon/medkit-tokens/0.0.3", "https://artifactory.example.com/artifactory/api/npm/virtual-npm/@babylon/medkit-tokens/-/@babylon/medkit-tokens-0.0.3.tgz", "0.0.3");
        assertResolved("@fortawesome/pro-solid-svg-icons/5.15.1", "https://npm.fontawesome.com/@fortawesome/pro-solid-svg-icons/-/5.15.1/pro-solid-svg-icons-5.15.1.tgz", "5.15.1");
    }

    @Test
    void yarn1ResolvedWithoutATarballPathHasNoIdentity() {
        assertResolved(null, "https://github.com/org/repo/archive/abc123.tar.gz", "1.0.0");
        assertResolved(null, "https://registry.yarnpkg.com/-/pkg.tgz", "1.0.0");
        assertResolved(null, null, "1.0.0");
    }

    private void assertResolved(String expectedNameSlashVersion, String resolved, String entryVersion) {
        String actual = YarnLockResolutionParser.parseResolvedIdentity(resolved, entryVersion)
            .map(identity -> identity.getName() + "/" + identity.getVersion())
            .orElse(null);
        Assertions.assertEquals(expectedNameSlashVersion, actual);
    }

    private void assertIdentity(String expectedNameSlashVersion, String resolution, String entryVersion) {
        String actual = YarnLockResolutionParser.parseRegistryIdentity(resolution, entryVersion)
            .map(identity -> identity.getName() + "/" + identity.getVersion())
            .orElse(null);
        Assertions.assertEquals(expectedNameSlashVersion, actual);
    }
}
