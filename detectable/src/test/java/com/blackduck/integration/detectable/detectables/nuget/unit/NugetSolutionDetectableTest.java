package com.blackduck.integration.detectable.detectables.nuget.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectables.nuget.NugetInspectorOptions;
import com.blackduck.integration.detectable.detectable.inspector.nuget.NugetInspectorResolver;
import com.blackduck.integration.detectable.detectables.nuget.NugetInspectorExtractor;
import com.blackduck.integration.detectable.detectables.nuget.NugetSolutionDetectable;
import com.blackduck.integration.detectable.util.MockDetectableEnvironment;
import com.blackduck.integration.detectable.util.MockFileFinder;

public class NugetSolutionDetectableTest {
    @Test
    public void testApplicableForSolution() {
        NugetInspectorResolver nugetInspectorManager = null;
        NugetInspectorExtractor nugetInspectorExtractor = null;
        NugetInspectorOptions nugetInspectorOptions = null;

        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = MockFileFinder.withFileNamed("test.sln");

        NugetSolutionDetectable detectable = new NugetSolutionDetectable(environment, fileFinder, nugetInspectorManager, nugetInspectorExtractor, nugetInspectorOptions);

        assertTrue(detectable.applicable().getPassed());
    }

    @Test
    public void testApplicableForSolutionXML() {
        NugetInspectorResolver nugetInspectorManager = null;
        NugetInspectorExtractor nugetInspectorExtractor = null;
        NugetInspectorOptions nugetInspectorOptions = null;

        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = MockFileFinder.withFileNamed("test.slnx");

        NugetSolutionDetectable detectable = new NugetSolutionDetectable(environment, fileFinder, nugetInspectorManager, nugetInspectorExtractor, nugetInspectorOptions);

        assertTrue(detectable.applicable().getPassed());
    }

    @Test
    public void notApplicableForPodfile() {
        NugetInspectorResolver nugetInspectorManager = null;
        NugetInspectorExtractor nugetInspectorExtractor = null;
        NugetInspectorOptions nugetInspectorOptions = null;

        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = MockFileFinder.withFileNamed("podfile.lock");

        NugetSolutionDetectable detectable = new NugetSolutionDetectable(environment, fileFinder, nugetInspectorManager, nugetInspectorExtractor, nugetInspectorOptions);

        assertFalse(detectable.applicable().getPassed());
    }
}

