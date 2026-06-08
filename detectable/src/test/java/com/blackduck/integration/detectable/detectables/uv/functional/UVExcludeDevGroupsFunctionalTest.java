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
 * Functional test for UV detector with excluded dependency groups.
 * 
 * This test validates that the UV detector correctly excludes dev dependencies
 * when configured with excluded dependency groups. The detector should pass
 * --no-group flags to the uv tree command for each excluded group.
 * 
 * WHY THIS TEST:
 * - Validates the exclude groups feature works end-to-end
 * - Ensures security/compliance use case where dev deps shouldn't be in BOM
 * - Tests that argument building produces correct command line flags
 * - Verifies filtered output is parsed correctly
 */
public class UVExcludeDevGroupsFunctionalTest extends DetectableFunctionalTest {

    protected UVExcludeDevGroupsFunctionalTest() throws IOException {
        super("uv-exclude-dev");
    }

    @Override
    protected void setup() throws IOException {
        // pyproject.toml with dev dependencies
        addFile(Paths.get("pyproject.toml"),
                "[project]\n" +
                "name = \"my-app\"\n" +
                "version = \"2.0.0\"\n" +
                "dependencies = [\n" +
                "  \"fastapi>=0.100.0\",\n" +
                "]\n" +
                "[tool.uv]\n" +
                "managed = true\n" +
                "dev-dependencies = [\n" +
                "  \"pytest>=7.0.0\",\n" +
                "  \"mypy>=1.0.0\",\n" +
                "]\n");

        // Tree output when dev group is excluded (no pytest or mypy)
        ExecutableOutput uvTreeDependencyOutput = createStandardOutput(
                "my-app v2.0.0\n" +
                "└── fastapi v0.109.0\n" +
                "    ├── starlette v0.35.0\n" +
                "    │   └── anyio v4.2.0\n" +
                "    └── pydantic v2.5.3\n" +
                "        └── typing-extensions v4.9.0");

        // Note: with --no-group dev, pytest and mypy are excluded from output
        addExecutableOutput(uvTreeDependencyOutput, new File("uv").getAbsolutePath(), "tree", "--no-dedupe", "--all-extras", "--all-groups", "--no-group", "dev");
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

        // Configure to exclude the "dev" dependency group
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev"),     // excludedDependencyGroups
            Collections.emptyList(),  // includedWorkspaceMembers
            Collections.emptyList()   // excludedWorkspaceMembers
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
        
        // Should have fastapi as root (dev deps excluded)
        graphAssert.hasRootSize(1);
        graphAssert.hasRootDependency("fastapi", "0.109.0");
        
        // Verify transitive dependencies
        graphAssert.hasDependency("starlette", "0.35.0");
        graphAssert.hasDependency("pydantic", "2.5.3");
        graphAssert.hasDependency("anyio", "4.2.0");
        graphAssert.hasDependency("typing-extensions", "4.9.0");
    }
}

