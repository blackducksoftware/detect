package com.blackduck.integration.detectable.detectables.setuptools.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsParsedResult;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsTomlParser;
import com.blackduck.integration.detectable.python.util.PythonDependency;

public class SetupToolsTomlParserTest {

    @Test
    public void testParse() throws IOException {
        String tomlContent = "[project]\nname = \"setuptools\"\nversion = \"1.0.0\"\ndependencies = [\n    \"requests\",\n    \"numpy\"\n]";
        Path pyProjectFile = Files.createTempFile("pyproject", ".toml");
        Files.write(pyProjectFile, tomlContent.getBytes());

        TomlParseResult result = Toml.parse(tomlContent);

        SetupToolsTomlParser tomlParser = new SetupToolsTomlParser(result);
        SetupToolsParsedResult parsedResult = tomlParser.parse();

        assertEquals("setuptools", parsedResult.getProjectName());
        assertEquals("1.0.0", parsedResult.getProjectVersion());
        assertEquals(2, parsedResult.getDirectDependencies().size());

        Files.delete(pyProjectFile); // delete the temporary file
    }

    @Test
    public void testParseWithExtrasTransitives() throws IOException {
        String tomlContent = "[project]\n" +
                "name = \"myproject\"\n" +
                "version = \"1.0.0\"\n" +
                "dependencies = [\n" +
                "    \"requests[security]==2.28.2\",\n" +
                "    \"httpx[http2]==0.23.3\"\n" +
                "]\n\n" +
                "[project.optional-dependencies]\n" +
                "security = [\n" +
                "    \"charset-normalizer>=3.3.1\",\n" +
                "    \"idna>=3.11\"\n" +
                "]\n" +
                "http2 = [\n" +
                "    \"certifi>=2025.11.12\",\n" +
                "    \"httpcore>=1.0.9\"\n" +
                "]\n";

        TomlParseResult result = Toml.parse(tomlContent);
        SetupToolsTomlParser tomlParser = new SetupToolsTomlParser(result);
        SetupToolsParsedResult parsedResult = tomlParser.parse();

        // Direct dependencies
        assertEquals(2, parsedResult.getDirectDependencies().size());

        // Extras transitives
        Map<String, List<PythonDependency>> extrasTransitives = parsedResult.getExtrasTransitives();
        assertNotNull(extrasTransitives);
        assertEquals(2, extrasTransitives.size());

        // requests -> security group
        assertTrue(extrasTransitives.containsKey("requests"));
        List<PythonDependency> requestsTransitives = extrasTransitives.get("requests");
        assertEquals(2, requestsTransitives.size());
        assertEquals("charset-normalizer", requestsTransitives.get(0).getName());
        assertEquals("3.3.1", requestsTransitives.get(0).getVersion());
        assertEquals("idna", requestsTransitives.get(1).getName());
        assertEquals("3.11", requestsTransitives.get(1).getVersion());

        // httpx -> http2 group
        assertTrue(extrasTransitives.containsKey("httpx"));
        List<PythonDependency> httpxTransitives = extrasTransitives.get("httpx");
        assertEquals(2, httpxTransitives.size());
        assertEquals("certifi", httpxTransitives.get(0).getName());
        assertEquals("2025.11.12", httpxTransitives.get(0).getVersion());
        assertEquals("httpcore", httpxTransitives.get(1).getName());
        assertEquals("1.0.9", httpxTransitives.get(1).getVersion());
    }

    @Test
    public void testExtrasGroupNotMatchingDependenciesIsIgnored() throws IOException {
        String tomlContent = "[project]\n" +
                "name = \"myproject\"\n" +
                "version = \"1.0.0\"\n" +
                "dependencies = [\n" +
                "    \"requests==2.28.2\"\n" +
                "]\n\n" +
                "[project.optional-dependencies]\n" +
                "dev = [\n" +
                "    \"pytest>=7.0\",\n" +
                "    \"flake8>=5.0\"\n" +
                "]\n";

        TomlParseResult result = Toml.parse(tomlContent);
        SetupToolsTomlParser tomlParser = new SetupToolsTomlParser(result);
        SetupToolsParsedResult parsedResult = tomlParser.parse();

        // Direct dependency has no extras specifier, so no match
        assertEquals(1, parsedResult.getDirectDependencies().size());
        assertEquals("requests", parsedResult.getDirectDependencies().get(0).getName());

        // Extras transitives should be empty since "dev" doesn't match any dependency extras
        assertTrue(parsedResult.getExtrasTransitives().isEmpty());
    }
}
