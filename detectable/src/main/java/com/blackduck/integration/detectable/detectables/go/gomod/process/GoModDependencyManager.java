package com.blackduck.integration.detectable.detectables.go.gomod.process;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.blackduck.integration.util.NameVersion;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.go.gomod.model.GoListAllData;
import com.blackduck.integration.detectable.detectables.go.gomod.model.ReplaceData;

public class GoModDependencyManager {
    private static final String INCOMPATIBLE_SUFFIX = "+incompatible";
    private static final String SHA1_REGEX = "[a-fA-F0-9]{40}";
    private static final String SHORT_SHA1_REGEX = "[a-fA-F0-9]{12}";
    private static final String GIT_VERSION_FORMAT = ".*(%s).*";
    private static final Pattern SHA1_VERSION_PATTERN = Pattern.compile(String.format(GIT_VERSION_FORMAT, SHA1_REGEX));
    private static final Pattern SHORT_SHA1_VERSION_PATTERN = Pattern.compile(String.format(GIT_VERSION_FORMAT, SHORT_SHA1_REGEX));

    private final ExternalIdFactory externalIdFactory;
    private final Map<String, Dependency> modulesAsDependencies;
    private final Map<String, String> modifiedVersionsMap;
    private final Map<String, NameVersion> originalRequiredNameAndVersion;

    public GoModDependencyManager(List<GoListAllData> allRequiredModules, ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
        modifiedVersionsMap = new HashMap<>(); // might no longer be necessary
        originalRequiredNameAndVersion = new HashMap<>();
        modulesAsDependencies = convertModulesToDependencies(allRequiredModules);

    }

    /**
     * A Go Module consists of a path (aka its name) and its version. Before converting to a Dependency, we apply any
     * replace directives and commit hash version truncation when applicable.
     * @param allModules
     * @return
     */
    private Map<String, Dependency> convertModulesToDependencies(List<GoListAllData> allModules) {
        Map<String, Dependency> dependencies = new HashMap<>();

        for (GoListAllData module : allModules) {
            if (module.getPath().contains("table")) {
                System.out.println("applying replace directive to table");
            }

            /// Keep track of original module path and version
            NameVersion ogNameVersion = new NameVersion(module.getPath(), module.getVersion()); // without replacements, without truncating commit hash version.
            originalRequiredNameAndVersion.putIfAbsent(module.getPath(), ogNameVersion);

            /// Apply replacements and version truncation if applicable
            String name = Optional.ofNullable(module.getReplace())
                .map(ReplaceData::getPath)
                .orElse(module.getPath());
            String version = Optional.ofNullable(module.getReplace())
                .map(ReplaceData::getVersion)
                .orElse(module.getVersion());
            if (version != null) {
                String kbCompatibleVersion = getKbCompatibleVersion(version);
                if (!version.equals(kbCompatibleVersion)) {
                    modifiedVersionsMap.put(kbCompatibleVersion, version);
                    version = kbCompatibleVersion;
                }
            }

            ///  Add dependency (will have replaced name/version = truncated commit hash if applicable)
            dependencies.put(module.getPath(), convertToDependency(name, version)); // potentially modified if applicable
        }

        return dependencies;
    }

    private String getKbCompatibleVersion(String version) {
        String kbCompatibleVersion;
        kbCompatibleVersion = handleGitHash(version);
        kbCompatibleVersion = removeIncompatibleSuffix(kbCompatibleVersion);
        return kbCompatibleVersion;
    }

    public NameVersion getOriginalRequiredNameAndVersion(String moduleName) {
           return originalRequiredNameAndVersion.get(moduleName); // todo handle null/not contain key
    }

    public String getOriginalVersionFromKbCompatibleVersion(String shortVersion) {
        if (modifiedVersionsMap.containsKey(shortVersion)) {
            return modifiedVersionsMap.get(shortVersion);
        }
        return shortVersion;
    }

    public Collection<Dependency> getRequiredDependencies() {
        return modulesAsDependencies.values();
    }

    public Dependency getDependencyForModule(String moduleName) {
        return modulesAsDependencies.getOrDefault(moduleName, convertToDependency(moduleName, null));
    }

    // When a version contains a commit hash, the KB only accepts the git hash, so we must strip out the rest.
    private String handleGitHash(String version) {
        return getVersionFromPattern(version, SHA1_VERSION_PATTERN)
            .orElseGet(() ->
                getVersionFromPattern(version, SHORT_SHA1_VERSION_PATTERN)
                    .orElse(version)
            );
    }

    private Dependency convertToDependency(String moduleName, @Nullable String moduleVersion) {
        return new Dependency(moduleName, moduleVersion, externalIdFactory.createNameVersionExternalId(Forge.GOLANG, moduleName, moduleVersion));
    }

    private Optional<String> getVersionFromPattern(String version, Pattern versionPattern) {
        Matcher matcher = versionPattern.matcher(version);
        if (matcher.matches()) {
            return Optional.ofNullable(StringUtils.trim(matcher.group(1)));
        }
        return Optional.empty();
    }

    // https://golang.org/ref/mod#incompatible-versions
    private String removeIncompatibleSuffix(String version) {
        if (version.endsWith(INCOMPATIBLE_SUFFIX)) {
            // Trim incompatible suffix so that KB can match component
            version = version.substring(0, version.length() - INCOMPATIBLE_SUFFIX.length());
        }
        return version;
    }
}
