package com.blackduck.integration.detectable.detectables.bun.lockfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunDependencyType;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockfileData;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunPackage;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.DirectDependency;

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
    void parsesDirectDependenciesWithResolvedVersions() throws Exception {
        BunLockfileData data = parser().parseBunLock(testLockFile());
        List<DirectDependency> directDependencies = data.getDirectDependencies();

        assertEquals(4, directDependencies.size(), "express + async + grunt + grunt-concurrent");
        assertTrue(directDependencies.stream()
            .anyMatch(d -> "express".equals(d.getName()) && "4.21.2".equals(d.getVersion()) && d.getType() == BunDependencyType.NORMAL),
            "express should be a normal direct dependency with resolved version");
        assertTrue(directDependencies.stream()
            .anyMatch(d -> "grunt".equals(d.getName()) && "1.6.3".equals(d.getVersion()) && d.getType() == BunDependencyType.DEV),
            "grunt should be a dev direct dependency with resolved version");
    }

    @Test
    void parsesAllPackageEntries() throws Exception {
        BunLockfileData data = parser().parseBunLock(testLockFile());
        // 7 flat + 2 path-qualified (grunt-concurrent/async, grunt-legacy-util/async) = 9 entries
        assertEquals(9, data.getPackages().size());
    }

    @Test
    void flatPackageHasCorrectKeyAndVersion() throws Exception {
        BunLockfileData data = parser().parseBunLock(testLockFile());
        BunPackage pkg = findByKey(data, "async");
        assertEquals("async", pkg.getName());
        assertEquals("2.6.4", pkg.getVersion());
    }

    @Test
    void pathQualifiedPackageHasCorrectKeyAndVersion() throws Exception {
        BunLockfileData data = parser().parseBunLock(testLockFile());
        BunPackage pkg = findByKey(data, "grunt-concurrent/async");
        assertEquals("async", pkg.getName());
        assertEquals("1.5.2", pkg.getVersion());
    }

    @Test
    void secondPathQualifiedPackageHasCorrectKeyAndVersion() throws Exception {
        BunLockfileData data = parser().parseBunLock(testLockFile());
        BunPackage pkg = findByKey(data, "grunt-legacy-util/async");
        assertEquals("async", pkg.getName());
        assertEquals("3.2.6", pkg.getVersion());
    }

    @Test
    void packageDependenciesAreParsed() throws Exception {
        BunLockfileData data = parser().parseBunLock(testLockFile());
        BunPackage async264 = findByKey(data, "async");
        assertTrue(async264.getDependencies().stream().anyMatch(d -> "lodash".equals(d.getName())),
            "async@2.6.4 should have lodash as a dependency");
    }

    private BunPackage findByKey(BunLockfileData data, String key) {
        return data.getPackages().stream()
            .filter(p -> key.equals(p.getKey()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Package not found with key: " + key));
    }
}
