package com.blackduck.integration.detectable.detectables.yarn.functional;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.yarn.YarnDependencyType;
import com.blackduck.integration.detectable.detectables.yarn.YarnLockOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;

/**
 * Yarn 4 aliases (npm alias, empty range, bare "npm:", patch) are reported as the real package named by
 * the entry's "resolution:", with its transitive dependencies attached. file: dependencies are unchanged.
 */
public class YarnLockAliasResolutionTest extends DetectableFunctionalTest {

    public YarnLockAliasResolutionTest() throws IOException {
        super("yarn");
    }

    protected boolean ignoreAllWorkspaces() {
        return false;
    }

    @Override
    protected void setup() throws IOException {
        addFile(
            Paths.get("yarn.lock"),
            "__metadata:",
            "  version: 8",
            "  cacheKey: 10c0",
            "",
            // Every descriptor in use has a header key in a real lockfile, including the one consumer@1.0.0 uses below
            "\"old-width@npm:string-width@4.2.3, string-width-alias@npm:string-width@4.2.3\":",
            "  version: 4.2.3",
            "  resolution: \"string-width@npm:4.2.3\"",
            "  dependencies:",
            "    emoji-regex: \"npm:^8.0.0\"",
            "    strip-ansi: \"npm:^6.0.1\"",
            "  languageName: node",
            "  linkType: hard",
            "",
            "\"empty-version-alias@npm:left-pad@\":",
            "  version: 1.3.0",
            "  resolution: \"left-pad@npm:1.3.0\"",
            "  languageName: node",
            "  linkType: hard",
            "",
            "\"empty-npm-dep@npm:, left-pad@npm:1.3.0\":",
            "  version: 1.3.0",
            "  resolution: \"left-pad@npm:1.3.0\"",
            "  languageName: node",
            "  linkType: hard",
            "",
            "\"file-dep@file:../../vendor/file-dep::locator=test-app%40workspace%3A.\":",
            "  version: 1.0.0",
            "  resolution: \"file-dep@file:../../vendor/file-dep::locator=test-app%40workspace%3A.\"",
            "  languageName: node",
            "  linkType: hard",
            "",
            "\"patched-ms@patch:ms@npm%3A2.1.3#../../.yarn/patches/ms-npm-2.1.3.patch::version=2.1.3&hash=fixture\":",
            "  version: 2.1.3",
            "  resolution: \"patched-ms@patch:ms@npm%3A2.1.3#../../.yarn/patches/ms-npm-2.1.3.patch::version=2.1.3&hash=fixture\"",
            "  languageName: node",
            "  linkType: hard",
            "",
            "\"emoji-regex@npm:^8.0.0\":",
            "  version: 8.0.0",
            "  resolution: \"emoji-regex@npm:8.0.0\"",
            "  languageName: node",
            "  linkType: hard",
            "",
            "\"strip-ansi@npm:^6.0.1\":",
            "  version: 6.0.1",
            "  resolution: \"strip-ansi@npm:6.0.1\"",
            "  dependencies:",
            "    ansi-regex: \"npm:^5.0.1\"",
            "  languageName: node",
            "  linkType: hard",
            "",
            "\"ansi-regex@npm:^5.0.1\":",
            "  version: 5.0.1",
            "  resolution: \"ansi-regex@npm:5.0.1\"",
            "  languageName: node",
            "  linkType: hard",
            "",
            // A package that pulls in an alias through its own dependencies block
            "\"consumer@npm:1.0.0\":",
            "  version: 1.0.0",
            "  resolution: \"consumer@npm:1.0.0\"",
            "  dependencies:",
            "    old-width: \"npm:string-width@4.2.3\"",
            "  languageName: node",
            "  linkType: hard"
        );

        addFile(
            Paths.get("package.json"),
            "{",
            "  \"name\": \"test-app\",",
            "  \"version\": \"1.0.0\",",
            "  \"dependencies\": {",
            "    \"string-width-alias\": \"npm:string-width@4.2.3\",",
            "    \"empty-version-alias\": \"npm:left-pad@\",",
            "    \"empty-npm-dep\": \"npm:\",",
            "    \"file-dep\": \"file:../../vendor/file-dep\",",
            "    \"patched-ms\": \"patch:ms@npm%3A2.1.3#../../.yarn/patches/ms-npm-2.1.3.patch\",",
            "    \"consumer\": \"1.0.0\"",
            "  }",
            "}"
        );
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        return detectableFactory.createYarnLockDetectable(
            detectableEnvironment,
            new YarnLockOptions(EnumListFilter.fromExcluded(YarnDependencyType.NON_PRODUCTION), new ArrayList<>(0), new ArrayList<>(0), ignoreAllWorkspaces())
        );
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals(1, extraction.getCodeLocations().size());
        CodeLocation codeLocation = extraction.getCodeLocations().get(0);
        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, codeLocation.getDependencyGraph());

        // No alias name may leak into the graph as a component
        graphAssert.hasNoDependency("string-width-alias", "4.2.3");
        graphAssert.hasNoDependency("empty-version-alias", "1.3.0");
        graphAssert.hasNoDependency("empty-npm-dep", "1.3.0");
        graphAssert.hasNoDependency("old-width", "4.2.3");

        // Real packages, with their transitive dependencies still attached
        graphAssert.hasDependency("string-width", "4.2.3");
        graphAssert.hasParentChildRelationship("string-width", "4.2.3", "emoji-regex", "8.0.0");
        graphAssert.hasParentChildRelationship("string-width", "4.2.3", "strip-ansi", "6.0.1");
        graphAssert.hasParentChildRelationship("strip-ansi", "6.0.1", "ansi-regex", "5.0.1");
        graphAssert.hasDependency("left-pad", "1.3.0");

        // An alias reached through another package's dependencies block
        graphAssert.hasParentChildRelationship("consumer", "1.0.0", "string-width", "4.2.3");

        assertModeSpecific(graphAssert);
    }

    protected void assertModeSpecific(NameVersionGraphAssert graphAssert) {
        graphAssert.hasRootDependency("string-width", "4.2.3");
        graphAssert.hasRootDependency("left-pad", "1.3.0");
        graphAssert.hasRootDependency("consumer", "1.0.0");

        // patch of an alias is the patched package itself
        graphAssert.hasRootDependency("ms", "2.1.3");
        graphAssert.hasNoDependency("patched-ms", "npm%3A2.1.3#../../.yarn/patches/ms-npm-2.1.3.patch");

        // file: has no registry identity, behavior is unchanged
        graphAssert.hasRootDependency("file-dep", "1.0.0");
    }
}
