package com.blackduck.integration.detectable.detectables.setuptools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsCfgParser;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsParser;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsPyParser;
import com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsTomlParser;

public class SetupToolsExtractUtils {
    
    private static final String BUILD_KEY = "build-system.requires";
    private static final String REQUIRED_KEY = "setuptools";
    private static final String TOML_DEPENDENCIES = "project.dependencies";
    
    private static final String SETUP_CFG = "setup.cfg";
    private static final String SETUP_PY = "setup.py";

    public static TomlParseResult extractToml(File projectToml) throws IOException {
        String projectTomlText = FileUtils.readFileToString(projectToml, StandardCharsets.UTF_8);

        return Toml.parse(projectTomlText);
    }

    public static boolean checkTomlRequiresSetupTools(TomlParseResult parsedToml) {
        if (parsedToml != null) {
            TomlArray buildRequires = parsedToml.getArray(BUILD_KEY);

            if (buildRequires != null) {
                for (int i = 0; i < buildRequires.size(); i++) {
                    String requires = buildRequires.getString(i);

                    if (requires.contains(REQUIRED_KEY)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static SetupToolsParser resolveSetupToolsParser(TomlParseResult parsedToml, FileFinder fileFinder, DetectableEnvironment environment) throws IOException {
        // Dependencies, if they exist at all, will be in one of three files.
        // Step 1: Check the pyproject.toml
        TomlArray tomlDependencies = parsedToml.getArray(TOML_DEPENDENCIES);
        
        if (tomlDependencies != null && !tomlDependencies.isEmpty()) {
            return new SetupToolsTomlParser(parsedToml);
        }
        
        // Step 2: Check the setup.py
        Requirements fileResolver = new Requirements(fileFinder, environment);
        File pyFile = fileResolver.file(SETUP_PY);
        
        if (pyFile != null) {
            SetupToolsPyParser pyParser = new SetupToolsPyParser(parsedToml);

            List<String> pyDependencies = pyParser.load(pyFile.toString());
            
            if (pyDependencies != null && !pyDependencies.isEmpty()) {
                return pyParser;
            }
        }
        
        // Step 3: Check the setup.cfg
        fileResolver = new Requirements(fileFinder, environment);
        File cfgFile = fileResolver.file(SETUP_CFG);

        if (cfgFile != null) {
            SetupToolsCfgParser cfgParser = new SetupToolsCfgParser(parsedToml);

            List<String> cfgDependencies = cfgParser.load(cfgFile.toString());
            Map<String, List<String>> extrasMap = cfgParser.loadExtrasRequire(cfgFile.toString());

            if ((cfgDependencies != null && !cfgDependencies.isEmpty()) || (extrasMap != null && !extrasMap.isEmpty())) {
                return cfgParser;
            }
        }
        
        return null;
    }
}
