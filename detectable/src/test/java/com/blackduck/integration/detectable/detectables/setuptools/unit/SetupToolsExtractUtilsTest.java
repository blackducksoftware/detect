package com.blackduck.integration.detectable.detectables.setuptools.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import org.junit.jupiter.api.Test;
import org.tomlj.TomlParseResult;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectables.setuptools.SetupToolsExtractUtils;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsCfgParser;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsParser;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsPyParser;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsTomlParser;

public class SetupToolsExtractUtilsTest {
    
    private static final String TOML = ".toml";
    private static final String BUILD_SYSTEM_REQUIRES_SETUPTOOLS = "[build-system]\nrequires = [\"setuptools\"]";
    private static final String PYPROJECT = "pyproject";

    @Test
    public void testExtractToml() throws IOException {
        String tomlContent = BUILD_SYSTEM_REQUIRES_SETUPTOOLS;
        Path tempFile = Files.createTempFile(PYPROJECT, TOML);
        Files.write(tempFile, tomlContent.getBytes());

        TomlParseResult result = SetupToolsExtractUtils.extractToml(tempFile.toFile());

        assertEquals("setuptools", result.getArray("build-system.requires").getString(0));

        Files.delete(tempFile); // delete the temporary file
    }

    @Test
    public void testCheckTomlRequiresSetupTools() throws IOException {
        String tomlContent = BUILD_SYSTEM_REQUIRES_SETUPTOOLS;
        Path tempFile = Files.createTempFile(PYPROJECT, TOML);
        Files.write(tempFile, tomlContent.getBytes());

        TomlParseResult result = SetupToolsExtractUtils.extractToml(tempFile.toFile());

        assertTrue(SetupToolsExtractUtils.checkTomlRequiresSetupTools(result));

        Files.delete(tempFile); // delete the temporary file
    }
    
    @Test
    public void testFindDependenciesFileForPyProjectToml() throws IOException, DetectableException {
        String tomlContent = "[project]\ndependencies = [\"requests\"]";
        Path tempFile = Files.createTempFile(PYPROJECT, TOML);
        Files.write(tempFile, tomlContent.getBytes());

        TomlParseResult result = SetupToolsExtractUtils.extractToml(tempFile.toFile());

        FileFinder fileFinder = mock(FileFinder.class);
        DetectableEnvironment environment = mock(DetectableEnvironment.class);
        when(fileFinder.findFile(environment.getDirectory(), "setup.cfg")).thenReturn(null);
        when(fileFinder.findFile(environment.getDirectory(), "setup.py")).thenReturn(null);
        
        SetupToolsParser setupToolsParser = SetupToolsExtractUtils.resolveSetupToolsParser(result, fileFinder, environment);

        assertNotNull(setupToolsParser);
        assertTrue(setupToolsParser instanceof SetupToolsTomlParser);

        Files.delete(tempFile); // delete the temporary file
    }
    
    @Test
    public void testFindDependenciesFileForSetupCfg() throws IOException, DetectableException {
        String tomlContent = BUILD_SYSTEM_REQUIRES_SETUPTOOLS;
        Path tempFileToml = Files.createTempFile(PYPROJECT, TOML);
        Files.write(tempFileToml, tomlContent.getBytes());
        
        TomlParseResult result = SetupToolsExtractUtils.extractToml(tempFileToml.toFile());
        
        String cfgContent = "install_requires = requests==2.31.0";
        Path tempFileCfg = Files.createTempFile("setup", ".cfg");
        Files.write(tempFileCfg, cfgContent.getBytes());

        FileFinder fileFinder = mock(FileFinder.class);
        DetectableEnvironment environment = mock(DetectableEnvironment.class);
        when(fileFinder.findFile(environment.getDirectory(), "setup.cfg")).thenReturn(tempFileCfg.toFile());
        
        SetupToolsParser setupToolsParser = SetupToolsExtractUtils.resolveSetupToolsParser(result, fileFinder, environment);

        assertNotNull(setupToolsParser);
        assertTrue(setupToolsParser instanceof SetupToolsCfgParser);

        Files.delete(tempFileCfg);
        Files.delete(tempFileToml);
    }
    
    @Test
    public void testFindDependenciesFileForSetupPy() throws IOException, DetectableException {
        String tomlContent = BUILD_SYSTEM_REQUIRES_SETUPTOOLS;
        Path tempFileToml = Files.createTempFile(PYPROJECT, TOML);
        Files.write(tempFileToml, tomlContent.getBytes());
        
        TomlParseResult result = SetupToolsExtractUtils.extractToml(tempFileToml.toFile());
        
        String pyContent = "setup(\n    install_requires=[\n        'BazSpam ==1.1',\n    ],\n)";
        Path tempFilePy = Files.createTempFile("setup", ".py");
        Files.write(tempFilePy, pyContent.getBytes());

        FileFinder fileFinder = mock(FileFinder.class);
        DetectableEnvironment environment = mock(DetectableEnvironment.class);
        when(fileFinder.findFile(environment.getDirectory(), "setup.py")).thenReturn(tempFilePy.toFile());
        
        SetupToolsParser setupToolsParser = SetupToolsExtractUtils.resolveSetupToolsParser(result, fileFinder, environment);

        assertNotNull(setupToolsParser);
        assertTrue(setupToolsParser instanceof SetupToolsPyParser);

        Files.delete(tempFilePy);
        Files.delete(tempFileToml);
    }
}
