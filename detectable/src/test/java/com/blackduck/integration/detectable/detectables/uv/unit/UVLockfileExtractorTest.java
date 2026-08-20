package com.blackduck.integration.detectable.detectables.uv.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.pip.parser.RequirementsFileDependencyTransformer;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import com.blackduck.integration.detectable.detectables.uv.lockfile.UVLockfileExtractor;
import com.blackduck.integration.detectable.detectables.uv.parse.UVTomlParser;
import com.blackduck.integration.detectable.detectables.uv.transform.UVLockParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.python.util.PythonDependencyTransformer;
import com.blackduck.integration.util.NameVersion;

class UVLockfileExtractorTest {

    @Test
    void extractReturnsEmptyCodeLocationWhenAllOnlyGroupsExcluded() throws Exception {
        // Setup: no lock file provided, onlyGroups is set → triggers empty-BOM fallback
        UVLockParser lockParser = mock(UVLockParser.class);

        UVTomlParser tomlParser = mock(UVTomlParser.class);
        when(tomlParser.parseNameVersion()).thenReturn(Optional.of(new NameVersion("my-project", "1.0.0")));
        when(tomlParser.getProjectName()).thenReturn("my-project");

        PythonDependencyTransformer reqTransformer = mock(PythonDependencyTransformer.class);
        RequirementsFileDependencyTransformer reqDepTransformer = mock(RequirementsFileDependencyTransformer.class);

        UVLockfileExtractor extractor = new UVLockfileExtractor(lockParser, reqTransformer, reqDepTransformer);

        // onlyGroups = [dev], excludedGroups = [dev] → all cancelled
        UVDetectorOptions options = new UVDetectorOptions(
            Arrays.asList("dev"),
            Arrays.asList("dev"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        // uvLockFile = null simulates the scenario where parseLockFile is not called,
        // codeLocations stays empty, and the empty-BOM fallback triggers
        Extraction extraction = extractor.extract(options, tomlParser, null, null);

        assertTrue(extraction.isSuccess(), "Extraction should succeed");
        assertEquals(1, extraction.getCodeLocations().size(), "Expected one empty code location with project identity");
        assertEquals(0, extraction.getCodeLocations().get(0).getDependencyGraph().getRootDependencies().size(),
                "Expected zero dependencies in the empty BOM");
        assertEquals("my-project", extraction.getCodeLocations().get(0).getExternalId().get().getName(),
                "Expected project name 'my-project' preserved in the empty code location");
        assertEquals("1.0.0", extraction.getCodeLocations().get(0).getExternalId().get().getVersion(),
                "Expected project version '1.0.0' preserved in the empty code location");
    }

    @Test
    void extractReturnsEmptyCodeLocationWithoutVersionWhenProjectVersionMissing() throws Exception {
        UVLockParser lockParser = mock(UVLockParser.class);

        UVTomlParser tomlParser = mock(UVTomlParser.class);
        when(tomlParser.parseNameVersion()).thenReturn(Optional.empty());
        when(tomlParser.getProjectName()).thenReturn("my-project");

        PythonDependencyTransformer reqTransformer = mock(PythonDependencyTransformer.class);
        RequirementsFileDependencyTransformer reqDepTransformer = mock(RequirementsFileDependencyTransformer.class);

        UVLockfileExtractor extractor = new UVLockfileExtractor(lockParser, reqTransformer, reqDepTransformer);

        UVDetectorOptions options = new UVDetectorOptions(
            Collections.emptyList(),
            Arrays.asList("dev"),
            Collections.emptyList(),
            Collections.emptyList()
        );

        Extraction extraction = extractor.extract(options, tomlParser, null, null);

        assertTrue(extraction.isSuccess(), "Extraction should succeed even without project version");
        assertEquals(1, extraction.getCodeLocations().size(), "Expected one empty code location when onlyGroups is set and parser returns empty");
        assertEquals(0, extraction.getCodeLocations().get(0).getDependencyGraph().getRootDependencies().size(),
                "Expected zero dependencies in the empty BOM");
    }
}

