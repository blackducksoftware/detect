package com.blackduck.integration.detectable.detectables.gradle.functional;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.detectable.annotations.UnitTest;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.gradle.inspection.model.GradleReport;
import com.blackduck.integration.detectable.detectables.gradle.inspection.parse.GradleReportParser;
import com.blackduck.integration.detectable.detectables.gradle.inspection.parse.GradleReportTransformer;
import com.blackduck.integration.detectable.util.FunctionalTestFiles;
import com.blackduck.integration.detectable.util.graph.MavenGraphAssert;

/**
 * Verifies that rich version state does not carry over between configurations.
 * Each configuration must resolve its own transitives independently.
 */
@UnitTest
public class GradleCrossConfigRichVersionBugTest {

    @Test
    void eachConfigurationResolvesTransitivesIndependently() {
        GradleReportParser parser = new GradleReportParser();
        Optional<GradleReport> report = parser.parseReport(
            FunctionalTestFiles.asFile("/gradle/rich_version_cross_config_depth0_dependencyGraph.txt"));
        Assertions.assertTrue(report.isPresent());

        GradleReportTransformer transformer = new GradleReportTransformer(EnumListFilter.excludeNone());
        CodeLocation codeLocation = transformer.transform(report.get());
        DependencyGraph graph = codeLocation.getDependencyGraph();
        MavenGraphAssert graphAssert = new MavenGraphAssert(graph);

        graphAssert.hasRootDependency("com.example:foo:1.0");
        graphAssert.hasRootDependency("com.example:foo:2.0");

        ExternalId foo10 = graphAssert.hasDependency("com.example:foo:1.0");
        ExternalId bar10 = graphAssert.hasDependency("com.example:bar:1.0");
        graphAssert.hasParentChildRelationship(foo10, bar10);

        ExternalId foo20 = graphAssert.hasDependency("com.example:foo:2.0");
        ExternalId bar20 = graphAssert.hasDependency("com.example:bar:2.0");
        graphAssert.hasParentChildRelationship(foo20, bar20);
    }
}
