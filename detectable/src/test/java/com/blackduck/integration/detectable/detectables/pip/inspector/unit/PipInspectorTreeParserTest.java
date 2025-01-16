package com.blackduck.integration.detectable.detectables.pip.inspector.unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.annotations.UnitTest;
import com.blackduck.integration.detectable.detectables.pip.inspector.model.NameVersionCodeLocation;
import com.blackduck.integration.detectable.detectables.pip.inspector.parser.PipInspectorTreeParser;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;

@UnitTest
public class PipInspectorTreeParserTest {
    private PipInspectorTreeParser parser;

    @BeforeEach
    public void init() {
        parser = new PipInspectorTreeParser(new ExternalIdFactory());
    }

    @Test
    public void validTest() {
        List<String> pipInspectorOutput = Arrays.asList(
            "projectName==projectVersionName",
            "   with-dashes==1.0.0",
            "   Uppercase==2.0.0",
            "      child==3.0.0",
            "   test==4.0.0"
        );

        Optional<NameVersionCodeLocation> validParse = parser.parse(pipInspectorOutput, "");
        Assertions.assertTrue(validParse.isPresent());
        Assertions.assertEquals("projectName", validParse.get().getProjectName());
        Assertions.assertEquals("projectVersionName", validParse.get().getProjectVersion());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.PYPI, validParse.get().getCodeLocation().getDependencyGraph());
        graphAssert.hasRootDependency("with-dashes", "1.0.0");
        graphAssert.hasRootDependency("Uppercase", "2.0.0");
        graphAssert.hasRootDependency("test", "4.0.0");
        graphAssert.hasParentChildRelationship("Uppercase", "2.0.0", "child", "3.0.0");

        graphAssert.hasRootSize(3);
    }

    @Test
    public void packageNotFoundTest() {
        List<String> pipInspectorOutput = Arrays.asList(
            "--scikit-learn",
            "projectName==projectVersionName",
            "   with-dashes==1.0.0",
            "   Uppercase==2.0.0",
            "      child==3.0.0",
            "   test==4.0.0"
        );

        Optional<NameVersionCodeLocation> result = parser.parse(pipInspectorOutput, "");
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void invalidParseTest() {
        List<String> invalidText = new ArrayList<>();
        invalidText.add("i am not a valid file");
        invalidText.add("the status should be optional.empty()");
        Optional<NameVersionCodeLocation> invalidParse = parser.parse(invalidText, "");
        Assertions.assertFalse(invalidParse.isPresent());
    }

    @Test
    public void errorTest() {
        List<String> invalidText = new ArrayList<>();
        invalidText.add(PipInspectorTreeParser.UNKNOWN_PACKAGE_PREFIX + "probably_an_internal_dependency_PY");
        invalidText.add(PipInspectorTreeParser.UNPARSEABLE_REQUIREMENTS_PREFIX + "/not/a/real/path/encrypted/requirements.txt");
        invalidText.add(PipInspectorTreeParser.UNKNOWN_REQUIREMENTS_PREFIX + "/not/a/real/path/requirements.txt");
        Optional<NameVersionCodeLocation> invalidParse = parser.parse(invalidText, "");
        Assertions.assertFalse(invalidParse.isPresent());
    }
}
