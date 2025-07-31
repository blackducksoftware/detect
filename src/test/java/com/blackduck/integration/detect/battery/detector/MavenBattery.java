package com.blackduck.integration.detect.battery.detector;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.battery.util.DetectorBatteryTestRunner;
import com.blackduck.integration.detect.configuration.DetectProperties;

@Tag("battery")
public class MavenBattery {
    private static final String MAVEN_OUTPUT_RESOURCE = "maven-dependencytree.xout";

    @Test
    void mavenFromProperty() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("maven-property", "maven-cli/simple");
        test.executableFromResourceFiles(DetectProperties.DETECT_MAVEN_PATH, MAVEN_OUTPUT_RESOURCE);
        test.sourceDirectoryNamed("linux-maven");
        test.sourceFileNamed("pom.xml");
        test.git("https://github.com/BlackDuckCoPilot/example-maven-travis", "master");
        test.expectBdioResources();
        test.run();
    }

    @Test
    void mavenInconsistentDependencies() {
        // Tests the case where maven, for a single component that occurs in multiple subprojects,
        // provides different lists of dependencies for the component.
        // Detect should aggregate these dependencies when generating its dependency list for the
        // component for the bdio.
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("maven-property", "maven-cli/inconsistent-definitions");
        test.executableFromResourceFiles(DetectProperties.DETECT_MAVEN_PATH, MAVEN_OUTPUT_RESOURCE);
        test.sourceDirectoryNamed("linux-maven");
        test.sourceFileNamed("pom.xml");
        test.git("https://github.com/BlackDuckCoPilot/example-maven-travis", "master");
        test.expectBdioResources();
        test.run();
    }

    @Test
    void mavenFromSourceFile() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("maven-wrapper", "maven-cli/simple");
        test.executableSourceFileFromResourceFiles("mvnw.cmd", "mvnw", MAVEN_OUTPUT_RESOURCE);
        test.sourceDirectoryNamed("linux-maven");
        test.sourceFileNamed("pom.xml");
        test.git("https://github.com/BlackDuckCoPilot/example-maven-travis", "master");
        test.expectBdioResources();
        test.run();
    }

    @Test
    void mavenCustomPomName() {
        DetectorBatteryTestRunner test = new DetectorBatteryTestRunner("maven-wrapper", "maven-cli/simple");
        test.executableSourceFileFromResourceFiles("mvnw.cmd", "mvnw", MAVEN_OUTPUT_RESOURCE);
        test.sourceDirectoryNamed("linux-maven");
        test.sourceFileNamed("custom-pom.xml");
//        test.property("detect.maven.build.command", "-f custom-pom.xml");
        test.git("https://github.com/BlackDuckCoPilot/example-maven-travis", "master");
        test.expectBdioResources();
        test.run();
    }
}

