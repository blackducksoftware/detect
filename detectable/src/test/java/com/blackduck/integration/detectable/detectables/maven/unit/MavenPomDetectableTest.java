package com.blackduck.integration.detectable.detectables.maven.unit;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.executable.resolver.MavenResolver;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenCliExtractor;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenCliExtractorOptions;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenPomDetectable;
import com.blackduck.integration.detectable.detectables.maven.parsing.MavenProjectInspectorDetectable;
import com.blackduck.integration.detectable.util.MockDetectableEnvironment;
import com.blackduck.integration.detectable.util.MockFileFinder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MavenPomDetectableTest {

    @Test
    public void testApplicable_defaultPomXMLName() {
        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = MockFileFinder.withFileNamed("pom.xml");
        MavenResolver mavenResolver = null;
        MavenCliExtractor mavenCliExtractor = null;
        MavenCliExtractorOptions mavenCliExtractorOptions = null;
        MavenProjectInspectorDetectable mavenProjectInspectorDetectable = null;
        File pomFile = Mockito.mock(File.class);


        MavenPomDetectable detectable = new MavenPomDetectable(environment, fileFinder, mavenResolver, mavenCliExtractor, mavenCliExtractorOptions, mavenProjectInspectorDetectable);
        assertTrue(detectable.applicable().getPassed());
    }

    @Test
    public void testApplicable_nondefaultPomXMLName() {
        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = MockFileFinder.withFileNamed("custom-pom.xml");
        MavenResolver mavenResolver = null;
        MavenCliExtractor mavenCliExtractor = null;
        MavenCliExtractorOptions mavenCliExtractorOptions = null;
        MavenProjectInspectorDetectable mavenProjectInspectorDetectable = null;
        File pomFile = Mockito.mock(File.class);


        MavenPomDetectable detectable = new MavenPomDetectable(environment, fileFinder, mavenResolver, mavenCliExtractor, mavenCliExtractorOptions, mavenProjectInspectorDetectable);
        assertTrue(detectable.applicable().getPassed());
    }

    @Test
    public void testApplicable_invalidPomXMLName() {
        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = MockFileFinder.withFileNamed("invalid-pom-name.xml");
        MavenResolver mavenResolver = null;
        MavenCliExtractor mavenCliExtractor = null;
        MavenCliExtractorOptions mavenCliExtractorOptions = null;
        MavenProjectInspectorDetectable mavenProjectInspectorDetectable = null;
        File pomFile = Mockito.mock(File.class);


        MavenPomDetectable detectable = new MavenPomDetectable(environment, fileFinder, mavenResolver, mavenCliExtractor, mavenCliExtractorOptions, mavenProjectInspectorDetectable);
        assertFalse(detectable.applicable().getPassed());
    }


}
