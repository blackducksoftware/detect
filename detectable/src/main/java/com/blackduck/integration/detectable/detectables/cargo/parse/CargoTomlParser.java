package com.blackduck.integration.detectable.detectables.cargo.parse;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.cargo.CargoDependencyType;
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

    public Set<NameVersion> parseDependenciesToInclude(String tomlFileContents, EnumListFilter<CargoDependencyType> dependencyTypeFilter) {
        TomlParseResult toml = Toml.parse(tomlFileContents);
        Map<NameVersion, EnumSet<CargoDependencyType>> dependencyTypeMap = new HashMap<>();
        Set<NameVersion> alwaysIncluded = new HashSet<>();

        // Parse dependencies from each section. If the type is null, it indicates a normal dependency (from [dependencies]).
        parseDependenciesFromTomlTable(toml, NORMAL_DEPENDENCIES_KEY, null, dependencyTypeMap, alwaysIncluded);
        parseDependenciesFromTomlTable(toml, BUILD_DEPENDENCIES_KEY, CargoDependencyType.BUILD, dependencyTypeMap, alwaysIncluded);
        parseDependenciesFromTomlTable(toml, DEV_DEPENDENCIES_KEY, CargoDependencyType.DEV, dependencyTypeMap, alwaysIncluded);

        Set<NameVersion> dependenciesToInclude = new HashSet<>(alwaysIncluded);
        for (Map.Entry<NameVersion, EnumSet<CargoDependencyType>> entry : dependencyTypeMap.entrySet()) {
            NameVersion nameVersion = entry.getKey();
            EnumSet<CargoDependencyType> types = entry.getValue();

            // Include the dependency only if at least one of its types is not excluded by the filter
            boolean shouldBeIncluded = types.stream()
                    .anyMatch(type -> !dependencyTypeFilter.shouldExclude(type));

            if (shouldBeIncluded) {
                dependenciesToInclude.add(nameVersion);
            }
        }
        return dependenciesToInclude;
    }

    private void parseDependenciesFromTomlTable(
            TomlParseResult toml,
            String sectionKey,
            CargoDependencyType cargoDependencyType,
            Map<NameVersion, EnumSet<CargoDependencyType>> dependencyTypeMap,
            Set<NameVersion> alwaysIncluded
    ) {
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
            EnumSet<CargoDependencyType> types = dependencyTypeMap.computeIfAbsent(nv, k -> EnumSet.noneOf(CargoDependencyType.class));
            if (cargoDependencyType != null) {
                types.add(cargoDependencyType);
            } else {
                alwaysIncluded.add(nv);
            }
        }
    }
}
