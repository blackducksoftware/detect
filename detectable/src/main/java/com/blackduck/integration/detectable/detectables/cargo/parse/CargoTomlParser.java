package com.blackduck.integration.detectable.detectables.cargo.parse;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

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
    private static final String NORMAL_DEPENDENCIES_KEY = "dependencies";
    private static final String BUILD_DEPENDENCIES_KEY = "build-dependencies";
    private static final String DEV_DEPENDENCIES_KEY = "dev-dependencies";

    public Optional<NameVersion> parseNameVersionFromCargoToml(String tomlFileContents) {
        TomlParseResult cargoTomlObject = Toml.parse(tomlFileContents);
        if (cargoTomlObject.contains(PACKAGE_KEY)) {
            return Optional.ofNullable(cargoTomlObject.getTable(PACKAGE_KEY))
                .filter(info -> info.contains(NAME_KEY))
                .map(info -> new NameVersion(info.getString(NAME_KEY), info.getString(VERSION_KEY)));
        }
        return Optional.empty();
    }

    public Map<String, String> parseDependenciesToExclude(String tomlFileContents, CargoDetectableOptions cargoDetectableOptions) {
        TomlParseResult toml = Toml.parse(tomlFileContents);

        Map<String, String> normalDeps = parseDependenciesFromTomlTable(toml, NORMAL_DEPENDENCIES_KEY);
        Map<String, String> buildDeps = parseDependenciesFromTomlTable(toml, BUILD_DEPENDENCIES_KEY);
        Map<String, String> devDeps = parseDependenciesFromTomlTable(toml, DEV_DEPENDENCIES_KEY);

        // This map collects all types of dependencies along with their types
        Map<String, EnumSet<CargoDependencyType>> dependencyTypeMap = new HashMap<>();

        normalDeps.keySet().forEach(dep -> dependencyTypeMap
            .computeIfAbsent(dep, k -> EnumSet.noneOf(CargoDependencyType.class))
            .add(CargoDependencyType.NORMAL));

        buildDeps.keySet().forEach(dep -> dependencyTypeMap
            .computeIfAbsent(dep, k -> EnumSet.noneOf(CargoDependencyType.class))
            .add(CargoDependencyType.BUILD));

        devDeps.keySet().forEach(dep -> dependencyTypeMap
            .computeIfAbsent(dep, k -> EnumSet.noneOf(CargoDependencyType.class))
            .add(CargoDependencyType.DEV));

        // Determining on which dependencies to exclude
        Map<String, String> dependenciesToExclude = new HashMap<>();
        for (Map.Entry<String, EnumSet<CargoDependencyType>> entry : dependencyTypeMap.entrySet()) {
            String dependencyName = entry.getKey();
            EnumSet<CargoDependencyType> types = entry.getValue();

            boolean shouldBeExcluded = types.stream()
                .allMatch(cargoDetectableOptions.getDependencyTypeFilter()::shouldExclude);

            if (shouldBeExcluded) {
                String version = normalDeps.getOrDefault(dependencyName,
                    buildDeps.getOrDefault(dependencyName, devDeps.get(dependencyName)));
                dependenciesToExclude.put(dependencyName, version);
            }
        }

        return dependenciesToExclude;
    }

    private Map<String, String> parseDependenciesFromTomlTable(TomlParseResult toml, String sectionKey) {
        Map<String, String> deps = new HashMap<>();
        TomlTable table = toml.getTable(sectionKey);
        if (table == null) {
            return deps;
        }

        for (String key : table.keySet()) {
            Object value = table.get(key);
            if (value instanceof String) {
                deps.put(key, (String) value);
            } else if (value instanceof TomlTable) {
                TomlTable dependencyTable = (TomlTable) value;
                String version = dependencyTable.getString(VERSION_KEY); // May be null
                deps.put(key, version);
            }
        }

        return deps;
    }
}
