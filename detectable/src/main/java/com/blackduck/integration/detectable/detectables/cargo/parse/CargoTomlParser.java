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
        Map<NameVersion, EnumSet<CargoDependencyType>> dependencyTypeMap = new HashMap<>();

        parseDependenciesFromTomlTable(toml, NORMAL_DEPENDENCIES_KEY, CargoDependencyType.NORMAL, dependencyTypeMap);
        parseDependenciesFromTomlTable(toml, BUILD_DEPENDENCIES_KEY, CargoDependencyType.BUILD, dependencyTypeMap);
        parseDependenciesFromTomlTable(toml, DEV_DEPENDENCIES_KEY, CargoDependencyType.DEV, dependencyTypeMap);

        Map<String, String> dependenciesToExclude = new HashMap<>();
        for (Map.Entry<NameVersion, EnumSet<CargoDependencyType>> entry : dependencyTypeMap.entrySet()) {
            NameVersion nameVersion = entry.getKey();
            EnumSet<CargoDependencyType> types = entry.getValue();

            boolean shouldBeExcluded = types.stream()
                .allMatch(cargoDetectableOptions.getDependencyTypeFilter()::shouldExclude);

            if (shouldBeExcluded) {
                dependenciesToExclude.put(nameVersion.getName(), nameVersion.getVersion());
            }
        }
        return dependenciesToExclude;
    }

    private void parseDependenciesFromTomlTable(TomlParseResult toml, String sectionKey, CargoDependencyType type, Map<NameVersion, EnumSet<CargoDependencyType>> dependencyTypeMap) {
        TomlTable table = toml.getTable(sectionKey);
        if (table == null) {
            return;
        }

        for (String key : table.keySet()) {
            Object value = table.get(key);
            String version = null;

            if (value instanceof String) {
                version = (String) value;
            } else if (value instanceof TomlTable) {
                version = ((TomlTable) value).getString(VERSION_KEY); // May be null
            }

            NameVersion nv = new NameVersion(key, version);
            dependencyTypeMap.computeIfAbsent(nv, k -> EnumSet.noneOf(CargoDependencyType.class)).add(type);
        }
    }
}
