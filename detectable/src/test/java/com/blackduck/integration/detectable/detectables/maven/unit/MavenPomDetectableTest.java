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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class MavenPomDetectableTest {

    @ParameterizedTest
    @CsvSource({
            "pom.xml, true",
            "custom-pom.xml, true",
            "invalid-pom-name.xml, false"
    })
    void testApplicable_withVariousPomNames(String fileName, boolean expectedResult) {
        DetectableEnvironment environment = MockDetectableEnvironment.empty();
        FileFinder fileFinder = MockFileFinder.withFileNamed(fileName);
        MavenResolver mavenResolver = null;
        MavenCliExtractor mavenCliExtractor = null;
        MavenCliExtractorOptions mavenCliExtractorOptions = null;
        MavenProjectInspectorDetectable mavenProjectInspectorDetectable = null;

        MavenPomDetectable detectable = new MavenPomDetectable(
                environment, fileFinder, mavenResolver, mavenCliExtractor,
                mavenCliExtractorOptions, mavenProjectInspectorDetectable
        );

        assertEquals(expectedResult, detectable.applicable().getPassed());
    }

}
