package com.blackduck.integration.detectable.detectables.maven.unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenCodeLocationPackager;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenParseResult;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;

public class MavenComplexOutputTest {
    
    private ExternalIdFactory externalIdFactory;
    private MavenCodeLocationPackager packager;
    private ExternalId hubCommon;
    private ExternalId integrationCommon;
    private ExternalId hubCommonRest;
    
    
    private enum TestCase {
        DEFAULT_CASE, IDETECT3228CASE1, IDETECT3228CASE2;
    }
    
    private List<String> getInputHeader() {
        return new ArrayList<>(Arrays.asList(
                "[INFO] Reactor Build Order:",
                "[INFO] ",
                "[INFO] generic-parent                                                     [pom]",
                "[INFO] MX Minimal Viable Runtime :: Parent POM                            [pom]",
                "[INFO] generic.boot:boot-client:jar:v3.1.42                               [jar]",
                "[INFO] MX FileServer :: Parent POM                                        [pom]",
                "[INFO] generic.fileserver:fileserver-client:jar:v3.1.42                   [jar]",
                "[INFO] REST Service Framework :: Parent                                   [pom]",
                "[INFO] REST Service Framework :: Logging                                  [jar]",
                "[INFO] common-parent                                                      [pom]",
                "[INFO] -------------------< generic.integration:integration >------------------",
                "[INFO] Building generic.integration:integration:pom:v3.1.42 v3.1.42   [70/2593]",
                "[INFO] --------------------------------[ pom ]---------------------------------",
                "[WARNING] The POM for org.eclipse.m2e:lifecycle-mapping:jar:1.0.0 is missing, no dependency information available",
                "[WARNING] Failed to retrieve plugin descriptor for org.eclipse.m2e:lifecycle-mapping:1.0.0: Plugin org.eclipse.m2e:lifecycle-mapping:1.0.0 or one of its dependencies could not be resolved: Failure to find org.eclipse.m2e:lifecycle-mapping:jar:1.0.0 in http://nexus-dev/nexus/content/groups/mx-artifacts/ was cached in the local repository, resolution will not be reattempted until the update interval of mx-artifacts-plugin has elapsed or updates are forced",
                "[INFO]",
                "7275 [main] [INFO] --- maven-dependency-plugin:2.10:tree (default-cli) @ hub-teamcity-common ---"
        ));
    }

    @BeforeEach
    void init() {
        externalIdFactory = new ExternalIdFactory();
        packager = new MavenCodeLocationPackager(externalIdFactory);
        hubCommon = externalIdFactory.createMavenExternalId("com.blackducksoftware.integration", "hub-common", "13.1.2");
        integrationCommon = externalIdFactory.createMavenExternalId("com.blackducksoftware.integration", "integration-common", "6.0.2");
        hubCommonRest = externalIdFactory.createMavenExternalId("com.blackducksoftware.integration", "hub-common-rest", "2.1.3");
    }   

    private List<String> getInput(TestCase testCase) {
        List<String> input = getInputHeader();
        switch(testCase) {
            case IDETECT3228CASE1:
                input.addAll(Arrays.asList(
                    "7450 [main] [INFO] com.blackducksoftware.integration:hub-teamcity-common:jar:3.2.0-SNAPSHOT:compile",
                    "7509 [main] [INFO] +- com.blackducksoftware.integration:hub-common:jar:13.1.2:compile",
                    "7560 [main] [INFO] |  +- com.blackducksoftware.integration:hub-common-rest:jar:2.1.3:compile",
                    "7638 [main] [INFO] |  |  +- com.blackducksoftware.integration:integration-common:jar:6.0.2:compile"
                ));
                break;
            case IDETECT3228CASE2:
                input.addAll(Arrays.asList(
                    "7450 [main] [INFO] com.blackducksoftware.integration:hub-teamcity-common:jar:3.2.0-SNAPSHOT |com.blackducksoftware.integration:hub-teamcity-common|",
                    "7509 [main] [INFO] +- com.blackducksoftware.integration:hub-common:jar:13.1.2:compile |com.blackducksoftware.integration:hub-teamcity-common|",
                    "7560 [main] [INFO] |  +- com.blackducksoftware.integration:hub-common-rest:jar:2.1.3:compile |com.blackducksoftware.integration:hub-teamcity-common|",
                    "7638 [main] [INFO] |  |  +- com.blackducksoftware.integration:integration-common:jar:6.0.2:compile |com.blackducksoftware.integration:hub-teamcity-common|"
                ));
                break;
            default:
                input.addAll(Arrays.asList(
                    "7450 [main] [INFO] com.blackducksoftware.integration:hub-teamcity-common:jar:3.2.0-SNAPSHOT",
                    "7509 [main] [INFO] +- com.blackducksoftware.integration:hub-common:jar:13.1.2:compile",
                    "7560 [main] [INFO] |  +- com.blackducksoftware.integration:hub-common-rest:jar:2.1.3:compile",
                    "7638 [main] [INFO] |  |  +- com.blackducksoftware.integration:integration-common:jar:6.0.2:compile"
                ));
        }
        return input;
    }

    @Test
    public void test() {
        testUtility(TestCase.DEFAULT_CASE);
    }
    
    @Test
    public void testIDETECT3228CaseOne() {
        testUtility(TestCase.IDETECT3228CASE1);
    }
    
    @Test
    public void testIDETECT3228CaseTwo() {
        testUtility(TestCase.IDETECT3228CASE2);
    }
    
    private void testUtility(TestCase testCase) {
        List<MavenParseResult> results = packager.extractCodeLocations(
                "",
                getInput(testCase),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap()
        );
        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.MAVEN, results.get(0).getCodeLocation().getDependencyGraph());
        graphAssert.hasRootSize(1);
        graphAssert.hasRootDependency(hubCommon);
        graphAssert.hasParentChildRelationship(hubCommonRest, integrationCommon);
        graphAssert.hasParentChildRelationship(hubCommon, hubCommonRest);
    }
}
