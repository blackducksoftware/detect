package com.blackduck.integration.detectable.detectables.conan.functional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.conan.cli.config.ConanCliOptions;
import com.blackduck.integration.detectable.detectables.conan.cli.config.ConanDependencyType;
import com.blackduck.integration.detectable.detectables.conan.cli.parser.conan2.ConanGraphInfoParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.util.FunctionalTestFiles;
import com.blackduck.integration.detectable.util.GraphCompare;

public class ConanGraphInfoParserTest {
    @Test
    public void testExtract() throws IOException, DetectableException {
        File conanInfoOutputFile = FunctionalTestFiles.asFile("/conan/cli/conan2_graph_info.txt");
        String conanGraphInfoOutput = FileUtils.readFileToString(conanInfoOutputFile, StandardCharsets.UTF_8);
        EnumListFilter<ConanDependencyType> dependencyTypeFilter = EnumListFilter.excludeNone();
        ConanCliOptions cliOptions = new ConanCliOptions(null, "", dependencyTypeFilter, false);
        ConanGraphInfoParser parser = new ConanGraphInfoParser(new Gson(), cliOptions, new ExternalIdFactory());
        Extraction extraction = parser.parse(conanGraphInfoOutput);

        assertTrue(extraction.isSuccess());
        assertEquals("project-name", extraction.getProjectName());
        assertEquals("0.17", extraction.getProjectVersion());

        DependencyGraph actualDependencyGraph = extraction.getCodeLocations().get(0).getDependencyGraph();
        GraphCompare.assertEqualsResource("/conan/cli/conan2_graph_result.json", actualDependencyGraph);
    }

    @Test
    public void testExtractPreferLongFormIds() throws IOException, DetectableException {
        File conanInfoOutputFile = FunctionalTestFiles.asFile("/conan/cli/conan2_graph_info.txt");
        String conanGraphInfoOutput = FileUtils.readFileToString(conanInfoOutputFile, StandardCharsets.UTF_8);
        EnumListFilter<ConanDependencyType> dependencyTypeFilter = EnumListFilter.excludeNone();
        ConanCliOptions cliOptions = new ConanCliOptions(null, "", dependencyTypeFilter, true);
        ConanGraphInfoParser parser = new ConanGraphInfoParser(new Gson(), cliOptions, new ExternalIdFactory());
        Extraction extraction = parser.parse(conanGraphInfoOutput);

        assertTrue(extraction.isSuccess());
        assertEquals("project-name", extraction.getProjectName());
        assertEquals("0.17", extraction.getProjectVersion());

        DependencyGraph actualDependencyGraph = extraction.getCodeLocations().get(0).getDependencyGraph();
        GraphCompare.assertEqualsResource("/conan/cli/conan2_graph_longIds_result.json", actualDependencyGraph);
    }

    @Test
    public void testExtractExcludeBuild() throws IOException, DetectableException {
        File conanInfoOutputFile = FunctionalTestFiles.asFile("/conan/cli/conan2_graph_info.txt");
        String conanGraphInfoOutput = FileUtils.readFileToString(conanInfoOutputFile, StandardCharsets.UTF_8);
        EnumListFilter<ConanDependencyType> dependencyTypeFilter = EnumListFilter.fromExcluded(ConanDependencyType.BUILD);
        ConanCliOptions cliOptions = new ConanCliOptions(null, "", dependencyTypeFilter, false);
        ConanGraphInfoParser parser = new ConanGraphInfoParser(new Gson(), cliOptions, new ExternalIdFactory());
        Extraction extraction = parser.parse(conanGraphInfoOutput);

        assertTrue(extraction.isSuccess());
        assertEquals("project-name", extraction.getProjectName());
        assertEquals("0.17", extraction.getProjectVersion());

        DependencyGraph actualDependencyGraph = extraction.getCodeLocations().get(0).getDependencyGraph();
        GraphCompare.assertEqualsResource("/conan/cli/conan2_graph_excludeBuild_result.json", actualDependencyGraph);
    }

    @Test
    public void testExtractExcludeTest() throws IOException, DetectableException {
        File conanInfoOutputFile = FunctionalTestFiles.asFile("/conan/cli/conan2_graph_info.txt");
        String conanGraphInfoOutput = FileUtils.readFileToString(conanInfoOutputFile, StandardCharsets.UTF_8);
        EnumListFilter<ConanDependencyType> dependencyTypeFilter = EnumListFilter.fromExcluded(ConanDependencyType.TEST);
        ConanCliOptions cliOptions = new ConanCliOptions(null, "", dependencyTypeFilter, false);
        ConanGraphInfoParser parser = new ConanGraphInfoParser(new Gson(), cliOptions, new ExternalIdFactory());
        Extraction extraction = parser.parse(conanGraphInfoOutput);

        assertTrue(extraction.isSuccess());
        assertEquals("project-name", extraction.getProjectName());
        assertEquals("0.17", extraction.getProjectVersion());

        DependencyGraph actualDependencyGraph = extraction.getCodeLocations().get(0).getDependencyGraph();
        GraphCompare.assertEqualsResource("/conan/cli/conan2_graph_excludeTest_result.json", actualDependencyGraph);
    }

    @Test
    public void testExtractParseException() throws IOException, DetectableException {
        String conanGraphInfoOutput = "invalid JSON";
        EnumListFilter<ConanDependencyType> dependencyTypeFilter = EnumListFilter.fromExcluded(ConanDependencyType.BUILD);
        ConanCliOptions cliOptions = new ConanCliOptions(null, "", dependencyTypeFilter, false);
        ConanGraphInfoParser parser = new ConanGraphInfoParser(new Gson(), cliOptions, new ExternalIdFactory());
        Extraction extraction = parser.parse(conanGraphInfoOutput);

        assertFalse(extraction.isSuccess());
        assertEquals("Unable to parse conan graph info", extraction.getDescription());
    }

    @Test
    public void testExtractNoRootNode() throws IOException, DetectableException {
        String conanGraphInfoOutput = "{\"graph\": {\"nodes\": {}}}";
        EnumListFilter<ConanDependencyType> dependencyTypeFilter = EnumListFilter.fromExcluded(ConanDependencyType.BUILD);
        ConanCliOptions cliOptions = new ConanCliOptions(null, "", dependencyTypeFilter, false);
        ConanGraphInfoParser parser = new ConanGraphInfoParser(new Gson(), cliOptions, new ExternalIdFactory());
        Extraction extraction = parser.parse(conanGraphInfoOutput);

        assertFalse(extraction.isSuccess());
        assertEquals("No root node was found in the conan graph info", extraction.getDescription());
    }
}
