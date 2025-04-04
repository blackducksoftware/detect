package com.blackduck.integration.detectable.detectables.go.gomod.process;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private final Map<NameVersion, Dependency> modulesAsDependencies;

    public GoModDependencyManager(List<GoListAllData> allRequiredModules, ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
        modulesAsDependencies = convertModulesToDependencies(allRequiredModules);
    }

    private Map<NameVersion, Dependency> convertModulesToDependencies(List<GoListAllData> allModules) {
        Map<NameVersion, Dependency> dependencies = new HashMap<>();

        for (GoListAllData module : allModules) {
            String name = Optional.ofNullable(module.getReplace())
                .map(ReplaceData::getPath)
                .orElse(module.getPath());
            String version = Optional.ofNullable(module.getReplace())
                .map(ReplaceData::getVersion)
                .orElse(module.getVersion());
            if (version != null) {
                version = handleGitHash(version);
                version = removeIncompatibleSuffix(version);
            }
            dependencies.put(new NameVersion(name, version), convertToDependency(name, version));
        }

        return dependencies;
    }

    private Dependency convertToDependency(String moduleName, @Nullable String moduleVersion) {
        return new Dependency(moduleName, moduleVersion, externalIdFactory.createNameVersionExternalId(Forge.GOLANG, moduleName, moduleVersion));
    }

    @Deprecated // TODO confirm test cases still pass. Is there a test case for null version?
    /**
     * Returns the Dependency object associated with this module name. The version will be the SELECTED version for the build. any other version in the graph of this module would introduce a false +ve
     * If it doesn't exist, returns a Dependency that just has a name and null as its version. (is that null handled properly .. that who knows.)
     */
    public Dependency getDependencyForModule(NameVersion module) {
        Dependency dep = modulesAsDependencies.getOrDefault(module, convertToDependency(module.getName(), null)); // why does viper170 not exist? because it is not part of go list -m all. if we return a dep that is not in go list, we introduce a lot of false +ves
        if (dep.getVersion() == null) {
            System.out.println("No version found for " + module.getName());
        }
        return dep;
    }

    // When a version contains a commit hash, the KB only accepts the git hash, so we must strip out the rest.
    private String handleGitHash(String version) {
        return getVersionFromPattern(version, SHA1_VERSION_PATTERN)
            .orElseGet(() ->
                getVersionFromPattern(version, SHORT_SHA1_VERSION_PATTERN)
                    .orElse(version)
            );
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
