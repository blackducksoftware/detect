package com.blackduck.integration.detectable.detectables.cargo.parse;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.cargo.CargoDependencyType;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;

import com.blackduck.integration.util.NameVersion;
import org.tomlj.TomlTable;

public class CargoTomlParser {
    private static final String NAME_KEY = "name";
    private static final String VERSION_KEY = "version";
    private static final String PACKAGE_KEY = "package";
    private static final String WORKSPACE_KEY = "workspace";
    private static final String WORKSPACE_MEMBER_KEY = "members";
    private static final String WORKSPACE_EXCLUSION_KEY = "exclude";
    private static final String WORKSPACE_PATH_SEPARATOR = "/";
    private static final String NORMAL_DEPENDENCIES_KEY = "dependencies";
    private static final String BUILD_DEPENDENCIES_KEY = "build-dependencies";
    private static final String DEV_DEPENDENCIES_KEY = "dev-dependencies";

    private Map<String, String> workspaceDependencies = new HashMap<>();

    public void setWorkspaceDependencies(String tomlFileContents) {
        this.workspaceDependencies = parseRootWorkspaceDependencies(tomlFileContents);
    }

    public Optional<NameVersion> parseNameVersionFromCargoToml(String tomlFileContents) {
        TomlParseResult cargoTomlObject = Toml.parse(tomlFileContents);
        TomlTable packageTable = cargoTomlObject.getTable(PACKAGE_KEY);
        if (packageTable == null || !packageTable.contains(NAME_KEY)) {
            return Optional.empty();
        }

        String name = packageTable.getString(NAME_KEY);
        String version = null;
        Object versionObj = packageTable.get(VERSION_KEY);

        if (versionObj instanceof String) {
            version = (String) versionObj;
        } else if (versionObj instanceof TomlTable && Boolean.TRUE.equals(((TomlTable) versionObj).getBoolean(WORKSPACE_KEY))) {
            TomlTable workspacePackage = cargoTomlObject.getTable(WORKSPACE_KEY) != null ? Objects.requireNonNull(cargoTomlObject.getTable(WORKSPACE_KEY)).getTable(PACKAGE_KEY) : null;
            if (workspacePackage != null) {
                version = workspacePackage.getString(VERSION_KEY);
            }
        }

        return Optional.of(new NameVersion(name, version));
    }

    public String parsePackageNameFromCargoToml(String tomlFileContents) {
        TomlParseResult toml = Toml.parse(tomlFileContents);
        TomlTable packageTable = toml.getTable(PACKAGE_KEY);
        if (packageTable != null && packageTable.contains(NAME_KEY)) {
            return packageTable.getString(NAME_KEY);
        }
        return null;
    }

    public Set<String> parseActiveWorkspaceMembers(String tomlFileContents, File workspaceRoot) {
        TomlParseResult toml = Toml.parse(tomlFileContents);
        Set<String> members = new HashSet<>();

        // Check for parsing errors
        if (toml.hasErrors()) {
            return members; // Return empty set if TOML is malformed
        }

        Set<String> exclusions = new HashSet<>();

        TomlTable workspace = toml.getTable(WORKSPACE_KEY);
        if (workspace != null) {
            // Parse members
            parseWorkspaceArray(workspace, WORKSPACE_MEMBER_KEY, workspaceRoot, members);

            // Parse exclusions
            parseWorkspaceArray(workspace, WORKSPACE_EXCLUSION_KEY, workspaceRoot, exclusions);
        }

        // Apply exclusions - remove any member that matches an exclusion pattern
        members.removeIf(exclusions::contains);

        return members;
    }

    public Set<String> parseAllWorkspaceMembers(String tomlFileContents, File workspaceRoot) {
        TomlParseResult toml = Toml.parse(tomlFileContents);
        Set<String> members = new HashSet<>();

        // Check for parsing errors
        if (toml.hasErrors()) {
            return members; // Return empty set if TOML is malformed
        }

        TomlTable workspace = toml.getTable(WORKSPACE_KEY);
        if (workspace != null) {
            // Parse members
            parseWorkspaceArray(workspace, WORKSPACE_MEMBER_KEY, workspaceRoot, members);
        }
        return members;
    }

    private void parseWorkspaceArray(TomlTable workspace, String key, File workspaceRoot, Set<String> targetSet) {
        if (workspace.contains(key)) {
            TomlArray array = workspace.getArray(key);
            if (array != null) {
                for (int i = 0; i < array.size(); i++) {
                    String value = array.getString(i);
                    if (value != null) {
                        processMember(value, workspaceRoot, targetSet);
                    }
                }
            }
        }
    }

    private void processMember(String member, File workspaceRoot, Set<String> members) {
        if (member == null || member.equals(".")) {
            return;
        }

        if (member.contains("*")) {
            members.addAll(expandGlobPattern(member, workspaceRoot));
        } else {
            members.add(member);
        }
    }

    private Set<String> expandGlobPattern(String globPattern, File workspaceRoot) {
        Set<String> expandedMembers = new HashSet<>();

        if (workspaceRoot == null || !workspaceRoot.exists()) {
            return expandedMembers;
        }

        int firstSlashIndex = globPattern.indexOf('/');
        if (firstSlashIndex <= 0) {
            return expandedMembers;
        }

        String prefix = globPattern.substring(0, firstSlashIndex);
        File baseDir = new File(workspaceRoot, prefix);

        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return expandedMembers;
        }

        // Validate that baseDir is within workspace root (prevent path traversal)
        try {
            File canonicalBase = baseDir.getCanonicalFile();
            File canonicalRoot = workspaceRoot.getCanonicalFile();
            if (!canonicalBase.getPath().startsWith(canonicalRoot.getPath())) {
                return expandedMembers; // Reject path traversal
            }
        } catch (IOException e) {
            return expandedMembers; // Reject if canonicalization fails
        }

        File[] subDirs = baseDir.listFiles(File::isDirectory);
        if (subDirs == null) {
            return expandedMembers;
        }

        for (File subDir : subDirs) {
            addWorkspaceMemberIfExists(subDir, prefix, expandedMembers);
        }

        return expandedMembers;
    }

    private void addWorkspaceMemberIfExists(File subDir, String prefix, Set<String> expandedMembers) {
        File cargoToml = new File(subDir, "Cargo.toml");
        if (!cargoToml.exists()) {
            return;
        }

        String relativePath = prefix + WORKSPACE_PATH_SEPARATOR + subDir.getName();
        expandedMembers.add(relativePath);
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
        parseDependenciesFromTomlTable(toml, BUILD_DEPENDENCIES_KEY, CargoDependencyType.BUILD, dependencyTypeMap );
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

    /**
     * Parses workspace-level dependency versions from the root Cargo.toml file.
     *
     * In Cargo workspace projects, dependencies can be defined once in [workspace.dependencies],
     * [workspace.build-dependencies], or [workspace.dev-dependencies] sections of the root Cargo.toml.
     * Workspace members can then reference these dependencies using `.workspace = true` syntax instead
     * of specifying versions directly. This method extracts all workspace-level dependencies and their
     * versions, which are later used to resolve dependencies in member crates that use workspace inheritance.
     *
     * @param tomlFileContents The content of the root Cargo.toml file
     * @return A map of dependency names to their versions defined at the workspace level
     */
    public Map<String, String> parseRootWorkspaceDependencies(String tomlFileContents) {
        TomlParseResult toml = Toml.parse(tomlFileContents);
        Map<String, String> rootWorkspaceDependencies = new HashMap<>();
        TomlTable workspace = toml.getTable(WORKSPACE_KEY);

        if (workspace != null) {
            // Parse [workspace.dependencies]
            extractWorkspaceDependencies(workspace, NORMAL_DEPENDENCIES_KEY, rootWorkspaceDependencies);

            // Parse [workspace.build-dependencies]
            extractWorkspaceDependencies(workspace, BUILD_DEPENDENCIES_KEY, rootWorkspaceDependencies);

            // Parse [workspace.dev-dependencies]
            extractWorkspaceDependencies(workspace, DEV_DEPENDENCIES_KEY, rootWorkspaceDependencies);
        }
        return rootWorkspaceDependencies;
    }

    private void extractWorkspaceDependencies(TomlTable workspace, String sectionKey, Map<String, String> rootWorkspaceDependencies) {
        TomlTable dependencies = workspace.getTable(sectionKey);
        if (dependencies != null) {
            for (String key : dependencies.keySet()) {
                String version = extractVersion(dependencies.get(key));
                if (version != null) {
                    rootWorkspaceDependencies.put(key, version);
                }
            }
        }
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
                    TomlTable depTable = (TomlTable) value;
                    // Check if this is a workspace reference
                    if (Boolean.TRUE.equals(depTable.getBoolean(WORKSPACE_KEY))) {
                        version = this.workspaceDependencies.get(key);
                    } else {
                        version = depTable.getString(VERSION_KEY);
                    }
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
                if(version == null) {
                    version = extractVersionWithWorkspace(depsTable.get(depName), depName);
                }
                addDependency(depName, version, cargoDependencyType, dependencyTypeMap);
            }
        } else if (value instanceof Map) {
            Map<?, ?> depsMap = (Map<?, ?>) value;
            for (Map.Entry<?, ?> depEntry : depsMap.entrySet()) {
                String depName = depEntry.getKey().toString();
                String version = extractVersion(depEntry.getValue());
                if(version == null) {
                    version = extractVersionWithWorkspace(depEntry.getValue(), depName);
                }
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

    private String extractVersionWithWorkspace(Object depValue, String depName) {
        if (depValue == null) return null;
        if (depValue instanceof String) return (String) depValue;

        if (depValue instanceof TomlTable) {
            TomlTable table = (TomlTable) depValue;
            if (Boolean.TRUE.equals(table.getBoolean(WORKSPACE_KEY))) {
                return this.workspaceDependencies.get(depName);
            }
            if (table.contains(VERSION_KEY)) {
                Object versionObj = table.get(VERSION_KEY);
                if (versionObj instanceof String) {
                    return (String) versionObj;
                }
            }
        }

        if (depValue instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) depValue;
            if (Boolean.TRUE.equals(map.get(WORKSPACE_KEY))) {
                return this.workspaceDependencies.get(depName);
            }
            Object version = map.get(VERSION_KEY);
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

    public boolean isVirtualWorkspace(String tomlFileContents) {
        TomlParseResult toml = Toml.parse(tomlFileContents);
        boolean hasWorkspace = toml.contains(WORKSPACE_KEY);
        boolean hasPackage = toml.contains(PACKAGE_KEY);
        return hasWorkspace && !hasPackage;
    }
}
