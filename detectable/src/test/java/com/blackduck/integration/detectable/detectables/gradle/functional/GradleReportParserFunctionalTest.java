package com.blackduck.integration.detectable.detectables.gradle.functional;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang3.SystemUtils;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.google.gson.GsonBuilder;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.detectable.annotations.UnitTest;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.gradle.inspection.GradleConfigurationType;
import com.blackduck.integration.detectable.detectables.gradle.inspection.model.GradleReport;
import com.blackduck.integration.detectable.detectables.gradle.inspection.parse.GradleReportParser;
import com.blackduck.integration.detectable.detectables.gradle.inspection.parse.GradleReportTransformer;
import com.blackduck.integration.detectable.util.FunctionalTestFiles;
import com.blackduck.integration.detectable.util.graph.GraphAssert;
import com.blackduck.integration.detectable.util.graph.MavenGraphAssert;

@UnitTest
public class GradleReportParserFunctionalTest {

    @Test
    void extractCodeLocationTest() throws JSONException, IOException {
        Assumptions.assumeFalse(SystemUtils.IS_OS_WINDOWS); //Does not work on Microsoft Windows due to path issues.

        GradleReportParser gradleReportParser = new GradleReportParser();
        Optional<GradleReport> gradleReport = gradleReportParser.parseReport(FunctionalTestFiles.asFile("/gradle/projectname_depth0_dependencyGraph.txt"));
        Assertions.assertTrue(gradleReport.isPresent());
        GradleReportTransformer transformer = new GradleReportTransformer(EnumListFilter.excludeNone());
        CodeLocation codeLocation = transformer.transform(gradleReport.get());
        Assertions.assertNotNull(codeLocation);

        Assertions.assertEquals("hub-detect", gradleReport.get().getProjectName());
        Assertions.assertEquals("2.0.0-SNAPSHOT", gradleReport.get().getProjectVersionName());

        String actual = new GsonBuilder().setPrettyPrinting().create().toJson(codeLocation);
        JSONAssert.assertEquals(FunctionalTestFiles.asString("/gradle/dependencyGraph-expected.json"), actual, false);
    }

    @Test
    void complexTest() {
        Optional<CodeLocation> codeLocation = buildCodeLocation("/gradle/complex_depth0_dependencyGraph.txt", true);
        Assertions.assertTrue(codeLocation.isPresent());
        DependencyGraph graph = codeLocation.get().getDependencyGraph();

        MavenGraphAssert graphAssert = new MavenGraphAssert(graph);
        graphAssert.hasDependency("non-project:with-nested:1.0.0");
        graphAssert.hasDependency("solo:component:4.12");
        graphAssert.hasDependency("some.group:child:2.2.2");
        graphAssert.hasDependency("terminal:child:6.2.3");

        // TODO: Get this to work in a sensible way
        //        graphAssert.noDependency("child-project");
        //        graphAssert.noDependency("nested-parent");
        //        graphAssert.noDependency("spring-webflux");
        //        graphAssert.noDependency("spring-beans");
        //        graphAssert.noDependency("spring-core");
        //        graphAssert.noDependency("spring-web");
        //        graphAssert.noDependency("should-suppress");

        graphAssert.hasRootDependency("solo:component:4.12");
        graphAssert.hasRootDependency("non-project:with-nested:1.0.0");
        graphAssert.hasRootDependency("some.group:parent:5.0.0");
        graphAssert.hasRootDependency("terminal:child:6.2.3");

        ExternalId parent = graphAssert.hasDependency("some.group:parent:5.0.0");
        ExternalId child = graphAssert.hasDependency("some.group:child:2.2.2");
        graphAssert.hasParentChildRelationship(parent, child);
    }

    private Optional<CodeLocation> buildCodeLocation(String resource, boolean includeUnresolvedConfigurations) {
        File file = FunctionalTestFiles.asFile(resource);
        GradleReportParser gradleReportParser = new GradleReportParser();
        EnumListFilter<GradleConfigurationType> enumListFilter = EnumListFilter.excludeNone();
        if (!includeUnresolvedConfigurations) {
            enumListFilter = EnumListFilter.fromExcluded(GradleConfigurationType.UNRESOLVED);
        }
        GradleReportTransformer gradleReportTransformer = new GradleReportTransformer(enumListFilter);

        return gradleReportParser.parseReport(file)
            .map(gradleReportTransformer::transform);
    }

    @Test
    void testImplementationsGraph() {
        Optional<CodeLocation> codeLocation = buildCodeLocation("/gradle/gradle_implementations_depth0_dependencyGraph.txt", true);
        Assertions.assertTrue(codeLocation.isPresent());

        DependencyGraph dependencyGraph = codeLocation.get().getDependencyGraph();
        GraphAssert graphAssert = new GraphAssert(Forge.MAVEN, dependencyGraph);
        graphAssert.hasRootSize(7);

        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(codeLocation.get()));
    }

    @Test
    void testUnresolvedConfigurations() {
        Optional<CodeLocation> codeLocation = buildCodeLocation("/gradle/gradle_implementations_depth0_dependencyGraph.txt", false);
        Assertions.assertTrue(codeLocation.isPresent());

        DependencyGraph dependencyGraph = codeLocation.get().getDependencyGraph();
        GraphAssert graphAssert = new GraphAssert(Forge.MAVEN, dependencyGraph);
        graphAssert.hasRootSize(0);

        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(codeLocation.get()));
    }

    @Test
    void testConstraintsAreFiltered() {
        Optional<CodeLocation> codeLocation = buildCodeLocation("/gradle/constraints_dependencyGraph.txt", true);
        Assertions.assertTrue(codeLocation.isPresent());
        DependencyGraph graph = codeLocation.get().getDependencyGraph();

        MavenGraphAssert graphAssert = new MavenGraphAssert(graph);
        graphAssert.hasRootSize(1);
        graphAssert.hasRootDependency("com.google.code.gson:gson:2.8.9");
        graphAssert.hasNoDependency("org.apache.logging.log4j:log4j-api:2.17.1");
        graphAssert.hasNoDependency("org.apache.commons:commons-collections4:4.4");
        graphAssert.hasNoDependency("javax.servlet:javax.servlet-api:3.1.0");
    }
}
