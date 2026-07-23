package com.blackduck.integration.detectable.detectables.pnpm.functional;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.PnpmLockOptions;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmDependencyType;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLinkedPackageResolver;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLockYamlParserInitial;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonReader;
import com.blackduck.integration.detectable.util.FunctionalTestFiles;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;

/**
 * Tests for pnpm 11 multi-document YAML lockfile support.
 *
 * pnpm 11 generates lockfiles with two YAML documents separated by ---:
 * Document 1 contains environment metadata, Document 2 contains the dependency graph.
 * These tests verify that Detect correctly parses the dependency data from Document 2
 * and that single-document lockfiles (pre-v11) continue to work as before.
 */
public class PnpmLockYamlParserMultiDocTest {

    private List<CodeLocation> parseLockFile(String resourcePath) throws IOException, IntegrationException {
        File pnpmLockYaml = FunctionalTestFiles.asFile(resourcePath);

        EnumListFilter<PnpmDependencyType> dependencyTypeFilter = EnumListFilter.excludeNone();
        PnpmLockOptions pnpmLockOptions = new PnpmLockOptions(dependencyTypeFilter, Collections.emptyList(), Collections.emptyList());

        PnpmLockYamlParserInitial parser = new PnpmLockYamlParserInitial(pnpmLockOptions);
        PnpmLinkedPackageResolver linkedPackageResolver = new PnpmLinkedPackageResolver(
            pnpmLockYaml.getParentFile(),
            new PackageJsonFiles(new PackageJsonReader(new Gson()))
        );

        return parser.parse(pnpmLockYaml, new NameVersion("project", "1.0.0"), linkedPackageResolver);
    }

    /**
     * Real-world pnpm 11 lockfile: two YAML documents separated by ---.
     * Document 1 has configDependencies/packageManagerDependencies (metadata).
     * Document 2 has lockfileVersion, importers, packages, snapshots (dependency graph).
     * Verifies that direct and transitive dependencies are extracted from Document 2.
     */
    @Test
    public void testParseV11MultiDocLockfile() throws IOException, IntegrationException {
        List<CodeLocation> codeLocations = parseLockFile("/pnpm/v11-multi-doc/pnpm-lock.yaml");

        Assertions.assertNotNull(codeLocations);
        Assertions.assertEquals(1, codeLocations.size(), "Single-importer lockfile should produce one code location");

        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();

        Set<String> rootDepNames = graph.getRootDependencies().stream()
            .map(Dependency::getName)
            .collect(Collectors.toSet());

        Assertions.assertTrue(rootDepNames.contains("express"),
            "Root dependencies should include express. Found: " + rootDepNames);

        // Verify transitive dependencies from snapshots section
        Set<String> expressChildren = graph.getRootDependencies().stream()
            .filter(dep -> "express".equals(dep.getName()))
            .flatMap(dep -> graph.getChildrenForParent(dep).stream())
            .map(Dependency::getName)
            .collect(Collectors.toSet());

        Assertions.assertTrue(expressChildren.contains("accepts"),
            "express should have transitive dependency accepts. Found: " + expressChildren);
        Assertions.assertTrue(expressChildren.contains("array-flatten"),
            "express should have transitive dependency array-flatten. Found: " + expressChildren);
    }

    /**
     * Real-world pnpm 11 workspace/monorepo lockfile: two YAML documents, with
     * multiple importers (root "." and "packages/utils").
     * Verifies that each workspace gets its own code location with correct dependencies.
     */
    @Test
    public void testParseV11MultiDocWorkspaceLockfile() throws IOException, IntegrationException {
        List<CodeLocation> codeLocations = parseLockFile("/pnpm/v11-multi-doc-workspace/pnpm-lock.yaml");

        Assertions.assertNotNull(codeLocations);
        Assertions.assertEquals(2, codeLocations.size(),
            "Workspace lockfile with 2 importers should produce 2 code locations");

        // Verify root project code location exists and has correct deps
        Optional<CodeLocation> rootLocation = codeLocations.stream()
            .filter(cl -> cl.getExternalId()
                .map(id -> "project".equals(id.getName()))
                .orElse(false))
            .findFirst();
        Assertions.assertTrue(rootLocation.isPresent(), "Should have a code location for the root project");

        Set<String> rootDeps = rootLocation.get().getDependencyGraph().getRootDependencies().stream()
            .map(Dependency::getName)
            .collect(Collectors.toSet());
        Assertions.assertTrue(rootDeps.contains("lodash"), "Root should depend on lodash. Found: " + rootDeps);
        Assertions.assertTrue(rootDeps.contains("typescript"), "Root should depend on typescript (dev). Found: " + rootDeps);

        // Verify packages/utils code location exists and has correct deps
        Optional<CodeLocation> utilsLocation = codeLocations.stream()
            .filter(cl -> cl.getExternalId()
                .map(id -> "packages/utils".equals(id.getName()))
                .orElse(false))
            .findFirst();
        Assertions.assertTrue(utilsLocation.isPresent(), "Should have a code location for packages/utils");

        Set<String> utilsDeps = utilsLocation.get().getDependencyGraph().getRootDependencies().stream()
            .map(Dependency::getName)
            .collect(Collectors.toSet());
        Assertions.assertTrue(utilsDeps.contains("ms"), "packages/utils should depend on ms. Found: " + utilsDeps);
    }

    /**
     * Regression test for the fallback path in selectLockfileDocument().
     *
     * Uses a single-document v5-style lockfile that has real dependency content
     * but NO lockfileVersion field — simulating older pnpm lockfiles where
     * lockfileVersion was not mandatory.
     *
     * Flow:
     *   1. loadAll() yields one document with lockfileVersion == null.
     *   2. selectLockfileDocument() finds no document with lockfileVersion
     *      → takes the fallback path → returns firstNonNull (the single doc).
     *   3. isV6OrNewer(null) == false → falls through to v5 re-parse.
     *   4. v5 parser reads the real importers + packages and returns a code location.
     *   5. Assertions verify specific named deps are present in the graph.
     *
     * If the fallback were broken (returned null instead of firstNonNull),
     * parse() would return an empty list and the dependency assertions below would fail.
     */
    @Test
    public void testSelectLockfileDocumentFallback_noLockfileVersionPresent() throws IOException, IntegrationException {
        List<CodeLocation> codeLocations = parseLockFile("/pnpm/v5-no-lockfileversion/pnpm-lock.yaml");

        Assertions.assertNotNull(codeLocations, "Parser must not return null when fallback path is used");
        Assertions.assertEquals(1, codeLocations.size(),
            "Should produce 1 code location for the root importer");

        DependencyGraph graph = codeLocations.get(0).getDependencyGraph();
        Set<String> rootDepNames = graph.getRootDependencies().stream()
            .map(Dependency::getName)
            .collect(Collectors.toSet());

        Assertions.assertTrue(rootDepNames.contains("ms"),
            "Root dependencies should include ms (direct dep). Found: " + rootDepNames);
        Assertions.assertTrue(rootDepNames.contains("chalk"),
            "Root dependencies should include chalk (dev dep). Found: " + rootDepNames);
    }

}

