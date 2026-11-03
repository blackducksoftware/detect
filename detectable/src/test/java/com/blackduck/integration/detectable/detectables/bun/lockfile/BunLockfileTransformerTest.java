package com.blackduck.integration.detectable.detectables.bun.lockfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunDependencyType;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockfileData;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunPackage;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunPackageDependency;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.DirectDependency;
import com.blackduck.integration.detectable.util.DependencyCreator;

class BunLockfileTransformerTest {

    private final BunLockfileTransformer transformer = new BunLockfileTransformer();

    @Test
    void producesSingleCodeLocationWithDirectDependenciesAsRoots() {
        BunLockfileData data = new BunLockfileData(
            Arrays.asList(direct("express", "4.21.2"), direct("grunt", "1.6.3")),
            Arrays.asList(pkg("express", "express", "4.21.2"), pkg("grunt", "grunt", "1.6.3"), pkg("lodash", "lodash", "4.17.21"))
        );

        List<CodeLocation> codeLocations = transformer.generateCodeLocations(data);

        assertEquals(1, codeLocations.size());
        assertEquals(setOf("express@4.21.2", "grunt@1.6.3"), describe(codeLocations.get(0).getDependencyGraph().getRootDependencies()));
    }

    @Test
    void resolvesDependencyFromFlatKeyWhenNoNestedKeyExists() {
        DependencyGraph graph = graphOf(
            Collections.singletonList(direct("express", "4.21.2")),
            pkg("express", "express", "4.21.2", dep("body-parser")),
            pkg("body-parser", "body-parser", "1.20.3")
        );

        assertEquals(setOf("body-parser@1.20.3"), childrenOf(graph, "express", "4.21.2"));
    }

    @Test
    void pathQualifiedKeyWinsOverFlatKey() {
        // grunt-concurrent needs async@1.5.2 even though the hoisted/flat async is 2.6.4.
        DependencyGraph graph = graphOf(
            Collections.singletonList(direct("grunt-concurrent", "2.3.1")),
            pkg("grunt-concurrent", "grunt-concurrent", "2.3.1", dep("async")),
            pkg("async", "async", "2.6.4"),
            pkg("grunt-concurrent/async", "async", "1.5.2")
        );

        assertEquals(setOf("async@1.5.2"), childrenOf(graph, "grunt-concurrent", "2.3.1"));
    }

    @Test
    void eachParentGetsItsOwnVersionOfTheSameDependency() {
        DependencyGraph graph = graphOf(
            Arrays.asList(direct("grunt-concurrent", "2.3.1"), direct("grunt-legacy-util", "2.0.2"), direct("async", "2.6.4")),
            pkg("async", "async", "2.6.4"),
            pkg("grunt-concurrent", "grunt-concurrent", "2.3.1", dep("async")),
            pkg("grunt-legacy-util", "grunt-legacy-util", "2.0.2", dep("async")),
            pkg("grunt-concurrent/async", "async", "1.5.2"),
            pkg("grunt-legacy-util/async", "async", "3.2.6")
        );

        assertEquals(setOf("async@1.5.2"), childrenOf(graph, "grunt-concurrent", "2.3.1"));
        assertEquals(setOf("async@3.2.6"), childrenOf(graph, "grunt-legacy-util", "2.0.2"));
    }

    @Test
    void ancestorWalkFindsDependencyOverriddenAtAnIntermediateLevel() {
        // npm/chalk depends on ansi-styles. There is no "npm/chalk/ansi-styles", but "npm/ansi-styles"
        // exists and must win over the flat ansi-styles@3.2.1.
        DependencyGraph graph = graphOf(
            Collections.singletonList(direct("npm", "1.0.0")),
            pkg("npm", "npm", "1.0.0", dep("chalk")),
            pkg("chalk", "chalk", "2.4.2"),
            pkg("npm/chalk", "chalk", "1.1.3", dep("ansi-styles")),
            pkg("ansi-styles", "ansi-styles", "3.2.1"),
            pkg("npm/ansi-styles", "ansi-styles", "2.2.1")
        );

        assertEquals(setOf("chalk@1.1.3"), childrenOf(graph, "npm", "1.0.0"));
        assertEquals(setOf("ansi-styles@2.2.1"), childrenOf(graph, "chalk", "1.1.3"));
    }

    @Test
    void deepestMatchingContextWinsOverShallowerOne() {
        DependencyGraph graph = graphOf(
            Collections.singletonList(direct("a", "1.0.0")),
            pkg("a", "a", "1.0.0", dep("b")),
            pkg("a/b", "b", "1.0.0", dep("c")),
            pkg("c", "c", "flat"),
            pkg("a/c", "c", "mid"),
            pkg("a/b/c", "c", "deep")
        );

        assertEquals(setOf("c@deep"), childrenOf(graph, "b", "1.0.0"));
    }

    @Test
    void scopedPackageKeyIsTreatedAsASingleContextSegment() {
        // "@scope/pkg" is one logical key. Its dependency must resolve via "@scope/pkg/dep"
        // and must never be looked up under a bare "@scope" prefix.
        DependencyGraph graph = graphOf(
            Collections.singletonList(direct("@scope/pkg", "1.0.0")),
            pkg("@scope/pkg", "@scope/pkg", "1.0.0", dep("dep")),
            pkg("dep", "dep", "flat"),
            pkg("@scope/pkg/dep", "dep", "nested")
        );

        assertEquals(setOf("dep@nested"), childrenOf(graph, "@scope/pkg", "1.0.0"));
    }

    @Test
    void scopedPackageFallsBackToFlatKeyWhenNoNestedKeyExists() {
        DependencyGraph graph = graphOf(
            Collections.singletonList(direct("@scope/pkg", "1.0.0")),
            pkg("@scope/pkg", "@scope/pkg", "1.0.0", dep("dep")),
            pkg("dep", "dep", "flat")
        );

        assertEquals(setOf("dep@flat"), childrenOf(graph, "@scope/pkg", "1.0.0"));
    }

    @Test
    void scopedDependencyUnderNestedParentResolvesViaAncestorWalk() {
        // parent "host/lib" needs "@scope/util"; only the flat entry exists.
        DependencyGraph graph = graphOf(
            Collections.singletonList(direct("host", "1.0.0")),
            pkg("host", "host", "1.0.0", dep("lib")),
            pkg("host/lib", "lib", "2.0.0", dep("@scope/util")),
            pkg("@scope/util", "@scope/util", "3.0.0")
        );

        assertEquals(setOf("@scope/util@3.0.0"), childrenOf(graph, "lib", "2.0.0"));
    }

    @Test
    void scopedDependencyOverriddenUnderScopedParentIsFound() {
        DependencyGraph graph = graphOf(
            Collections.singletonList(direct("@scope/pkg", "1.0.0")),
            pkg("@scope/pkg", "@scope/pkg", "1.0.0", dep("@other/util")),
            pkg("@other/util", "@other/util", "flat"),
            pkg("@scope/pkg/@other/util", "@other/util", "nested")
        );

        assertEquals(setOf("@other/util@nested"), childrenOf(graph, "@scope/pkg", "1.0.0"));
    }

    @Test
    void unresolvableDependencyIsSkippedWithoutFailing() {
        DependencyGraph graph = graphOf(
            Collections.singletonList(direct("express", "4.21.2")),
            pkg("express", "express", "4.21.2", dep("missing-package"), dep("body-parser")),
            pkg("body-parser", "body-parser", "1.20.3")
        );

        assertEquals(setOf("body-parser@1.20.3"), childrenOf(graph, "express", "4.21.2"));
    }

    @Test
    void usesNpmjsForge() {
        DependencyGraph graph = graphOf(
            Collections.singletonList(direct("express", "4.21.2")),
            pkg("express", "express", "4.21.2")
        );

        assertTrue(graph.getRootDependencies().stream().allMatch(d -> Forge.NPMJS.equals(d.getExternalId().getForge())));
    }

    private DependencyGraph graphOf(List<DirectDependency> directs, BunPackage... packages) {
        List<CodeLocation> codeLocations = transformer.generateCodeLocations(new BunLockfileData(directs, Arrays.asList(packages)));
        return codeLocations.get(0).getDependencyGraph();
    }

    private Set<String> childrenOf(DependencyGraph graph, String name, String version) {
        Dependency parent = DependencyCreator.nameVersion(Forge.NPMJS, name, version);
        return describe(graph.getChildrenForParent(parent));
    }

    private Set<String> describe(Set<Dependency> dependencies) {
        return dependencies.stream()
            .map(d -> d.getExternalId().getName() + "@" + d.getExternalId().getVersion())
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<String> setOf(String... values) {
        return new TreeSet<>(Arrays.asList(values));
    }

    private DirectDependency direct(String name, String version) {
        return new DirectDependency(name, version, BunDependencyType.NORMAL);
    }

    private BunPackageDependency dep(String name) {
        return new BunPackageDependency(name, "*", BunDependencyType.NORMAL);
    }

    private BunPackage pkg(String key, String name, String version, BunPackageDependency... dependencies) {
        return new BunPackage(key, name, version, Arrays.asList(dependencies));
    }
}
