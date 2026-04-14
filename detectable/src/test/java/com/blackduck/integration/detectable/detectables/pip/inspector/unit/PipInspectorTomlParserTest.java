package com.blackduck.integration.detectable.detectables.pip.inspector.unit;

import com.blackduck.integration.detectable.detectables.pip.inspector.parser.PipInspectorTomlParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tomlj.TomlParseResult;

import java.io.File;

public class PipInspectorTomlParserTest {

    PipInspectorTomlParser pipInspectorTomlParser;

    @BeforeEach
    public void init() {
        pipInspectorTomlParser = new PipInspectorTomlParser();
    }

    @Test
    public void testParsePipInspectorInvalidTomlFile() {
        File pyprojectTomlFile = new File("src/test/resources/detectables/unit/pip/native/invalid-toml/pyproject.toml");
        TomlParseResult parseResult = pipInspectorTomlParser.parseToml(pyprojectTomlFile);

        Boolean validFile = pipInspectorTomlParser.checkIfProjectKeyExists(parseResult);

        Assertions.assertFalse(validFile);
    }

    @Test
    public void testParsePipInspectorValidTomlFile() {
        File pyprojectTomlFile = new File("src/test/resources/detectables/unit/pip/native/tomlfile/pyproject.toml");
        TomlParseResult parseResult = pipInspectorTomlParser.parseToml(pyprojectTomlFile);

        Boolean validFile = pipInspectorTomlParser.checkIfProjectKeyExists(parseResult);

        Assertions.assertTrue(validFile);
    }
}
