package com.blackduck.integration.detectable.detectables.uv.functional;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.UVResolver;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.functional.DetectableFunctionalTest;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;
import com.blackduck.integration.executable.ExecutableOutput;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

/**
 * Functional test for UV detector with detect.uv.dependency.groups.only.
 *
 * Validates that the UV detector correctly limits scanning to only the specified
 * dependency groups. When onlyGroups is set, the detector should pass --only-group
 * flags (not --all-groups) and exclude regular dependencies and optional extras.
 */
public class UVOnlyGroupsFunctionalTest extends DetectableFunctionalTest {

    protected UVOnlyGroupsFunctionalTest() throws IOException {
        super("uv-only-groups");
    }

    @Override
    protected void setup() throws IOException {
        // pyproject.toml with regular dependencies and multiple dependency groups
        addFile(Paths.get("pyproject.toml"),
                "[project]\n" +
                "name = \"my-app\"\n" +
                "version = \"1.0.0\"\n" +
                "dependencies = [\n" +
                "  \"fastapi>=0.100.0\",\n" +
                "]\n" +
                "[dependency-groups]\n" +
                "dev = [\n" +
                "  \"pytest>=7.0.0\",\n" +
                "]\n" +
                "lint = [\n" +
                "  \"ruff>=0.4.0\",\n" +
                "]\n" +
                "[tool.uv]\n" +
                "managed = true\n");

        // Tree output when only "dev" group is requested via --only-group dev.
        // Regular dependencies (fastapi) and other groups (lint/ruff) are NOT in the output.
        ExecutableOutput uvTreeDependencyOutput = createStandardOutput(
                "my-app v1.0.0\n" +
                "└── pytest v8.3.4\n" +
                "    ├── iniconfig v2.1.0\n" +
                "    ├── packaging v24.2\n" +
                "    └── pluggy v1.5.0");

        // Expected command: uv tree --no-dedupe --only-group dev
        // Note: --all-groups should NOT be present
        addExecutableOutput(uvTreeDependencyOutput, new File("uv").getAbsolutePath(), "tree", "--no-dedupe", "--only-group", "dev");
    }

    @NotNull
    @Override
    public Detectable create(@NotNull DetectableEnvironment detectableEnvironment) {
        class UVResolverTest implements UVResolver {
            @Override
            public ExecutableTarget resolveUV() throws DetectableException {
                return ExecutableTarget.forFile(new File("uv"));
            }
        }

        // Configure to include ONLY the "dev" dependency group
        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),          // excludedDependencyGroups
            Arrays.asList("dev"),             // onlyDependencyGroups
            Collections.emptyList(),          // includedWorkspaceMembers
            Collections.emptyList()           // excludedWorkspaceMembers
        );

        return detectableFactory.createUVBuildDetectable(
            detectableEnvironment,
            new UVResolverTest(),
            options
        );
    }

    @Override
    public void assertExtraction(@NotNull Extraction extraction) {
        Assertions.assertEquals(1, extraction.getCodeLocations().size(), "Expected exactly one code location.");

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.PYPI, extraction.getCodeLocations().get(0).getDependencyGraph());

        // Only dev group deps should be present — no fastapi (regular dep), no ruff (lint group)
        graphAssert.hasRootSize(1);
        graphAssert.hasRootDependency("pytest", "8.3.4");

        // Verify transitive dependencies of pytest
        graphAssert.hasDependency("iniconfig", "2.1.0");
        graphAssert.hasDependency("packaging", "24.2");
        graphAssert.hasDependency("pluggy", "1.5.0");

        // Verify excluded deps are NOT in the graph
        graphAssert.hasNoDependency("fastapi", "0.109.0");
        graphAssert.hasNoDependency("ruff", "0.4.1");
    }
}

