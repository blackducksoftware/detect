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
 * Verifies that Yarn 4 (Berry) "npm:" protocol specifiers (any version) are scanned
 * without crashing and the components appear in the dependency graph with their resolved versions.
 *
 * Two header variants are exercised in the same lockfile:
 *   - Dual-ID:   "client-lib@npm:, client-lib@npm:2.3.0" — another package pins the same version
 *   - Single-ID: "utils-lib@npm:"                        — no other specifier for this package
 *
 * Root cause: the trailing colon of "@npm:" is stripped by removeEnd(), causing a key mismatch
 * that routes through the LazyId fallback and produces an empty version string. Without Fix 2,
 * mustUpgradeEqual() crashes on "".substring(1).
 */
public class YarnLockV4NpmProtocolSpecifierTest extends DetectableFunctionalTest {

    public YarnLockV4NpmProtocolSpecifierTest() throws IOException {
        super("yarn");
    }

    @Override
    protected void setup() throws IOException {
        addFile(
            Paths.get("yarn.lock"),
            "\"server-lib@npm:1.0.0\":",
            "  version: 1.0.0",
            "  resolution: \"server-lib@npm:1.0.0\"",
            "  dependencies:",
            "    client-lib: \"npm:\"",
            "    utils-lib: \"npm:\"",
            "  languageName: node",
            "  linkType: hard",
            "",
            // Dual-ID: client-lib@npm: alongside a pinned versioned ID
            "\"client-lib@npm:, client-lib@npm:2.3.0\":",
            "  version: 2.3.0",
            "  resolution: \"client-lib@npm:2.3.0\"",
            "  languageName: node",
            "  linkType: hard",
            "",
            // Single-ID: utils-lib@npm: is the only specifier — no companion versioned ID
            "\"utils-lib@npm:\":",
            "  version: 1.5.0",
            "  resolution: \"utils-lib@npm:1.5.0\"",
            "  languageName: node",
            "  linkType: hard"
        );

        addFile(
            Paths.get("package.json"),
            "{",
            "  \"name\": \"test-app\",",
            "  \"version\": \"1.0.0\",",
            "  \"dependencies\": {",
            "    \"server-lib\": \"1.0.0\"",
            "  }",
            "}"
        );
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        return detectableFactory.createYarnLockDetectable(
            detectableEnvironment,
            new YarnLockOptions(EnumListFilter.fromExcluded(YarnDependencyType.NON_PRODUCTION), new ArrayList<>(0), new ArrayList<>(0), false)
        );
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals(1, extraction.getCodeLocations().size());
        CodeLocation codeLocation = extraction.getCodeLocations().get(0);

        Assertions.assertEquals("test-app", extraction.getProjectName());
        Assertions.assertEquals("1.0.0", extraction.getProjectVersion());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, codeLocation.getDependencyGraph());
        graphAssert.hasRootSize(1);
        graphAssert.hasRootDependency("server-lib", "1.0.0");

        // Dual-ID case: "client-lib@npm:, client-lib@npm:2.3.0"
        graphAssert.hasDependency("client-lib", "2.3.0");
        graphAssert.hasParentChildRelationship("server-lib", "1.0.0", "client-lib", "2.3.0");

        // Single-ID case: "utils-lib@npm:" with no companion versioned ID
        graphAssert.hasDependency("utils-lib", "1.5.0");
        graphAssert.hasParentChildRelationship("server-lib", "1.0.0", "utils-lib", "1.5.0");
    }
}
