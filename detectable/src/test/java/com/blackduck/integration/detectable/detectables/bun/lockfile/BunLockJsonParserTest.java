package com.blackduck.integration.detectable.detectables.bun.lockfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockPackage;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockResult;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockfileData;

class BunLockJsonParserTest {

    private BunLockJsonParser parser() {
        return new BunLockJsonParser();
    }

    private File testLockFile() throws Exception {
        URL resource = BunLockJsonParserTest.class.getResource("/detectables/functional/bun/lockfile/bun.lock");
        if (resource == null) {
            throw new IllegalStateException("Test bun.lock resource not found");
        }
        return new File(resource.toURI());
    }

    @Test
    void parsesCorrectPackageCount() throws Exception {
        BunLockResult result = parser().parseBunLock(testLockFile());
        // 7 flat + 2 path-qualified (async@1.5.2, async@3.2.6) = 9 unique (name, version) pairs
        assertEquals(9, result.getData().getPackages().size());
    }

    @Test
    void assignsWorkspaceRangeToFlatEntry() throws Exception {
        BunLockfileData data = parser().parseBunLock(testLockFile()).getData();
        Map<String, String> gruntVersions = data.getRangeToVersion().get("grunt");
        // workspace declares grunt@^1.0.3 → must map to 1.6.3
        assertTrue(gruntVersions != null && "1.6.3".equals(gruntVersions.get("^1.0.3")),
            "Expected ^1.0.3 → 1.6.3 in grunt rangeToVersion");
    }

    @Test
    void assignsPathQualifiedRangeToNestedVersion() throws Exception {
        BunLockfileData data = parser().parseBunLock(testLockFile()).getData();
        Map<String, String> asyncVersions = data.getRangeToVersion().get("async");

        // grunt-concurrent depends on async@^1.2.1; grunt-concurrent/async resolves to async@1.5.2
        assertEquals("1.5.2", asyncVersions.get("^1.2.1"), "^1.2.1 should map to async@1.5.2");

        // grunt-legacy-util depends on async@~3.2.0; grunt-legacy-util/async resolves to async@3.2.6
        assertEquals("3.2.6", asyncVersions.get("~3.2.0"), "~3.2.0 should map to async@3.2.6");

        // workspace declares async@^2.0.0-rc.4; top-level async resolves to 2.6.4
        assertEquals("2.6.4", asyncVersions.get("^2.0.0-rc.4"), "^2.0.0-rc.4 should map to async@2.6.4");
    }

    @Test
    void doesNotCrossContaminateRangesAcrossVersions() throws Exception {
        BunLockfileData data = parser().parseBunLock(testLockFile()).getData();
        Map<String, String> asyncVersions = data.getRangeToVersion().get("async");

        // Each range must map to exactly the right version, not bleed into others
        assertFalse("1.5.2".equals(asyncVersions.get("^2.0.0-rc.4")), "^2.0.0-rc.4 must not map to 1.5.2");
        assertFalse("2.6.4".equals(asyncVersions.get("^1.2.1")), "^1.2.1 must not map to 2.6.4");
        assertFalse("3.2.6".equals(asyncVersions.get("^1.2.1")), "^1.2.1 must not map to 3.2.6");
    }

    @Test
    void fallsBackToFlatEntryWhenNoPathQualifiedKeyExists() throws Exception {
        BunLockfileData data = parser().parseBunLock(testLockFile()).getData();
        // lodash is a dep of async@2.6.4 with range ^4.17.14; no path-qualified lodash entry exists
        Map<String, String> lodashVersions = data.getRangeToVersion().get("lodash");
        assertEquals("4.17.21", lodashVersions.get("^4.17.14"), "^4.17.14 should fall back to lodash@4.17.21");
    }

    @Test
    void mergesDepsFromMultipleKeysForSameVersion() throws Exception {
        BunLockResult result = parser().parseBunLock(testLockFile());
        BunLockPackage async264 = findPackage(result, "async", "2.6.4");
        // async@2.6.4 has lodash as a dep
        assertTrue(async264.getDependencies().stream().anyMatch(d -> "lodash".equals(d.getName())),
            "async@2.6.4 should have lodash dep");
    }

    private BunLockPackage findPackage(BunLockResult result, String name, String version) {
        return result.getData().getPackages().stream()
            .filter(p -> name.equals(p.getName()) && version.equals(p.getVersion()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Package not found: " + name + "@" + version));
    }
}
