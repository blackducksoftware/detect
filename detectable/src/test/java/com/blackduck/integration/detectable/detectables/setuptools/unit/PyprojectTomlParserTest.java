package com.blackduck.integration.detectable.detectables.setuptools.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsParsedResult;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsTomlParser;

class PyprojectTomlParserTest {

    @Test
    void testParseComplexPyprojectToml() throws IOException {
        String tomlContent = "[build-system]\n" +
            "requires = [\"setuptools>=61.0\", \"wheel>=0.37.1\"]\n" +
            "build-backend = \"setuptools.build_meta\"\n\n" +
            "[project]\n" +
            "name = \"complex-setuptools-project\"\n" +
            "version = \"0.1.0\"\n" +
            "description = \"Sample project testing complex PEP 508 dependencies\"\n" +
            "authors = [\n" +
            "    { name = \"Example User\", email = \"example.user@email.com\" }\n" +
            "]\n" +
            "readme = \"README.md\"\n" +
            "license = { file = \"LICENSE\" }\n" +
            "keywords = [\"python\", \"pep508\", \"dependencies\", \"testing\"]\n" +
            "classifiers = [\n" +
            "    \"Programming Language :: Python :: 3\",\n" +
            "    \"License :: OSI Approved :: MIT License\",\n" +
            "    \"Operating System :: OS Independent\"\n" +
            "]\n\n" +
            "dependencies = [\n" +
            "    \"requests>=2.31.0,<3.0\",\n" +
            "    \"alembic==1.12.0\",\n" +
            "    \"beautifulsoup4==4.13.3\",\n" +
            "    \"six==1.16.0\",\n" +
            "    \"torch @ https://download.pytorch.org/whl/cpu/torch-2.6.0%2Bcpu-cp310-cp310-linux_x86_64.whl\",\n" +
            "    \"torchvision @ https://download.pytorch.org/whl/cpu/torchvision-0.21.0%2Bcpu-cp310-cp310-linux_x86_64.whl\",\n" +
            "    \"flask @ git+https://github.com/pallets/flask.git@2.3.3\",\n" +
            "    \"requests[security,socks]==2.31.0\",\n" +
            "    \"pandas[all]>=2.1.0,<3.0; python_version>'3.8'\"\n" +
            "]\n\n" +
            "[project.optional-dependencies]\n" +
            "dev = [\n" +
            "    \"pytest>=7.4.0\",\n" +
            "    \"black==24.3.0\",\n" +
            "    \"mypy>=1.5.1\"\n" +
            "]\n" +
            "docs = [\n" +
            "    \"sphinx>=7.0.0\",\n" +
            "    \"sphinx-rtd-theme>=1.2.0\"\n" +
            "]\n\n" +
            "[tool.setuptools]\n" +
            "py-modules = [\"main\"]\n\n" +
            "[project.scripts]\n" +
            "complex-setuptools-project = \"main:main\"\n";

        Path pyProjectFile = Files.createTempFile("pyproject", ".toml");
        Files.write(pyProjectFile, tomlContent.getBytes());

        TomlParseResult result = Toml.parse(tomlContent);

        SetupToolsTomlParser tomlParser = new SetupToolsTomlParser(result);
        SetupToolsParsedResult parsedResult = tomlParser.parse();

        // Assertions for project metadata
        assertEquals("complex-setuptools-project", parsedResult.getProjectName());
        assertEquals("0.1.0", parsedResult.getProjectVersion());

        // Assertions for dependencies
        assertEquals(9, parsedResult.getDirectDependencies().size());
        assertTrue(parsedResult.getDirectDependencies().stream()
            .anyMatch(dep -> dep.getName().equals("requests") && dep.getVersion().equals("2.31.0")));
        assertTrue(parsedResult.getDirectDependencies().stream()
            .anyMatch(dep -> dep.getName().equals("torch") && dep.getVersion().equals("2.6.0")));

        // Assertions for optional dependencies
        assertTrue(result.contains("project.optional-dependencies.dev"));
        assertTrue(result.contains("project.optional-dependencies.docs"));

        Files.delete(pyProjectFile); // Clean up the temporary file
    }
}