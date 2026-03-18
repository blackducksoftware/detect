package com.blackduck.integration.detectable.detectables.setuptools.parse;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.tomlj.TomlParseResult;

import com.blackduck.integration.detectable.python.util.PythonDependency;
import com.blackduck.integration.detectable.python.util.PythonDependencyTransformer;

import static com.blackduck.integration.detectable.detectables.setuptools.parse.SetupToolsExtrasUtils.buildExtrasTransitives;

public class SetupToolsCfgParser implements SetupToolsParser {

    private static final String TOML_PROJECT_NAME_KEY = "project.name";
    private static final String TOML_PROJECT_VERSION_KEY = "project.version";

    private static final String CFG_NAME_PREFIX = "name";
    private static final String CFG_INSTALL_REQUIRES_PREFIX = "install_requires=";
    private static final String CFG_EXTRAS_REQUIRE_SECTION = "[options.extras_require]";

    private static final String CONDITIONAL_MARKER = ";";
    private static final String KEY_VALUE_SEPARATOR = "=";

    // Matches a new cfg key line: e.g. "python_requires = >=3.10"
    private static final String NEW_KEY_LINE_REGEX = "^\\s*[a-zA-Z0-9_.-]+\\s*=\\s*(?![=!<>~]).*$";

    private final TomlParseResult parsedToml;

    private String projectName;

    private final List<String> dependencies;

    private final Map<String, List<String>> extrasRequireMap;

    public SetupToolsCfgParser(TomlParseResult parsedToml) {
        this.parsedToml = parsedToml;
        this.dependencies = new ArrayList<>();
        this.extrasRequireMap = new HashMap<>();
    }

    @Override
    public SetupToolsParsedResult parse() throws IOException {
        String tomlProjectName = parsedToml.getString(TOML_PROJECT_NAME_KEY);
        String projectVersion = parsedToml.getString(TOML_PROJECT_VERSION_KEY);

        // If we have multiple project names the name from the toml wins
        // I've only seen version information in the toml so use that.
        String finalProjectName = (tomlProjectName != null && !tomlProjectName.isEmpty()) ? tomlProjectName : projectName;

        List<PythonDependency> parsedDirectDependencies = parseDirectDependencies();

        Map<String, List<PythonDependency>> extrasTransitives = buildExtrasTransitives(dependencies, extrasRequireMap);

        return new SetupToolsParsedResult(finalProjectName, projectVersion, parsedDirectDependencies, extrasTransitives);
    }

    /**
     * Extracts, does not parse, any entries in the install_requires section of the
     * setup.cfg
     *
     * @param filePath path to the setup.cfg file
     * @return a list of dependencies extracted from the install_requires section
     * @throws FileNotFoundException
     * @throws IOException
     */
    public List<String> load(String filePath) throws FileNotFoundException, IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            // This flag is used to indicate whether we are currently reading the lines under the "install_requires" key
            boolean isInstallRequiresSection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith(CFG_NAME_PREFIX)) {
                    parseProjectName(line);
                }

                // Remove all whitespace from the line for key searching
                String keySearch = line.replaceAll("\\s", "");

                // If the line starts with "install_requires=", we've found the key we're interested in
                if (keySearch.startsWith(CFG_INSTALL_REQUIRES_PREFIX)) {
                    isInstallRequiresSection = true;
                    String[] parts = line.split(KEY_VALUE_SEPARATOR, 2);

                    // If there is a value and it's not empty, add it to the dependencies list
                    if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                        dependencies.add(parts[1].trim());
                    }
                }
                else if (isInstallRequiresSection) {
                    if (isEndofInstallRequiresSection(line)) {
                        break;
                    }
                    // If the line is not empty, add it to the dependencies list
                    else if (!line.isEmpty()) {
                        dependencies.add(line);
                    }
                }
            }
        }

        return dependencies;
    }

    private List<PythonDependency> parseDirectDependencies() {
        List<PythonDependency> results = new LinkedList<>();

        PythonDependencyTransformer dependencyTransformer = new PythonDependencyTransformer();

        for (String dependencyLine : dependencies) {
            PythonDependency dependency = dependencyTransformer.transformLine(dependencyLine);

            // If we have a ; in our requirements line then there is a condition on this dependency.
            // We want to know this so we don't consider it a failure later if we try to run pip show
            // on it and we don't find it.
            if (dependencyLine.contains(CONDITIONAL_MARKER)) {
                dependency.setConditional(true);
            }

            if (dependency != null) {
                results.add(dependency);
            }
        }

        return results;
    }

    public void parseProjectName(String line) {
        String[] parts = line.split(KEY_VALUE_SEPARATOR, 2);
        if (parts.length > 1 && !parts[1].trim().isEmpty()) {
            projectName = parts[1].trim();
        }
    }

    /**
     * Extracts entries in the [options.extras_require] section of the setup.cfg,
     * grouped by extras group name.
     *
     * @param filePath path to the setup.cfg file
     * @return a map of group name to list of dependency strings
     * @throws FileNotFoundException
     * @throws IOException
     */
    public Map<String, List<String>> loadExtrasRequire(String filePath) throws FileNotFoundException, IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isExtrasRequireSection = false;
            String currentGroup = null;

            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();

                if (trimmedLine.equals(CFG_EXTRAS_REQUIRE_SECTION)) {
                    isExtrasRequireSection = true;
                    continue;
                }

                if (isExtrasRequireSection) {
                    // A new section header means we've left [options.extras_require]
                    if (trimmedLine.startsWith("[")) {
                        break;
                    }

                    // Skip empty lines
                    if (trimmedLine.isEmpty()) {
                        continue;
                    }

                    // In INI format, continuation lines (dependencies) are always indented.
                    // Group key lines (e.g., "http2 =", "security =") are not indented.
                    if (line.length() > 0 && (line.charAt(0) == ' ' || line.charAt(0) == '\t')) {
                        // Indented line — it's a dependency under the current group
                        if (currentGroup != null) {
                            extrasRequireMap.computeIfAbsent(currentGroup, k -> new ArrayList<>()).add(trimmedLine);
                        }
                    } else {
                        // Non-indented line — it's a group key (e.g., "security =" or "http2 =")
                        int equalsIndex = trimmedLine.indexOf('=');
                        if (equalsIndex >= 0) {
                            currentGroup = trimmedLine.substring(0, equalsIndex).trim();
                        }
                    }
                }
            }
        }

        return extrasRequireMap;
    }

    private boolean isEndofInstallRequiresSection(String line) {
        /*
         * If the line starts with a [ we have reached a new section and want to exit.
         *
         * The line.matches call looks for a new key.
         * It will return true if the string starts with optional whitespace,
         * followed by one or more alphanumeric characters, periods, underscores, or hyphens,
         * (which is the allowed set of characters for a key), followed by
         * optional whitespace, an equal sign, optional whitespace, and then any
         * character that is not another =, !, <, >, or ~ which would indicate a requirement
         * operator and not a new key.
         */
        return line.startsWith("[") || line.matches(NEW_KEY_LINE_REGEX);
    }
}
