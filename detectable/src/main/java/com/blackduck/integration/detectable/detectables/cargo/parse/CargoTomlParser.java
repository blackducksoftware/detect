package com.blackduck.integration.detectable.detectables.cargo.parse;

import java.util.*;

import com.blackduck.integration.detectable.detectables.cargo.CargoDependencyType;
import com.blackduck.integration.detectable.detectables.cargo.CargoDetectableOptions;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import com.blackduck.integration.util.NameVersion;
import org.tomlj.TomlTable;

public class CargoTomlParser {
    private static final String NAME_KEY = "name";
    private static final String VERSION_KEY = "version";
    private static final String PACKAGE_KEY = "package";
    private static final String DEV_DEPENDENCIES_KEY = "dev-dependencies";
    private static final String BUILD_DEPENDENCIES_KEY = "build-dependencies";

    public Optional<NameVersion> parseNameVersionFromCargoToml(String tomlFileContents) {
        TomlParseResult cargoTomlObject = Toml.parse(tomlFileContents);
        if (cargoTomlObject.contains(PACKAGE_KEY)) {
            return Optional.ofNullable(cargoTomlObject.getTable(PACKAGE_KEY))
                .filter(info -> info.contains(NAME_KEY))
                .map(info -> new NameVersion(info.getString(NAME_KEY), info.getString(VERSION_KEY)));
        }
        return Optional.empty();
    }

    public Map<String, String> parseDependencyNameVersions(String tomlFileContents, CargoDetectableOptions cargoDetectableOptions) {
        TomlParseResult toml = Toml.parse(tomlFileContents);
        Map<String, String> allDeps = new HashMap<>();

        if (cargoDetectableOptions.getDependencyTypeFilter().shouldExclude(CargoDependencyType.DEV)) {
            allDeps.putAll(parseNamedDependenciesFromTable(toml, DEV_DEPENDENCIES_KEY));
        }
        if (cargoDetectableOptions.getDependencyTypeFilter().shouldExclude(CargoDependencyType.BUILD)) {
            allDeps.putAll(parseNamedDependenciesFromTable(toml, BUILD_DEPENDENCIES_KEY));
        }

        return allDeps;
    }

    private Map<String, String> parseNamedDependenciesFromTable(TomlParseResult toml, String sectionKey) {
        Map<String, String> deps = new HashMap<>();
        TomlTable table = toml.getTable(sectionKey);
        if (table == null) {
            return deps;
        }

        for (String key : table.keySet()) {
            Object value = table.get(key);
            if (value instanceof String) {
                deps.put(key, (String) value);
            } else {
                deps.put(key, null);
            }
        }

        return deps;
    }
}
