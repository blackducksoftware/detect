package com.blackduck.integration.detectable.detectables.yarn.packagejson;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.blackduck.integration.detectable.detectables.npm.packagejson.model.YarnPackageJson;

public class NullSafePackageJson {
    private final YarnPackageJson rawPackageJson;

    public NullSafePackageJson(YarnPackageJson rawPackageJson) {
        this.rawPackageJson = rawPackageJson;
    }

    public Optional<String> getName() {
        if (rawPackageJson.name == null) {
            return Optional.empty();
        }
        return Optional.of(rawPackageJson.name);
    }

    public String getNameString() {
        return getName().orElse("");
    }

    public Optional<String> getVersion() {
        if (rawPackageJson.version == null) {
            return Optional.empty();
        }
        return Optional.of(rawPackageJson.version);
    }

    public String getVersionString() {
        return getVersion().orElse("");
    }

    public Map<String, String> getDependencies() {
        if (rawPackageJson.dependencies == null) {
            return new HashMap<>();
        }
        return rawPackageJson.dependencies;
    }

    public Map<String, String> getDevDependencies() {
        if (rawPackageJson.devDependencies == null) {
            return new HashMap<>();
        }
        return rawPackageJson.devDependencies;
    }

    public Map<String, String> getOptionalDependencies() {
        if (rawPackageJson.optionalDependencies == null) {
            return new HashMap<>();
        }
        return rawPackageJson.optionalDependencies;
    }

    public Map<String, String> getPeerDependencies() {
        if (rawPackageJson.peerDependencies == null) {
            return new HashMap<>();
        }
        return rawPackageJson.peerDependencies;
    }
}
