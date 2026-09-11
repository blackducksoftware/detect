package com.blackduck.integration.detectable.detectables.bun.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;

class BunCliParserTest {

    private BunCliParser parser() {
        return new BunCliParser(new ExternalIdFactory());
    }

    // Finds a dependency by name in the graph's root children (direct deps)
    private Dependency findDirect(DependencyGraph graph, String name) {
        return graph.getRootDependencies().stream()
            .filter(d -> name.equals(d.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Direct dep not found: " + name));
    }

    // Finds a dependency by name in the children of a given parent
    private Dependency findChild(DependencyGraph graph, Dependency parent, String name) {
        return graph.getChildrenForParent(parent).stream()
            .filter(d -> name.equals(d.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Child dep not found: " + name + " under " + parent.getName()));
    }

    @Test
    void skipsHeaderLineAndParsesDirectDeps() {
        List<String> lines = Arrays.asList(
            "C:\\Users\\test\\NodeGoat node_modules",
            "├── react@18.3.1",
            "└── typescript@5.4.5"
        );
        DependencyGraph graph = parser().parse(lines);

        Set<String> directNames = graph.getRootDependencies().stream()
            .map(Dependency::getName)
            .collect(Collectors.toSet());

        assertEquals(2, directNames.size());
        assertTrue(directNames.contains("react"));
        assertTrue(directNames.contains("typescript"));
    }

    @Test
    void parsesCorrectVersions() {
        List<String> lines = Arrays.asList(
            "├── react@18.3.1",
            "└── typescript@5.4.5"
        );
        DependencyGraph graph = parser().parse(lines);

        Dependency react = findDirect(graph, "react");
        assertEquals("18.3.1", react.getVersion());

        Dependency ts = findDirect(graph, "typescript");
        assertEquals("5.4.5", ts.getVersion());
    }

    @Test
    void parsesScopedPackageNames() {
        List<String> lines = Arrays.asList(
            "├── @cypress/listr-verbose-renderer@0.4.1",
            "└── @types/node@20.14.0"
        );
        DependencyGraph graph = parser().parse(lines);

        Dependency listr = findDirect(graph, "@cypress/listr-verbose-renderer");
        assertEquals("0.4.1", listr.getVersion());

        Dependency types = findDirect(graph, "@types/node");
        assertEquals("20.14.0", types.getVersion());
    }

    @Test
    void buildsTransitiveRelationships() {
        // @cypress/listr-verbose-renderer → chalk → ansi-styles, escape-string-regexp, supports-color
        List<String> lines = Arrays.asList(
            "├── @cypress/listr-verbose-renderer@0.4.1",
            "│   └── chalk@1.1.3",
            "│       ├── ansi-styles@2.2.1",
            "│       ├── escape-string-regexp@1.0.5",
            "│       └── supports-color@2.0.0"
        );
        DependencyGraph graph = parser().parse(lines);

        Dependency listr = findDirect(graph, "@cypress/listr-verbose-renderer");
        Dependency chalk = findChild(graph, listr, "chalk");
        assertEquals("1.1.3", chalk.getVersion());

        Set<String> chalkChildren = graph.getChildrenForParent(chalk).stream()
            .map(Dependency::getName)
            .collect(Collectors.toSet());
        assertTrue(chalkChildren.contains("ansi-styles"));
        assertTrue(chalkChildren.contains("escape-string-regexp"));
        assertTrue(chalkChildren.contains("supports-color"));
        assertEquals(3, chalkChildren.size());
    }

    @Test
    void handlesMultipleLevelNesting() {
        // anymatch → micromatch → extend-shallow → is-extendable
        List<String> lines = Arrays.asList(
            "├── anymatch@2.0.0",
            "│   ├── micromatch@3.1.10",
            "│   │   └── extend-shallow@3.0.2",
            "│   │       └── is-extendable@1.0.1",
            "│   └── normalize-path@2.1.1"
        );
        DependencyGraph graph = parser().parse(lines);

        Dependency anymatch = findDirect(graph, "anymatch");
        Dependency micromatch = findChild(graph, anymatch, "micromatch");
        Dependency extendShallow = findChild(graph, micromatch, "extend-shallow");
        Dependency isExtendable = findChild(graph, extendShallow, "is-extendable");
        assertEquals("1.0.1", isExtendable.getVersion());

        // normalize-path is a sibling of micromatch under anymatch
        Dependency normalizePath = findChild(graph, anymatch, "normalize-path");
        assertEquals("2.1.1", normalizePath.getVersion());
    }

    @Test
    void correctlyAttributesSiblingsAfterMovingUp() {
        // When depth decreases from 3 back to 2, the next dep must be under the depth-1 parent
        List<String> lines = Arrays.asList(
            "├── anymatch@2.0.0",
            "│   ├── micromatch@3.1.10",
            "│   │   └── extend-shallow@3.0.2",
            "│   └── normalize-path@2.1.1",  // depth 2, sibling of micromatch
            "└── arch@2.1.1"                  // depth 1, new direct dep
        );
        DependencyGraph graph = parser().parse(lines);

        Dependency anymatch = findDirect(graph, "anymatch");
        Set<String> anymatchChildren = graph.getChildrenForParent(anymatch).stream()
            .map(Dependency::getName)
            .collect(Collectors.toSet());
        assertTrue(anymatchChildren.contains("micromatch"));
        assertTrue(anymatchChildren.contains("normalize-path"));
        assertEquals(2, anymatchChildren.size());

        // arch must be a direct dep, not a child of anymatch
        Dependency arch = findDirect(graph, "arch");
        assertEquals("2.1.1", arch.getVersion());
    }

    @Test
    void handlesBlankLinesAndWindowsPathHeader() {
        List<String> lines = Arrays.asList(
            "C:\\Users\\ZahidulJewel\\Desktop\\NodeGoat node_modules",
            "",
            "├── abbrev@1.1.1",
            "",
            "└── accepts@1.3.8"
        );
        DependencyGraph graph = parser().parse(lines);
        assertEquals(2, graph.getRootDependencies().size());
    }

    @Test
    void skipsHoistedTransitivesAndWiresThemUnderTheirActualParents() {
        // Exercises the optional directDepNameVersions filter: level-0 entries not in the set
        // are skipped as direct deps but still pushed onto the stack so their subtrees wire
        // correctly when the same package appears nested under an actual direct dep.
        List<String> lines = Arrays.asList(
            "/project node_modules",
            "├── aws-sign2@0.6.0",          // level-0 but not in filter — skip as direct
            "├── bl@1.1.2",                  // level-0 but not in filter — skip as direct
            "│   └── readable-stream@2.0.6", // level-1 under bl — wired via dedup cache
            "└── zaproxy@0.3.0",             // level-0 AND in filter — add as direct dep
            "    ├── lodash@4.16.6",
            "    └── request@2.75.0",
            "        ├── aws-sign2@0.6.0",   // correctly nested under request
            "        └── bl@1.1.2"           // correctly nested under request
        );

        Set<String> directDepNameVersions = new HashSet<>(Arrays.asList("zaproxy@0.3.0"));
        DependencyGraph graph = parser().parse(lines, directDepNameVersions);

        // Only zaproxy is a direct dep
        Set<String> roots = graph.getRootDependencies().stream()
            .map(Dependency::getName)
            .collect(Collectors.toSet());
        assertEquals(1, roots.size());
        assertTrue(roots.contains("zaproxy"));

        // lodash and request are children of zaproxy
        Dependency zaproxy = findDirect(graph, "zaproxy");
        findChild(graph, zaproxy, "lodash");
        Dependency request = findChild(graph, zaproxy, "request");

        // aws-sign2 and bl are children of request (not direct deps)
        findChild(graph, request, "aws-sign2");
        Dependency bl = findChild(graph, request, "bl");

        // bl's sub-dep (readable-stream) was wired in via the hoisted depth-1 entry for bl
        findChild(graph, bl, "readable-stream");
    }

    @Test
    void parsesFullSubtreeFromRealOutput() {
        // Subset of NodeGoat bun list --all covering all structural patterns
        List<String> lines = Arrays.asList(
            "C:\\Users\\ZahidulJewel\\Desktop\\NodeGoat node_modules",
            "├── @cypress/listr-verbose-renderer@0.4.1",
            "│   └── chalk@1.1.3",
            "│       ├── ansi-styles@2.2.1",
            "│       ├── escape-string-regexp@1.0.5",
            "│       └── supports-color@2.0.0",
            "├── @one-ini/wasm@0.2.1",
            "├── @types/sizzle@2.3.2",
            "├── abbrev@1.1.1",
            "├── anymatch@2.0.0",
            "│   ├── micromatch@3.1.10",
            "│   │   └── extend-shallow@3.0.2",
            "│   │       └── is-extendable@1.0.1",
            "│   └── normalize-path@2.1.1",
            "└── arch@2.1.1"
        );
        DependencyGraph graph = parser().parse(lines);

        // 6 direct deps: @cypress/listr-verbose-renderer, @one-ini/wasm, @types/sizzle, abbrev, anymatch, arch
        assertEquals(6, graph.getRootDependencies().size());

        // verify a few parent-child relationships
        Dependency listr = findDirect(graph, "@cypress/listr-verbose-renderer");
        Dependency chalk = findChild(graph, listr, "chalk");
        assertEquals(3, graph.getChildrenForParent(chalk).size());

        Dependency anymatch = findDirect(graph, "anymatch");
        assertEquals(2, graph.getChildrenForParent(anymatch).size());

        Dependency micromatch = findChild(graph, anymatch, "micromatch");
        Dependency extendShallow = findChild(graph, micromatch, "extend-shallow");
        assertEquals(1, graph.getChildrenForParent(extendShallow).size());
    }
}
