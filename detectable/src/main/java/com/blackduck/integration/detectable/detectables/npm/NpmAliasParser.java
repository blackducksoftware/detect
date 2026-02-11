package com.blackduck.integration.detectable.detectables.npm;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MultiValuedMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for parsing npm package aliases.
 * Npm aliases allow packages to be installed under a different name using the format:
 * "alias-name": "npm:actual-package@version"
 */
public class NpmAliasParser {
    private static final Logger logger = LoggerFactory.getLogger(NpmAliasParser.class);

    /**
     * Parses an npm alias string to extract the actual package name.
     * Npm aliases have the format: "npm:package@version" or "npm:@scope/package@version"
     *
     * For scoped packages (starting with @), there are two @ symbols:
     *   - First @ is part of the scope name
     *   - Second @ (after the /) separates the package name from the version
     *
     * @param aliasValue The full alias string (e.g., "npm:package@^1.0.0" or "npm:@scope/package@^7.0.0")
     * @return Array with [0] = package name, [1] = version specifier, or null if not an alias
     */
    public static String[] parseNpmAlias(String aliasValue) {
        if (aliasValue == null || !aliasValue.startsWith("npm:")) {
            return null;
        }

        String normalizedPackage = aliasValue.substring(4); // Remove "npm:" prefix
        int versionAtIndex = -1;

        // For scoped packages (e.g., @scope/package@^7.0.0), find the @ after the /
        // For non-scoped packages (e.g., package@^1.0.0), find the first @
        if (normalizedPackage.startsWith("@")) {
            int slashIndex = normalizedPackage.indexOf('/');
            if (slashIndex > 0) {
                versionAtIndex = normalizedPackage.indexOf('@', slashIndex + 1);
            }
        } else {
            versionAtIndex = normalizedPackage.indexOf('@');
        }

        if (versionAtIndex > 0) {
            return new String[] {
                normalizedPackage.substring(0, versionAtIndex),
                normalizedPackage.substring(versionAtIndex + 1)
            };
        } else {
            // No version specified (e.g., "npm:package" or "npm:@scope/package")
            return new String[] { normalizedPackage, normalizedPackage };
        }
    }

    /**
     * Checks if a dependency value string is an npm alias.
     *
     * @param value The dependency value from package.json
     * @return true if the value is an npm alias (starts with "npm:")
     */
    public static boolean isNpmAlias(String value) {
        return value != null && value.startsWith("npm:");
    }

    /**
     * Extracts just the package name from an npm alias.
     *
     * @param aliasValue The full alias string
     * @return The actual package name, or null if not an alias
     */
    public static String extractPackageName(String aliasValue) {
        String[] parsed = parseNpmAlias(aliasValue);
        return parsed != null ? parsed[0] : null;
    }

    /**
     * Builds a mapping of alias names to actual package names from a MultiValuedMap of dependencies.
     * Scans the provided dependencies looking for entries with "npm:" prefix.
     *
     * @param dependencies The dependencies map from package.json
     * @param aliasMapping The map to populate with alias -> actual name mappings
     */
    public static void scanDependenciesForAliases(@Nullable MultiValuedMap<String, String> dependencies, Map<String, String> aliasMapping) {
        if (dependencies == null) {
            return;
        }

        for (Map.Entry<String, String> entry : dependencies.entries()) {
            String aliasName = entry.getKey();
            String versionSpec = entry.getValue();

            if (isNpmAlias(versionSpec)) {
                String actualPackageName = extractPackageName(versionSpec);
                if (actualPackageName != null) {
                    aliasMapping.put(aliasName, actualPackageName);
                    logger.debug("Found npm alias: {} -> {}", aliasName, actualPackageName);
                }
            }
        }
    }

    /**
     * Builds a complete alias mapping from all dependency types in a package.json.
     * Scans dependencies, devDependencies, peerDependencies, and optionalDependencies.
     *
     * @param dependencies Regular dependencies
     * @param devDependencies Dev dependencies
     * @param peerDependencies Peer dependencies
     * @param optionalDependencies Optional dependencies
     * @return Map of alias name -> actual package name
     */
    public static Map<String, String> buildAliasMapping(
        @Nullable MultiValuedMap<String, String> dependencies,
        @Nullable MultiValuedMap<String, String> devDependencies,
        @Nullable MultiValuedMap<String, String> peerDependencies,
        @Nullable MultiValuedMap<String, String> optionalDependencies
    ) {
        Map<String, String> aliasMapping = new HashMap<>();
        scanDependenciesForAliases(dependencies, aliasMapping);
        scanDependenciesForAliases(devDependencies, aliasMapping);
        scanDependenciesForAliases(peerDependencies, aliasMapping);
        scanDependenciesForAliases(optionalDependencies, aliasMapping);
        return aliasMapping;
    }
}
