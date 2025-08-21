package com.blackduck.integration.detectable.detectables.cargo.parse;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
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

    public boolean hasDependencySections(String tomlFileContents) {
        TomlParseResult toml = Toml.parse(tomlFileContents);
        return toml.contains(NORMAL_DEPENDENCIES_KEY)
            || toml.contains(BUILD_DEPENDENCIES_KEY)
            || toml.contains(DEV_DEPENDENCIES_KEY);
    }

    public Set<NameVersion> parseDependenciesToInclude(String tomlFileContents, EnumListFilter<CargoDependencyType> dependencyTypeFilter) {
        TomlParseResult toml = Toml.parse(tomlFileContents);
        Map<NameVersion, EnumSet<CargoDependencyType>> dependencyTypeMap = new HashMap<>();

        parseDependenciesFromTomlTable(toml, NORMAL_DEPENDENCIES_KEY, CargoDependencyType.NORMAL, dependencyTypeMap);
        parseDependenciesFromTomlTable(toml, BUILD_DEPENDENCIES_KEY, CargoDependencyType.BUILD, dependencyTypeMap);
        parseDependenciesFromTomlTable(toml, DEV_DEPENDENCIES_KEY, CargoDependencyType.DEV, dependencyTypeMap);

        Set<NameVersion> dependenciesToInclude = new HashSet<>();
        for (Map.Entry<NameVersion, EnumSet<CargoDependencyType>> entry : dependencyTypeMap.entrySet()) {
            NameVersion nameVersion = entry.getKey();
            EnumSet<CargoDependencyType> types = entry.getValue();

            boolean shouldBeIncluded;
            if (dependencyTypeFilter == null) {
                shouldBeIncluded = true; // No filter, include all
            } else {
                shouldBeIncluded = types.stream()
                        .anyMatch(type -> !dependencyTypeFilter.shouldExclude(type));
            }

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
        Map<NameVersion, EnumSet<CargoDependencyType>> dependencyTypeMap
    ) {
        // Sanitize the section key before fetching table
        String sanitizedSectionKey = sanitizeKey(sectionKey);
        TomlTable table = toml.getTable(sanitizedSectionKey);
        if (table != null) {
            for (String key : table.keySet()) {
                Object value = table.get(key);
                String version = null;

                if (value instanceof String) {
                    version = (String) value;
                } else if (value instanceof TomlTable) {
                    version = ((TomlTable) value).getString(VERSION_KEY); // may be null
                }

                NameVersion nv = new NameVersion(key, version);
                dependencyTypeMap
                    .computeIfAbsent(nv, k -> EnumSet.noneOf(CargoDependencyType.class))
                    .add(cargoDependencyType);
            }
        }

        // Recursively check for nested sections
        parseNestedDependencies(
            toml,
            sectionKey, // keep original for matching
            cargoDependencyType,
            dependencyTypeMap
        );
    }

    private void parseNestedDependencies(
        Object node,
        String sectionKey,
        CargoDependencyType cargoDependencyType,
        Map<NameVersion, EnumSet<CargoDependencyType>> dependencyTypeMap
    ) {
        if (node == null) return;

        if (node instanceof TomlTable) {
            parseTomlTableNode((TomlTable) node, sectionKey, cargoDependencyType, dependencyTypeMap);
        } else if (node instanceof Map) {
            parseMapNode((Map<?, ?>) node, sectionKey, cargoDependencyType, dependencyTypeMap);
        } else if (node instanceof List) {
            parseListNode((List<?>) node, sectionKey, cargoDependencyType, dependencyTypeMap);
        }
    }

    private void parseTomlTableNode(
        TomlTable tableNode,
        String sectionKey,
        CargoDependencyType cargoDependencyType,
        Map<NameVersion, EnumSet<CargoDependencyType>> dependencyTypeMap
    ) {
        for (String key : tableNode.keySet()) {
            Object value = tableNode.get(sanitizeKey(key));

            if (key.equals(sectionKey)) {
                parseDependenciesFromSection(value, cargoDependencyType, dependencyTypeMap);
            }

            if (value instanceof TomlTable || value instanceof Map || value instanceof List) {
                parseNestedDependencies(value, sectionKey, cargoDependencyType, dependencyTypeMap);
            }
        }
    }

    private void parseMapNode(
        Map<?, ?> mapNode,
        String sectionKey,
        CargoDependencyType cargoDependencyType,
        Map<NameVersion, EnumSet<CargoDependencyType>> dependencyTypeMap
    ) {
        for (Map.Entry<?, ?> entry : mapNode.entrySet()) {
            Object value = entry.getValue();

            if (entry.getKey().toString().equals(sectionKey)) {
                parseDependenciesFromSection(value, cargoDependencyType, dependencyTypeMap);
            }

            if (value instanceof TomlTable || value instanceof Map || value instanceof List) {
                parseNestedDependencies(value, sectionKey, cargoDependencyType, dependencyTypeMap);
            }
        }
    }

    private void parseListNode(
        List<?> listNode,
        String sectionKey,
        CargoDependencyType cargoDependencyType,
        Map<NameVersion, EnumSet<CargoDependencyType>> dependencyTypeMap
    ) {
        for (Object elem : listNode) {
            parseNestedDependencies(elem, sectionKey, cargoDependencyType, dependencyTypeMap);
        }
    }

    private void parseDependenciesFromSection(
        Object value,
        CargoDependencyType cargoDependencyType,
        Map<NameVersion, EnumSet<CargoDependencyType>> dependencyTypeMap
    ) {
        if (value instanceof TomlTable) {
            TomlTable depsTable = (TomlTable) value;
            for (String depName : depsTable.keySet()) {
                String version = extractVersion(depsTable.get(depName));
                addDependency(depName, version, cargoDependencyType, dependencyTypeMap);
            }
        } else if (value instanceof Map) {
            Map<?, ?> depsMap = (Map<?, ?>) value;
            for (Map.Entry<?, ?> depEntry : depsMap.entrySet()) {
                String depName = depEntry.getKey().toString();
                String version = extractVersion(depEntry.getValue());
                addDependency(depName, version, cargoDependencyType, dependencyTypeMap);
            }
        }
    }

    private void addDependency(
        String depName,
        String version,
        CargoDependencyType cargoDependencyType,
        Map<NameVersion, EnumSet<CargoDependencyType>> dependencyTypeMap
    ) {
        NameVersion nv = new NameVersion(depName, version);
        dependencyTypeMap
            .computeIfAbsent(nv, k -> EnumSet.noneOf(CargoDependencyType.class))
            .add(cargoDependencyType);
    }

    private String extractVersion(Object depValue) {
        if (depValue == null) return null;
        if (depValue instanceof String) return (String) depValue;

        if (depValue instanceof TomlTable) {
            TomlTable table = (TomlTable) depValue;
            if (table.contains(VERSION_KEY)) {
                Object versionObj = table.get(VERSION_KEY);
                if (versionObj instanceof String) {
                    return (String) versionObj;
                }
            }
        }

        if (depValue instanceof Map) {
            Object version = ((Map<?, ?>) depValue).get(VERSION_KEY);
            if (version instanceof String) {
                return (String) version;
            }
        }

        return null;
    }

    // Helper method to sanitize TOML keys for getTable()
    private String sanitizeKey(String key) {
        // Escape double quotes inside the key
        String escaped = key.replace("\"", "\\\"");
        // If key contains any character outside valid bare keys, wrap in quotes
        if (!key.matches("^[A-Za-z0-9_-]+$")) {
            return "\"" + escaped + "\"";
        }
        return key;
    }
}
