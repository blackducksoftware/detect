package com.blackduck.integration.detectable.detectables.uv.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.blackduck.integration.detectable.detectables.pip.parser.RequirementsFileDependencyTransformer;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import com.blackduck.integration.detectable.detectables.uv.lockfile.UVLockfileExtractor;
import com.blackduck.integration.detectable.detectables.uv.parse.UVTomlParser;
import com.blackduck.integration.detectable.detectables.uv.transform.UVLockParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.python.util.PythonDependencyTransformer;
import com.blackduck.integration.util.NameVersion;

class UVLockfileExtractorTest {

    @TempDir
    Path tempDir;

    /**
     * When uvLockFile is present and parseLockFile returns empty (because all onlyGroups are
     * excluded), the extractor should create one empty CodeLocation with project identity so
     * the BOM still contains the project — consistent with the CLI/buildless detector path.
     */
    @Test
    void extractReturnsEmptyCodeLocationWhenAllOnlyGroupsExcluded() throws Exception {
        // Write a real (empty-content) uv.lock file so FileUtils.readFileToString succeeds
        File uvLockFile = tempDir.resolve("uv.lock").toFile();
        FileUtils.write(uvLockFile, "", StandardCharsets.UTF_8);

        // parseLockFile returns empty mutable list — faithfully represents the real UVLockParser
        // which always returns its internal ArrayList (mutable). Simulates all onlyGroups cancelled.
        UVLockParser lockParser = mock(UVLockParser.class);
        when(lockParser.parseLockFile(anyString(), anyString(), any())).thenReturn(new ArrayList<>());

        UVTomlParser tomlParser = mock(UVTomlParser.class);
        when(tomlParser.parseNameVersion()).thenReturn(Optional.of(new NameVersion("my-project", "1.0.0")));
        when(tomlParser.getProjectName()).thenReturn("my-project");

        PythonDependencyTransformer reqTransformer = mock(PythonDependencyTransformer.class);
        RequirementsFileDependencyTransformer reqDepTransformer = mock(RequirementsFileDependencyTransformer.class);

        UVLockfileExtractor extractor = new UVLockfileExtractor(lockParser, reqTransformer, reqDepTransformer);

        // excludedGroups = [dev], onlyGroups = [dev] → all onlyGroups cancelled
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev"),   // excludedDependencyGroups
            Arrays.asList("dev"),   // onlyDependencyGroups
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(options, tomlParser, uvLockFile, null);

        assertTrue(extraction.isSuccess(), "Extraction should succeed");
        assertEquals(1, extraction.getCodeLocations().size(),
                "Expected exactly one empty CodeLocation preserving project identity in the BOM");
        assertEquals(0, extraction.getCodeLocations().get(0).getDependencyGraph().getRootDependencies().size(),
                "Expected zero dependencies in the empty BOM (all groups were cancelled)");
        assertTrue(extraction.getCodeLocations().get(0).getExternalId().isPresent(),
                "Expected ExternalId to be present so project identity is preserved in the BOM");
        assertEquals("my-project", extraction.getCodeLocations().get(0).getExternalId().get().getName(),
                "Expected ExternalId name to match project name 'my-project'");
        assertEquals("1.0.0", extraction.getCodeLocations().get(0).getExternalId().get().getVersion(),
                "Expected ExternalId version to match project version '1.0.0'");
    }

    /**
     * When uvLockFile is present, parseLockFile returns empty, onlyGroups is set, but the
     * project version is not available, the extractor should still create one empty CodeLocation
     * (without an ExternalId) rather than returning zero code locations.
     */
    @Test
    void extractReturnsEmptyCodeLocationWithoutExternalIdWhenProjectVersionMissing() throws Exception {
        File uvLockFile = tempDir.resolve("uv.lock").toFile();
        FileUtils.write(uvLockFile, "", StandardCharsets.UTF_8);

        UVLockParser lockParser = mock(UVLockParser.class);
        when(lockParser.parseLockFile(anyString(), anyString(), any())).thenReturn(new ArrayList<>());

        UVTomlParser tomlParser = mock(UVTomlParser.class);
        when(tomlParser.parseNameVersion()).thenReturn(Optional.empty()); // no version available
        when(tomlParser.getProjectName()).thenReturn("my-project");

        PythonDependencyTransformer reqTransformer = mock(PythonDependencyTransformer.class);
        RequirementsFileDependencyTransformer reqDepTransformer = mock(RequirementsFileDependencyTransformer.class);

        UVLockfileExtractor extractor = new UVLockfileExtractor(lockParser, reqTransformer, reqDepTransformer);

        // excludedGroups = [dev], onlyGroups = [dev] → all cancelled
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev"),
            Arrays.asList("dev"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(options, tomlParser, uvLockFile, null);

        assertTrue(extraction.isSuccess(), "Extraction should succeed even when project version is missing");
        assertEquals(1, extraction.getCodeLocations().size(),
                "Expected exactly one empty CodeLocation even when project version is unavailable");
        assertEquals(0, extraction.getCodeLocations().get(0).getDependencyGraph().getRootDependencies().size(),
                "Expected zero dependencies in the empty BOM");
        assertFalse(extraction.getCodeLocations().get(0).getExternalId().isPresent(),
                "Expected no ExternalId when project version is not available");
    }

    /**
     * Regression test for the null-uvLockFile bug:
     * When uvLockFile is null (only requirements.txt is present), the extractor must NOT
     * add a spurious empty CodeLocation even when onlyGroups is set, because parseLockFile
     * was never called — there is no cancellation scenario to compensate for.
     */
    @Test
    void extractDoesNotAddSpuriousEmptyCodeLocationWhenLockFileIsNull() throws Exception {
        UVLockParser lockParser = mock(UVLockParser.class);

        UVTomlParser tomlParser = mock(UVTomlParser.class);
        when(tomlParser.parseNameVersion()).thenReturn(Optional.of(new NameVersion("my-project", "1.0.0")));
        when(tomlParser.getProjectName()).thenReturn("my-project");

        PythonDependencyTransformer reqTransformer = mock(PythonDependencyTransformer.class);
        RequirementsFileDependencyTransformer reqDepTransformer = mock(RequirementsFileDependencyTransformer.class);

        UVLockfileExtractor extractor = new UVLockfileExtractor(lockParser, reqTransformer, reqDepTransformer);

        // onlyGroups is set — in the old (buggy) code this would trigger the fallback
        // even though uvLockFile is null and parseLockFile was never called
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev"),   // excludedDependencyGroups
            Arrays.asList("dev"),   // onlyDependencyGroups
            Collections.emptyList(),
            Collections.emptyList()
        );

        // uvLockFile = null, requirementsTxtFile = null
        Extraction extraction = extractor.extract(options, tomlParser, null, null);

        assertTrue(extraction.isSuccess(), "Extraction should succeed");
        assertEquals(0, extraction.getCodeLocations().size(),
                "Expected zero code locations: uvLockFile was null so parseLockFile was never called, "
                + "the empty-BOM fallback must NOT fire in this case");
    }
}

