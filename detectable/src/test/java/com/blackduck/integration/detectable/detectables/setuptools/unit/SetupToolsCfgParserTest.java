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

import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsCfgParser;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsParsedResult;
import com.blackduck.integration.detectable.python.util.PythonDependency;

public class SetupToolsCfgParserTest {
    
    @Test
    public void testLoad() throws IOException {
        String cfgContent = "[metadata]\nname = \"setuptools\"\n\n[options]\ninstall_requires =\n    requests\n    numpy";
        Path tempFile = Files.createTempFile("setup", ".cfg");
        Files.write(tempFile, cfgContent.getBytes());

        TomlParseResult result = Toml.parse(cfgContent);

        SetupToolsCfgParser cfgParser = new SetupToolsCfgParser(result);
        List<String> dependencies = cfgParser.load(tempFile.toString());

        assertEquals(2, dependencies.size());
        assertTrue(dependencies.contains("requests"));
        assertTrue(dependencies.contains("numpy"));

        Files.delete(tempFile); // delete the temporary file
    }
    
    @Test
    public void testParse() throws IOException {
        String cfgContent = "[metadata]\nname = setuptools\n[options]\ninstall_requires =\n    requests\n    numpy";
        Path cfgFile = Files.createTempFile("setup", ".cfg");
        Files.write(cfgFile, cfgContent.getBytes());
        
        String tomlContent = "[build-system]\nrequires = [\"setuptools\"]";

        TomlParseResult result = Toml.parse(tomlContent);

        SetupToolsCfgParser cfgParser = new SetupToolsCfgParser(result);
        cfgParser.load(cfgFile.toString());
        SetupToolsParsedResult parsedResult = cfgParser.parse();

        assertEquals("setuptools", parsedResult.getProjectName());
        assertEquals(2, parsedResult.getDirectDependencies().size());
        
        for (PythonDependency dependency : parsedResult.getDirectDependencies()) {
            if (!dependency.getName().equals("requests") && !dependency.getName().equals("numpy")) {
                fail();
            }
        }

        Files.delete(cfgFile); // delete the temporary file
    }

    @Test
    public void testLoadExtrasRequireReturnsGroupedMap() throws IOException {
        String cfgContent = "[metadata]\n" +
                "name = sample\n" +
                "\n" +
                "[options.extras_require]\n" +
                "http2 =\n" +
                "    h2>=4,<5\n" +
                "    hpack>=4,<5\n" +
                "security =\n" +
                "    pyOpenSSL>=23.0\n" +
                "    cryptography>=41.0\n" +
                "cli =\n" +
                "    click==8.*\n" +
                "    rich>=10,<13\n";
        Path tempFile = Files.createTempFile("setup", ".cfg");
        Files.write(tempFile, cfgContent.getBytes());

        TomlParseResult result = Toml.parse("[build-system]\nrequires = [\"setuptools\"]");

        SetupToolsCfgParser cfgParser = new SetupToolsCfgParser(result);
        Map<String, List<String>> extrasMap = cfgParser.loadExtrasRequire(tempFile.toString());

        assertEquals(3, extrasMap.size());

        assertTrue(extrasMap.containsKey("http2"));
        assertEquals(2, extrasMap.get("http2").size());
        assertTrue(extrasMap.get("http2").contains("h2>=4,<5"));
        assertTrue(extrasMap.get("http2").contains("hpack>=4,<5"));

        assertTrue(extrasMap.containsKey("security"));
        assertEquals(2, extrasMap.get("security").size());
        assertTrue(extrasMap.get("security").contains("pyOpenSSL>=23.0"));
        assertTrue(extrasMap.get("security").contains("cryptography>=41.0"));

        assertTrue(extrasMap.containsKey("cli"));
        assertEquals(2, extrasMap.get("cli").size());
        assertTrue(extrasMap.get("cli").contains("click==8.*"));
        assertTrue(extrasMap.get("cli").contains("rich>=10,<13"));

        Files.delete(tempFile);
    }

    @Test
    public void testParseWithExtrasTransitives() throws IOException {
        String cfgContent = "[metadata]\n" +
                "name = sample\n" +
                "\n" +
                "[options]\n" +
                "install_requires =\n" +
                "    requests[security]==2.28.2\n" +
                "    httpx[http2]==0.23.3\n" +
                "\n" +
                "[options.extras_require]\n" +
                "http2 =\n" +
                "    certifi>=2025.11.12\n" +
                "    httpcore>=1.0.9\n" +
                "security =\n" +
                "    charset-normalizer>=3.3.1\n" +
                "    idna>=3.11\n";
        Path tempFile = Files.createTempFile("setup", ".cfg");
        Files.write(tempFile, cfgContent.getBytes());

        TomlParseResult result = Toml.parse("[build-system]\nrequires = [\"setuptools\"]");

        SetupToolsCfgParser cfgParser = new SetupToolsCfgParser(result);
        cfgParser.load(tempFile.toString());
        cfgParser.loadExtrasRequire(tempFile.toString());
        SetupToolsParsedResult parsedResult = cfgParser.parse();

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

        Files.delete(tempFile);
    }

    @Test
    public void testExtrasGroupNotMatchingInstallRequiresIsIgnored() throws IOException {
        String cfgContent = "[metadata]\n" +
                "name = sample\n" +
                "\n" +
                "[options]\n" +
                "install_requires =\n" +
                "    requests==2.28.2\n" +
                "\n" +
                "[options.extras_require]\n" +
                "dev =\n" +
                "    pytest>=7.0\n" +
                "    flake8>=5.0\n";
        Path tempFile = Files.createTempFile("setup", ".cfg");
        Files.write(tempFile, cfgContent.getBytes());

        TomlParseResult result = Toml.parse("[build-system]\nrequires = [\"setuptools\"]");

        SetupToolsCfgParser cfgParser = new SetupToolsCfgParser(result);
        cfgParser.load(tempFile.toString());
        cfgParser.loadExtrasRequire(tempFile.toString());
        SetupToolsParsedResult parsedResult = cfgParser.parse();

        // Direct dependency has no extras specifier, so no match
        assertEquals(1, parsedResult.getDirectDependencies().size());
        assertEquals("requests", parsedResult.getDirectDependencies().get(0).getName());

        // Extras transitives should be empty since "dev" doesn't match any install_requires extras
        assertTrue(parsedResult.getExtrasTransitives().isEmpty());

        Files.delete(tempFile);
    }

    @Test
    public void testParseWithMultiExtrasTransitives() throws IOException {
        String cfgContent = "[metadata]\n" +
                "name = sample\n" +
                "\n" +
                "[options]\n" +
                "install_requires =\n" +
                "    requests[security,socks]==2.28.2\n" +
                "\n" +
                "[options.extras_require]\n" +
                "security =\n" +
                "    pyOpenSSL>=23.0\n" +
                "    cryptography>=41.0\n" +
                "socks =\n" +
                "    PySocks>=1.5.6\n";
        Path tempFile = Files.createTempFile("setup", ".cfg");
        Files.write(tempFile, cfgContent.getBytes());

        TomlParseResult result = Toml.parse("[build-system]\nrequires = [\"setuptools\"]");

        SetupToolsCfgParser cfgParser = new SetupToolsCfgParser(result);
        cfgParser.load(tempFile.toString());
        cfgParser.loadExtrasRequire(tempFile.toString());
        SetupToolsParsedResult parsedResult = cfgParser.parse();

        // Direct dependencies
        assertEquals(1, parsedResult.getDirectDependencies().size());

        // Extras transitives: both security and socks groups should be merged under "requests"
        Map<String, List<PythonDependency>> extrasTransitives = parsedResult.getExtrasTransitives();
        assertNotNull(extrasTransitives);
        assertEquals(1, extrasTransitives.size());
        assertTrue(extrasTransitives.containsKey("requests"));

        List<PythonDependency> requestsTransitives = extrasTransitives.get("requests");
        assertEquals(3, requestsTransitives.size());
        assertEquals("pyOpenSSL", requestsTransitives.get(0).getName());
        assertEquals("cryptography", requestsTransitives.get(1).getName());
        assertEquals("PySocks", requestsTransitives.get(2).getName());

        Files.delete(tempFile);
    }

    @Test
    public void testLoadExtrasRequireStopsAtNextSection() throws IOException {
        String cfgContent = "[options.extras_require]\n" +
                "dev =\n" +
                "    pytest>=7.0\n" +
                "\n" +
                "[options.package_data]\n" +
                "* = *.txt\n";
        Path tempFile = Files.createTempFile("setup", ".cfg");
        Files.write(tempFile, cfgContent.getBytes());

        TomlParseResult result = Toml.parse("[build-system]\nrequires = [\"setuptools\"]");

        SetupToolsCfgParser cfgParser = new SetupToolsCfgParser(result);
        Map<String, List<String>> extrasMap = cfgParser.loadExtrasRequire(tempFile.toString());

        assertEquals(1, extrasMap.size());
        assertTrue(extrasMap.containsKey("dev"));
        assertEquals(1, extrasMap.get("dev").size());
        assertTrue(extrasMap.get("dev").contains("pytest>=7.0"));

        Files.delete(tempFile);
    }
}
