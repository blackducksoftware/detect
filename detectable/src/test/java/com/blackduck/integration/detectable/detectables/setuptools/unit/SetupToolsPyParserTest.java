package com.blackduck.integration.detectable.detectables.setuptools.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
    public void testLoadExtrasRequire() throws IOException {
        String pyContent = "from setuptools import setup\n\n"
            + "setup(\n"
            + "    install_requires=[\n"
            + "        'requests==2.28.2',\n"
            + "    ],\n"
            + "    extras_require={\n"
            + "        \"http2\": [\n"
            + "            \"certifi>=2025.11.12\",\n"
            + "            \"httpcore>=1.0.9\",\n"
            + "        ],\n"
            + "        \"security\": [\n"
            + "            \"certifi>=2025.11.12\",\n"
            + "            \"charset-normalizer>=3.3.1\",\n"
            + "        ],\n"
            + "    },\n"
            + ")\n";

        Path tempFile = Files.createTempFile("setup", ".py");
        Files.write(tempFile, pyContent.getBytes());

        TomlParseResult toml = Toml.parse("");
        SetupToolsPyParser pyParser = new SetupToolsPyParser(toml);
        Map<String, List<String>> extrasMap = pyParser.loadExtrasRequire(tempFile.toString());

        assertEquals(2, extrasMap.size());
        assertTrue(extrasMap.containsKey("http2"));
        assertTrue(extrasMap.containsKey("security"));
        assertEquals(2, extrasMap.get("http2").size());
        assertTrue(extrasMap.get("http2").contains("certifi>=2025.11.12"));
        assertTrue(extrasMap.get("http2").contains("httpcore>=1.0.9"));
        assertEquals(2, extrasMap.get("security").size());
        assertTrue(extrasMap.get("security").contains("certifi>=2025.11.12"));
        assertTrue(extrasMap.get("security").contains("charset-normalizer>=3.3.1"));

        Files.delete(tempFile);
    }

    @Test
    public void testParseWithExtrasTransitives() throws IOException {
        String pyContent = "from setuptools import setup\n\n"
            + "setup(\n"
            + "    install_requires=[\n"
            + "        'requests[security]==2.28.2',\n"
            + "    ],\n"
            + "    extras_require={\n"
            + "        \"security\": [\n"
            + "            \"certifi>=2025.11.12\",\n"
            + "            \"charset-normalizer>=3.3.1\",\n"
            + "        ],\n"
            + "    },\n"
            + ")\n";

        Path tempFile = Files.createTempFile("setup", ".py");
        Files.write(tempFile, pyContent.getBytes());

        String tomlContent = "[build-system]\nrequires = [\"setuptools\"]";
        TomlParseResult toml = Toml.parse(tomlContent);

        SetupToolsPyParser pyParser = new SetupToolsPyParser(toml);
        pyParser.load(tempFile.toString());
        pyParser.loadExtrasRequire(tempFile.toString());
        SetupToolsParsedResult parsedResult = pyParser.parse();

        assertEquals(1, parsedResult.getDirectDependencies().size());
        assertEquals("requests", parsedResult.getDirectDependencies().get(0).getName());

        Map<String, List<PythonDependency>> extrasTransitives = parsedResult.getExtrasTransitives();
        assertNotNull(extrasTransitives);
        assertTrue(extrasTransitives.containsKey("requests"));
        assertEquals(2, extrasTransitives.get("requests").size());
        assertEquals("certifi", extrasTransitives.get("requests").get(0).getName());
        assertEquals("charset-normalizer", extrasTransitives.get("requests").get(1).getName());

        Files.delete(tempFile);
    }

    @Test
    public void testExtrasGroupNotMatchingInstallRequiresIsIgnored() throws IOException {
        String pyContent = "from setuptools import setup\n\n"
            + "setup(\n"
            + "    install_requires=[\n"
            + "        'requests==2.28.2',\n"
            + "    ],\n"
            + "    extras_require={\n"
            + "        \"dev\": [\n"
            + "            \"pytest>=7.0\",\n"
            + "        ],\n"
            + "    },\n"
            + ")\n";

        Path tempFile = Files.createTempFile("setup", ".py");
        Files.write(tempFile, pyContent.getBytes());

        String tomlContent = "[build-system]\nrequires = [\"setuptools\"]";
        TomlParseResult toml = Toml.parse(tomlContent);

        SetupToolsPyParser pyParser = new SetupToolsPyParser(toml);
        pyParser.load(tempFile.toString());
        pyParser.loadExtrasRequire(tempFile.toString());
        SetupToolsParsedResult parsedResult = pyParser.parse();

        assertTrue(parsedResult.getExtrasTransitives().isEmpty());

        Files.delete(tempFile);
    }

    @Test
    public void testParseWithMultiExtrasTransitives() throws IOException {
        String pyContent = "from setuptools import setup\n\n"
            + "setup(\n"
            + "    install_requires=[\n"
            + "        'requests[security,socks]==2.28.2',\n"
            + "    ],\n"
            + "    extras_require={\n"
            + "        \"security\": [\n"
            + "            \"certifi>=2025.11.12\",\n"
            + "        ],\n"
            + "        \"socks\": [\n"
            + "            \"PySocks>=1.5.6\",\n"
            + "        ],\n"
            + "    },\n"
            + ")\n";

        Path tempFile = Files.createTempFile("setup", ".py");
        Files.write(tempFile, pyContent.getBytes());

        String tomlContent = "[build-system]\nrequires = [\"setuptools\"]";
        TomlParseResult toml = Toml.parse(tomlContent);

        SetupToolsPyParser pyParser = new SetupToolsPyParser(toml);
        pyParser.load(tempFile.toString());
        pyParser.loadExtrasRequire(tempFile.toString());
        SetupToolsParsedResult parsedResult = pyParser.parse();

        Map<String, List<PythonDependency>> extrasTransitives = parsedResult.getExtrasTransitives();
        assertNotNull(extrasTransitives);
        assertTrue(extrasTransitives.containsKey("requests"));
        assertEquals(2, extrasTransitives.get("requests").size());
        assertEquals("certifi", extrasTransitives.get("requests").get(0).getName());
        assertEquals("PySocks", extrasTransitives.get("requests").get(1).getName());

        Files.delete(tempFile);
    }
}
