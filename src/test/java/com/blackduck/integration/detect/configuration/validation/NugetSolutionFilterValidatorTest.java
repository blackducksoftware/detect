package com.blackduck.integration.detect.configuration.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;

class NugetSolutionFilterValidatorTest {
    private static final List<String> NONE = Collections.emptyList();
    private static final List<String> MODULES = Collections.singletonList("^Project.A$");

    private final NugetSolutionFilterValidator validator = new NugetSolutionFilterValidator();

    @Test
    void passesWhenPropertyNotSet() {
        assertFalse(validator.validate(null, MODULES, MODULES).isPresent());
    }

    @Test
    void passesForExistingSlnf(@TempDir Path tempDir) throws IOException {
        Path slnf = Files.write(tempDir.resolve("filtered.slnf"), "{}".getBytes());
        assertFalse(validator.validate(slnf, NONE, NONE).isPresent());
    }

    @Test
    void failsWhenFileDoesNotExist(@TempDir Path tempDir) {
        assertConfigurationFailure(validator.validate(tempDir.resolve("missing.slnf"), NONE, NONE), "does not exist");
    }

    @Test
    void failsWhenExtensionIsNotSlnf(@TempDir Path tempDir) throws IOException {
        Path sln = Files.write(tempDir.resolve("solution.sln"), "".getBytes());
        assertConfigurationFailure(validator.validate(sln, NONE, NONE), ".slnf extension");
    }

    @Test
    void failsWhenPathIsDirectory(@TempDir Path tempDir) throws IOException {
        Path directory = Files.createDirectories(tempDir.resolve("folder.slnf"));
        assertConfigurationFailure(validator.validate(directory, NONE, NONE), "not a regular file");
    }

    @Test
    void failsWhenCombinedWithIncludedModules(@TempDir Path tempDir) throws IOException {
        Path slnf = Files.write(tempDir.resolve("filtered.slnf"), "{}".getBytes());
        assertConfigurationFailure(validator.validate(slnf, MODULES, NONE), "(found: detect.nuget.included.modules)");
    }

    @Test
    void failsWhenCombinedWithExcludedModules(@TempDir Path tempDir) throws IOException {
        Path slnf = Files.write(tempDir.resolve("filtered.slnf"), "{}".getBytes());
        assertConfigurationFailure(validator.validate(slnf, NONE, MODULES), "(found: detect.nuget.excluded.modules)");
    }

    @Test
    void failsWhenCombinedWithBothModuleProperties(@TempDir Path tempDir) throws IOException {
        Path slnf = Files.write(tempDir.resolve("filtered.slnf"), "{}".getBytes());
        assertConfigurationFailure(validator.validate(slnf, MODULES, MODULES), "(found: detect.nuget.included.modules, detect.nuget.excluded.modules)");
    }

    private static void assertConfigurationFailure(Optional<DetectUserFriendlyException> result, String expectedMessageFragment) {
        assertTrue(result.isPresent());
        assertEquals(ExitCodeType.FAILURE_CONFIGURATION, result.get().getExitCodeType());
        assertTrue(result.get().getMessage().contains(expectedMessageFragment), result.get().getMessage());
    }
}
