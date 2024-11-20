package com.blackduck.integration.detectable.detectables.npm;

import org.slf4j.Logger;

import com.blackduck.integration.detectable.detectables.npm.lockfile.model.NpmRequires;

@FunctionalInterface
public interface MissingNpmDependencyHandler {
    void handleMissingDependency(Logger logger, NpmRequires missingDependency);
}
