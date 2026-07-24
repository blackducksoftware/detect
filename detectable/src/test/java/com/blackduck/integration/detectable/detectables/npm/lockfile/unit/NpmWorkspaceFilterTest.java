package com.blackduck.integration.detectable.detectables.npm.lockfile.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.npm.lockfile.NpmLockfileOptions;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfileGraphTransformer;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfilePackager;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockFileProjectIdTransformer;
import com.blackduck.integration.detectable.detectables.npm.lockfile.result.NpmPackagerResult;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.npm.NpmDependencyType;
import com.blackduck.integration.detectable.util.FunctionalTestFiles;
import com.blackduck.integration.detectable.util.graph.GraphAssert;
import com.blackduck.integration.util.ExcludedIncludedWildcardFilter;

public class NpmWorkspaceFilterTest {

    // Fixture files live under src/test/resources/detectables/functional/npm/workspace-filter-test/
    // The fixture has two workspaces:
    //   packages/api  (package name "my-api") — has express as a dependency
    //   packages/ui   (package name "my-ui")  — has lodash as a dependency
    // The root package.json declares express as a direct dependency.
    private static final String FIXTURE_PATH = "/npm/workspace-filter-test/package.json";
    private static final String PACKAGE_JSON = FunctionalTestFiles.asString(FIXTURE_PATH);
    private static final String LOCKFILE = FunctionalTestFiles.asString("/npm/workspace-filter-test/package-lock.json");

    @Test
    public void excludedWorkspaceIsNotTreatedAsWorkspace() throws IOException {
        NpmLockfileOptions options = new NpmLockfileOptions(
            EnumListFilter.excludeNone(),
            Arrays.asList("packages/ui"),   // exclude by relative path
            Collections.emptyList()
        );
        NpmPackagerResult result = buildResult(options);
        // lodash is only brought in as a workspace dep of my-ui.
        // When my-ui is excluded as a workspace, lodash should not be promoted to root.
        // express (from my-api workspace) should still appear as a root dep.
        DependencyGraph graph = result.getCodeLocation().getDependencyGraph();
        ExternalId expressId = new ExternalIdFactory().createNameVersionExternalId(Forge.NPMJS, "express", "4.18.0");
        ExternalId lodashId = new ExternalIdFactory().createNameVersionExternalId(Forge.NPMJS, "lodash", "4.17.21");

        boolean expressAtRoot = graph.getRootDependencies().stream()
            .map(Dependency::getExternalId)
            .anyMatch(expressId::equals);
        boolean lodashAtRoot = graph.getRootDependencies().stream()
            .map(Dependency::getExternalId)
            .anyMatch(lodashId::equals);

        assertTrue(expressAtRoot, "express should be a root dependency (promoted from my-api workspace)");
        assertFalse(lodashAtRoot, "lodash should NOT be a root dependency when my-ui workspace is excluded");
    }

    @Test
    public void includedWorkspaceOnlyIncludesThatWorkspace() throws IOException {
        NpmLockfileOptions options = new NpmLockfileOptions(
            EnumListFilter.excludeNone(),
            Collections.emptyList(),
            Arrays.asList("packages/api")   // only include by relative path
        );
        NpmPackagerResult result = buildResult(options);
        DependencyGraph graph = result.getCodeLocation().getDependencyGraph();
        ExternalId expressId = new ExternalIdFactory().createNameVersionExternalId(Forge.NPMJS, "express", "4.18.0");
        ExternalId lodashId = new ExternalIdFactory().createNameVersionExternalId(Forge.NPMJS, "lodash", "4.17.21");

        boolean expressAtRoot = graph.getRootDependencies().stream()
            .map(Dependency::getExternalId)
            .anyMatch(expressId::equals);
        boolean lodashAtRoot = graph.getRootDependencies().stream()
            .map(Dependency::getExternalId)
            .anyMatch(lodashId::equals);

        assertTrue(expressAtRoot, "express should be a root dependency (promoted from my-api workspace)");
        assertFalse(lodashAtRoot, "lodash should NOT be a root dependency when only my-api workspace is included");
    }

    @Test
    public void ignoreAllWorkspacesExcludesAllWorkspaceDeps() throws IOException {
        NpmLockfileOptions options = new NpmLockfileOptions(
            EnumListFilter.excludeNone(),
            Collections.emptyList(),
            Collections.emptyList(),
            true
        );
        NpmPackagerResult result = buildResult(options);
        DependencyGraph graph = result.getCodeLocation().getDependencyGraph();
        ExternalId expressId = new ExternalIdFactory().createNameVersionExternalId(Forge.NPMJS, "express", "4.18.0");
        ExternalId lodashId = new ExternalIdFactory().createNameVersionExternalId(Forge.NPMJS, "lodash", "4.17.21");

        boolean expressAtRoot = graph.getRootDependencies().stream()
            .map(Dependency::getExternalId)
            .anyMatch(expressId::equals);
        boolean lodashAtRoot = graph.getRootDependencies().stream()
            .map(Dependency::getExternalId)
            .anyMatch(lodashId::equals);

        assertTrue(expressAtRoot, "express should be a root dependency (declared in root package.json)");
        assertFalse(lodashAtRoot, "lodash should NOT be a root dependency when all workspaces are ignored");
    }

    private NpmPackagerResult buildResult(NpmLockfileOptions options) throws IOException {
        String rootJsonPath = FunctionalTestFiles.resolvePath(FIXTURE_PATH);
        Gson gson = new Gson();
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        NpmLockfileGraphTransformer transformer =
            new NpmLockfileGraphTransformer(options.getNpmDependencyTypeFilter());
        ExcludedIncludedWildcardFilter workspaceFilter = options.isIgnoreAllWorkspaces()
            ? ExcludedIncludedWildcardFilter.fromCollections(Collections.singletonList("*"), Collections.emptyList())
            : ExcludedIncludedWildcardFilter.fromCollections(options.getExcludedWorkspaceNames(), options.getIncludedWorkspaceNames());
        NpmLockfilePackager packager =
            new NpmLockfilePackager(gson, externalIdFactory,
                new NpmLockFileProjectIdTransformer(gson, externalIdFactory), transformer, workspaceFilter);
        return packager.parseAndTransform(rootJsonPath, PACKAGE_JSON, LOCKFILE);
    }
}
