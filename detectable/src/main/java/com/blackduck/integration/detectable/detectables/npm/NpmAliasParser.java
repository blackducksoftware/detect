package com.blackduck.integration.detectable.detectables.npm;

/**
 * Utility class for parsing npm package aliases.
 * Npm aliases allow packages to be installed under a different name using the format:
 * "alias-name": "npm:actual-package@version"
 */
public class NpmAliasParser {

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
}
