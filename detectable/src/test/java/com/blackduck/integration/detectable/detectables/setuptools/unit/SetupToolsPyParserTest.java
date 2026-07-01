package com.blackduck.integration.detectable.detectables.setuptools.unit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsParsedResult;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsPyParser;
import com.blackduck.integration.detectable.python.util.PythonDependency;

import static org.junit.jupiter.api.Assertions.*;

public class SetupToolsPyParserTest {
    private Path createTempSetupPy(Path tempDir, String pyContent) throws IOException {
        Path tempFile = Files.createTempFile(tempDir, "setup", ".py");
        Files.write(tempFile, pyContent.getBytes());
        return tempFile;
    }

    private TomlParseResult createSetuptoolsTomlResult() {
        return Toml.parse("[build-system]\nrequires = [\"setuptools\"]");
    }
    
    @Test
    public void testLoadSingleDependencyFromMultilineList(@TempDir Path tempDir) throws IOException, DetectableException {
        String pyContent = "from setuptools import setup\n\nsetup(\ninstall_requires=[\n'requests==2.31.0',\n],\n)";
        Path tempFile = createTempSetupPy(tempDir, pyContent);

        TomlParseResult result = Toml.parse(pyContent);

        SetupToolsPyParser pyParser = new SetupToolsPyParser(result);
        List<String> dependencies = pyParser.load(tempFile.toString());

        assertEquals(1, dependencies.size());
        assertTrue(dependencies.contains("requests==2.31.0"));
    }

    @Test
    public void testLoadWithInlineOneLineList(@TempDir Path tempDir) throws IOException, DetectableException {
        String pyContent = "from setuptools import setup\n\nsetup(install_requires=[\"requests>=2.31.0\", \"numpy<2\"])";
        Path tempFile = createTempSetupPy(tempDir, pyContent);

        TomlParseResult result = createSetuptoolsTomlResult();

        SetupToolsPyParser pyParser = new SetupToolsPyParser(result);
        List<String> dependencies = pyParser.load(tempFile.toString());

        assertEquals(2, dependencies.size());
        assertTrue(dependencies.contains("requests>=2.31.0"));
        assertTrue(dependencies.contains("numpy<2"));
    }

    @Test
    public void testLoadWithMultipleItemsOnOneLineInsideList(@TempDir Path tempDir) throws IOException, DetectableException {
        String pyContent = "from setuptools import setup\n\nsetup(\ninstall_requires=[\n\"requests>=2.31.0\", \"numpy<2\", \"urllib3<3\",\n],\n)";
        Path tempFile = createTempSetupPy(tempDir, pyContent);

        TomlParseResult result = createSetuptoolsTomlResult();

        SetupToolsPyParser pyParser = new SetupToolsPyParser(result);
        List<String> dependencies = pyParser.load(tempFile.toString());

        assertEquals(3, dependencies.size());
        assertTrue(dependencies.contains("requests>=2.31.0"));
        assertTrue(dependencies.contains("numpy<2"));
        assertTrue(dependencies.contains("urllib3<3"));
    }

    @Test
    public void testLoadWithVariableReferenceInstallRequiresUnsupported(@TempDir Path tempDir) throws IOException {
        String pyContent = "from setuptools import setup\n\nrequirements=['requests>=2.31.0']\n\nsetup(\ninstall_requires=requirements,\n)";
        Path tempFile = createTempSetupPy(tempDir, pyContent);

        TomlParseResult result = createSetuptoolsTomlResult();
        SetupToolsPyParser pyParser = new SetupToolsPyParser(result);

        DetectableException exception = assertThrows(
                DetectableException.class,
                () -> pyParser.load(tempFile.toString())
        );
        assertTrue(exception.getMessage().contains("install_requires must be a literal Python list"));
    }

    @Test
    public void testLoadIgnoresCommentedInstallRequiresText(@TempDir Path tempDir) throws IOException, DetectableException {
        String pyContent = "from setuptools import setup\n"
            + "# this is a comment install_requires=[\"requests>=1.0.0\"]\n"
            + "setup(\n"
            + "    install_requires=[\"numpy<2\"],\n"
            + ")";
        Path tempFile = createTempSetupPy(tempDir, pyContent);

        TomlParseResult result = createSetuptoolsTomlResult();

        SetupToolsPyParser pyParser = new SetupToolsPyParser(result);
        List<String> dependencies = pyParser.load(tempFile.toString());

        assertEquals(1, dependencies.size());
        assertTrue(dependencies.contains("numpy<2"));
        assertFalse(dependencies.contains("requests>=1.0.0"));
    }

    @Test
    public void testLoadDoesNotParseInstallRequiresFromCommentOnlyLine(@TempDir Path tempDir) throws IOException, DetectableException {
        String pyContent = "from setuptools import setup\n"
            + "# install_requires=[\"requests>=1.0.0\"]\n"
            + "setup(name=\"example\")";
        Path tempFile = createTempSetupPy(tempDir, pyContent);

        TomlParseResult result = createSetuptoolsTomlResult();

        SetupToolsPyParser pyParser = new SetupToolsPyParser(result);
        List<String> dependencies = pyParser.load(tempFile.toString());

        assertTrue(dependencies.isEmpty());
    }

    @Test
    public void testParse(@TempDir Path tempDir) throws IOException, DetectableException {
        String pyContent = "from setuptools import setup\n\nsetup(\ninstall_requires=[\n'requests==2.31.0',\n],\n)";
        Path pyFile = createTempSetupPy(tempDir, pyContent);
        
        TomlParseResult result = createSetuptoolsTomlResult();

        SetupToolsPyParser pyParser = new SetupToolsPyParser(result);
        pyParser.load(pyFile.toString());
        SetupToolsParsedResult parsedResult = pyParser.parse();

        assertEquals(1, parsedResult.getDirectDependencies().size());
        
        for (PythonDependency dependency : parsedResult.getDirectDependencies()) {
            assertEquals("requests", dependency.getName());
            assertEquals("2.31.0", dependency.getVersion());
        }
    }

    @Test
    public void testParseWithInlineOneLineList(@TempDir Path tempDir) throws IOException, DetectableException {
        String pyContent = "from setuptools import setup\n\nsetup(install_requires=[\"requests>=2.31.0\", \"numpy<2\"])";
        Path pyFile = createTempSetupPy(tempDir, pyContent);

        TomlParseResult result = createSetuptoolsTomlResult();

        SetupToolsPyParser pyParser = new SetupToolsPyParser(result);
        pyParser.load(pyFile.toString());
        SetupToolsParsedResult parsedResult = pyParser.parse();

        assertEquals(2, parsedResult.getDirectDependencies().size());

        PythonDependency first = parsedResult.getDirectDependencies().get(0);
        PythonDependency second = parsedResult.getDirectDependencies().get(1);

        assertEquals("requests", first.getName());
        assertEquals("2.31.0", first.getVersion());

        assertEquals("numpy", second.getName());
        assertEquals("2", second.getVersion());
    }

    @Test
    public void testParseWithVariableReferenceInstallRequiresUnsupported(@TempDir Path tempDir) throws IOException {
        String pyContent = "from setuptools import setup\n\nrequirements=['requests>=2.31.0']\n\nsetup(\ninstall_requires=requirements,\n)";
        Path tempFile = createTempSetupPy(tempDir, pyContent);

        TomlParseResult result = createSetuptoolsTomlResult();
        SetupToolsPyParser pyParser = new SetupToolsPyParser(result);

        DetectableException exception = assertThrows(
                DetectableException.class,
                () -> pyParser.load(tempFile.toString())
        );
        assertTrue(exception.getMessage().contains("install_requires must be a literal Python list"));

        // A failed load should not populate any dependencies for parse.
        assertTrue(pyParser.parse().getDirectDependencies().isEmpty());
    }

    @Test
    public void testParseWithEnvironmentMarkers(@TempDir Path tempDir) throws IOException, DetectableException {
        String pyContent = "from setuptools import setup\n\nsetup(\ninstall_requires=[\n"
            + "\"contextvars;python_version<'3.7'\",\n"
            + "\"requests>=2.31.0\",\n"
            + "\"dataclasses;python_version<'3.7'\",\n"
            + "],\n)";
        Path pyFile = createTempSetupPy(tempDir, pyContent);

        TomlParseResult result = createSetuptoolsTomlResult();

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
    }
}
