package com.blackduck.integration.detectable.detectables.bun.lockfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunDependencyType;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockfileData;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunPackage;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.DirectDependency;
import com.blackduck.integration.detectable.util.DependencyCreator;

class BunLockJsonParserTest {

    private BunLockJsonParser parser() {
        return new BunLockJsonParser();
    }

    private File testLockFile() throws Exception {
        return lockFile("/detectables/functional/bun/lockfile/bun.lock");
    }

    private File monorepoLockFile() throws Exception {
        return lockFile("/detectables/functional/bun/lockfile/monorepo/bun.lock");
    }

    private File lockFile(String resourcePath) throws Exception {
        URL resource = BunLockJsonParserTest.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Test bun.lock resource not found: " + resourcePath);
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

    @Test
    void subWorkspaceDirectDependencyResolvesToItsWorkspaceQualifiedVersion() throws Exception {
        List<DirectDependency> directs = parser().parseBunLock(monorepoLockFile()).getDirectDependencies();

        assertEquals(1, directs.stream().filter(d -> "vite".equals(d.getName()) && "8.2.2".equals(d.getVersion())).count(),
            "root workspace keeps the flat/hoisted vite");
        assertEquals(1, directs.stream().filter(d -> "vite".equals(d.getName()) && "7.3.6".equals(d.getVersion())).count(),
            "@acme/ui pins vite 7.3.6 via the '@acme/ui/vite' key, not the hoisted 8.2.2");
    }

    @Test
    void subWorkspaceDependencyWithNoFlatKeyIsNotDropped() throws Exception {
        List<DirectDependency> directs = parser().parseBunLock(monorepoLockFile()).getDirectDependencies();

        assertTrue(directs.stream().anyMatch(d -> "only-nested".equals(d.getName()) && "1.0.0".equals(d.getVersion())),
            "only-nested exists solely as '@acme/ui/only-nested'");
    }

    @Test
    void workspaceNameDeclaredAfterDependencyMapsIsStillApplied() throws Exception {
        List<DirectDependency> directs = parser().parseBunLock(monorepoLockFile()).getDirectDependencies();

        assertTrue(directs.stream().anyMatch(d -> "lodash".equals(d.getName()) && "4.17.21".equals(d.getVersion())));
        // 2 vite + only-nested + lodash; the workspace: reference to @acme/ui is skipped
        assertEquals(4, directs.size());
    }

    @Test
    void monorepoGraphContainsBothViteVersionsWithTheirOwnChildren() throws Exception {
        BunLockfileData data = parser().parseBunLock(monorepoLockFile());
        DependencyGraph graph = new BunLockfileTransformer().generateCodeLocations(data).get(0).getDependencyGraph();

        Dependency vite7 = DependencyCreator.nameVersion(Forge.NPMJS, "vite", "7.3.6");
        Dependency vite8 = DependencyCreator.nameVersion(Forge.NPMJS, "vite", "8.2.2");
        assertTrue(graph.getRootDependencies().contains(vite7));
        assertTrue(graph.getRootDependencies().contains(vite8));
        assertEquals(1, graph.getChildrenForParent(vite7).size());
        assertEquals("esbuild", graph.getChildrenForParent(vite7).iterator().next().getExternalId().getName());
        assertEquals("rollup", graph.getChildrenForParent(vite8).iterator().next().getExternalId().getName());
    }

    private BunPackage findByKey(BunLockfileData data, String key) {
        return data.getPackages().stream()
            .filter(p -> key.equals(p.getKey()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Package not found with key: " + key));
    }
}
