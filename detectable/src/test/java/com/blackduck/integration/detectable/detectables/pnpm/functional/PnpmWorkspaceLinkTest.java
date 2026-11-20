package com.blackduck.integration.detectable.detectables.pnpm.functional;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.PnpmLockOptions;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmDependencyType;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLinkedPackageResolver;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLockYamlParserInitial;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmWorkspaceDependencySummary;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonReader;
import com.blackduck.integration.detectable.util.FunctionalTestFiles;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;

public class PnpmWorkspaceLinkTest {
    private static final String WORKSPACE_ROOT = "/pnpm/v9-workspace-links";
    private static final String LOCKFILE = WORKSPACE_ROOT + "/pnpm-lock.yaml";
    private static final String NO_VERSION = "";

    @Test
    public void linkedWorkspacesAreDirectDependenciesOfDeclaringModule() throws Exception {
        NameVersionGraphAssert nextjs = nextjsGraph(EnumListFilter.excludeNone());

        nextjs.hasRootDependency("@acme/api", NO_VERSION);
        nextjs.hasRootDependency("@acme/validators", NO_VERSION);
        nextjs.hasRootDependency("@acme/tsconfig", "0.1.0");
        nextjs.hasRootDependency("@tanstack/react-query", "5.90.8");
        nextjs.hasRootDependency("@trpc/client", "11.7.1");
        nextjs.hasRootDependency("@trpc/server", "11.7.1");
        nextjs.hasRootDependency("superjson", "2.2.3");
        nextjs.hasRootSize(7);
    }

    @Test
    public void crossModuleChainIsConnected() throws Exception {
        NameVersionGraphAssert nextjs = nextjsGraph(EnumListFilter.excludeNone());

        nextjs.hasParentChildRelationship("@acme/api", NO_VERSION, "zod", "4.1.12");
        // sibling links: link:../auth and link:../db from packages/api, link:../db from packages/auth
        nextjs.hasParentChildRelationship("@acme/api", NO_VERSION, "@acme/auth", NO_VERSION);
        nextjs.hasParentChildRelationship("@acme/auth", NO_VERSION, "@acme/db", NO_VERSION);
        nextjs.hasParentChildRelationship("@acme/auth", NO_VERSION, "@t3-oss/env-core", "0.13.8");
        nextjs.hasParentChildRelationship("@acme/db", NO_VERSION, "drizzle-orm", "0.44.7");
        // the registry subtree under a linked module comes from the snapshots section
        nextjs.hasParentChildRelationship("drizzle-zod", "0.8.3", "drizzle-orm", "0.44.7");
    }

    @Test
    public void workspaceReachedThroughTwoPathsIsExpandedOnce() throws Exception {
        NameVersionGraphAssert nextjs = nextjsGraph(EnumListFilter.excludeNone());

        // @acme/db is linked from both packages/api and packages/auth; it is one node with both parents
        nextjs.hasParentChildRelationship("@acme/api", NO_VERSION, "@acme/db", NO_VERSION);
        nextjs.hasParentChildRelationship("@acme/auth", NO_VERSION, "@acme/db", NO_VERSION);
        nextjs.hasRelationshipCount(ExternalId.FACTORY.createNameVersionExternalId(Forge.NPMJS, "@acme/db", NO_VERSION), 3);
    }

    @Test
    public void devDependenciesOfLinkedModuleAreNotFollowed() throws Exception {
        NameVersionGraphAssert nextjs = nextjsGraph(EnumListFilter.excludeNone());

        // packages/api has devDependencies on @acme/tsconfig and typescript, which are not part of what @acme/api ships:
        // its children are @acme/auth, @acme/db, @acme/validators, @trpc/server, superjson and zod
        nextjs.hasRelationshipCount(ExternalId.FACTORY.createNameVersionExternalId(Forge.NPMJS, "@acme/api", NO_VERSION), 6);
    }

    @Test
    public void devDependencyLinkOfReportingModuleRespectsDependencyTypeFilter() throws Exception {
        NameVersionGraphAssert nextjs = nextjsGraph(EnumListFilter.fromExcluded(PnpmDependencyType.DEV));

        nextjs.hasRootDependency("@acme/api", NO_VERSION);
        nextjs.hasNoDependency("@acme/tsconfig", "0.1.0");
        nextjs.hasRootSize(6);
    }

    // --detect.pnpm.included.packages=apps/nextjs
    @Test
    public void includedPackagesAppsNextjsEmitsOnlyTheAppsNextjsCodeLocation() throws Exception {
        List<CodeLocation> codeLocations = parse(EnumListFilter.excludeNone(), Collections.singletonList("apps/nextjs"));

        Assertions.assertEquals(1, codeLocations.size());
        Assertions.assertEquals("apps/nextjs", codeLocations.get(0).getExternalId().map(ExternalId::getName).orElse(null));
    }

    // --detect.pnpm.included.packages=apps/nextjs --detect.pnpm.dependency.types.excluded=DEV
    @Test
    public void includedPackagesAppsNextjsReportsDependenciesInheritedThroughLinkedWorkspaces() throws Exception {
        List<CodeLocation> codeLocations = parse(EnumListFilter.fromExcluded(PnpmDependencyType.DEV), Collections.singletonList("apps/nextjs"));

        Set<String> reachable = new HashSet<>(PnpmWorkspaceDependencySummary.from(codeLocations.get(0).getDependencyGraph(), true).depNames);
        Assertions.assertEquals(expectedAppsNextjsProductionPackages(), reachable);
    }

    // packages/api, auth, db and validators are excluded by the filter, but apps/nextjs ships their dependencies.
    // drizzle-orm, drizzle-zod, @t3-oss/env-core and zod are reachable only through those linked workspaces.
    private Set<String> expectedAppsNextjsProductionPackages() {
        return new HashSet<>(Arrays.asList(
            "@t3-oss/env-core@0.13.8", "@tanstack/query-core@5.90.8", "@tanstack/react-query@5.90.8", "@trpc/client@11.7.1",
            "@trpc/server@11.7.1", "copy-anything@4.1.5", "drizzle-orm@0.44.7", "drizzle-zod@0.8.3", "react@19.3.1",
            "superjson@2.2.3", "typescript@5.9.3", "zod@4.1.12",
            "@acme/api@", "@acme/auth@", "@acme/db@", "@acme/validators@"
        ));
    }

    @Test
    public void linkedModuleCodeLocationIsUnchanged() throws Exception {
        // a module whose dependencies are all registry packages is not affected by link handling
        NameVersionGraphAssert db = graphFor(parse(EnumListFilter.fromExcluded(PnpmDependencyType.DEV), Collections.emptyList()), "packages/db");

        db.hasRootDependency("drizzle-orm", "0.44.7");
        db.hasRootDependency("drizzle-zod", "0.8.3");
        db.hasRootDependency("zod", "4.1.12");
        db.hasRootSize(3);
    }

    private List<CodeLocation> parse(EnumListFilter<PnpmDependencyType> dependencyTypeFilter, List<String> includedPackages) throws Exception {
        PnpmLockOptions options = new PnpmLockOptions(dependencyTypeFilter, Collections.emptyList(), includedPackages);
        PnpmLinkedPackageResolver resolver = new PnpmLinkedPackageResolver(
            FunctionalTestFiles.asFile(WORKSPACE_ROOT),
            new PackageJsonFiles(new PackageJsonReader(new Gson()))
        );
        File lockfile = FunctionalTestFiles.asFile(LOCKFILE);
        return new PnpmLockYamlParserInitial(options).parse(lockfile, new NameVersion("create-t3-turbo", "1.0.0"), resolver);
    }

    private NameVersionGraphAssert nextjsGraph(EnumListFilter<PnpmDependencyType> dependencyTypeFilter) throws Exception {
        return graphFor(parse(dependencyTypeFilter, Collections.emptyList()), "apps/nextjs");
    }

    private NameVersionGraphAssert graphFor(List<CodeLocation> codeLocations, String importerKey) {
        Optional<CodeLocation> codeLocation = codeLocations.stream()
            .filter(location -> location.getExternalId().map(ExternalId::getName).filter(importerKey::equals).isPresent())
            .findFirst();
        Assertions.assertTrue(codeLocation.isPresent(), "Expected a code location for " + importerKey);
        return new NameVersionGraphAssert(Forge.NPMJS, codeLocation.get().getDependencyGraph());
    }
}
