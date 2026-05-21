package com.blackduck.integration.detectable.detectables.setuptools.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsParsedResult;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsPyParser;
import com.blackduck.integration.detectable.python.util.PythonDependency;

public class SetupToolsPyParserTest {
    
    @Test
    public void testLoad() throws IOException {
        String pyContent = "from setuptools import setup\n\nsetup(\ninstall_requires=[\n'requests==2.31.0',\n],\n)";
        Path tempFile = Files.createTempFile("setup", ".py");
        Files.write(tempFile, pyContent.getBytes());

        TomlParseResult result = Toml.parse(pyContent);

        SetupToolsPyParser pyParser = new SetupToolsPyParser(result);
        List<String> dependencies = pyParser.load(tempFile.toString());

        assertEquals(1, dependencies.size());
        assertTrue(dependencies.contains("requests==2.31.0"));

        Files.delete(tempFile); // delete the temporary file
    }
    
    @Test
    public void testParse() throws IOException {
        String pyContent = "from setuptools import setup\n\nsetup(\ninstall_requires=[\n'requests==2.31.0',\n],\n)";
        Path pyFile = Files.createTempFile("setup", ".py");
        Files.write(pyFile, pyContent.getBytes());
        
        String tomlContent = "[build-system]\nrequires = [\"setuptools\"]";

        TomlParseResult result = Toml.parse(tomlContent);

        SetupToolsPyParser pyParser = new SetupToolsPyParser(result);
        pyParser.load(pyFile.toString());
        SetupToolsParsedResult parsedResult = pyParser.parse();

        assertEquals(1, parsedResult.getDirectDependencies().size());
        
        for (PythonDependency dependency : parsedResult.getDirectDependencies()) {
            assertEquals("requests", dependency.getName());
            assertEquals("2.31.0", dependency.getVersion());
        }

        Files.delete(pyFile); // delete the temporary file
    }

    @Test
    public void testParseWithEnvironmentMarkers() throws IOException {
        String pyContent = "from setuptools import setup\n\nsetup(\ninstall_requires=[\n"
            + "\"contextvars;python_version<'3.7'\",\n"
            + "\"requests>=2.31.0\",\n"
            + "\"dataclasses;python_version<'3.7'\",\n"
            + "],\n)";
        Path pyFile = Files.createTempFile("setup", ".py");
        Files.write(pyFile, pyContent.getBytes());

        String tomlContent = "[build-system]\nrequires = [\"setuptools\"]";
        TomlParseResult result = Toml.parse(tomlContent);

        SetupToolsPyParser pyParser = new SetupToolsPyParser(result);
        pyParser.load(pyFile.toString());
        SetupToolsParsedResult parsedResult = pyParser.parse();

        List<PythonDependency> deps = parsedResult.getDirectDependencies();
        assertEquals(3, deps.size());

        // IDETECT-5090: PEP 508 environment markers must be stripped from the package name.
        // Before the fix, "contextvars;python_version<'3.7'" produced "contextvars;python_version"
        // as the package name instead of "contextvars".
        PythonDependency contextvars = deps.get(0);
        assertEquals("contextvars", contextvars.getName(), "Environment marker should be stripped; got: " + contextvars.getName());
        assertFalse(contextvars.getName().contains(";"), "Package name must not contain marker separator ';'");
        assertTrue(contextvars.isConditional(), "Dependency with environment marker should be marked conditional");

        PythonDependency requests = deps.get(1);
        assertEquals("requests", requests.getName());
        assertEquals("2.31.0", requests.getVersion());
        assertFalse(requests.isConditional(), "Dependency without environment marker should not be conditional");

        PythonDependency dataclasses = deps.get(2);
        assertEquals("dataclasses", dataclasses.getName(), "Environment marker should be stripped; got: " + dataclasses.getName());
        assertFalse(dataclasses.getName().contains(";"), "Package name must not contain marker separator ';'");
        assertTrue(dataclasses.isConditional(), "Dependency with environment marker should be marked conditional");

        Files.delete(pyFile);
    }
}
