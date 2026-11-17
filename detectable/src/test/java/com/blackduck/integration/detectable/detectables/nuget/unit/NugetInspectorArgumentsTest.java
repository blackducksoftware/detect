package com.blackduck.integration.detectable.detectables.nuget.unit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.blackduck.integration.detectable.detectables.nuget.NugetInspectorArguments;
import com.blackduck.integration.detectable.detectables.nuget.NugetInspectorOptions;

public class NugetInspectorArgumentsTest {
    @Test
    public void solutionFilterIsPassedAsTargetPath(@TempDir Path tempDir) throws IOException {
        Path slnf = Files.write(tempDir.resolve("filtered.slnf"), "{}".getBytes());
        NugetInspectorOptions options = new NugetInspectorOptions.Builder()
            .excludedModules(Collections.emptyList())
            .includedModules(Collections.emptyList())
            .nugetExcludedDependencyTypes(Collections.emptySet())
            .solutionFilterPath(slnf)
            .build();

        List<String> arguments = NugetInspectorArguments.fromInspectorOptions(options, slnf.toFile(), new File(tempDir.toFile(), "out"));

        assertTrue(arguments.contains("--target_path=" + slnf.toFile().getCanonicalPath()));
    }
}
