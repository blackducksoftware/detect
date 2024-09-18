package com.blackduck.integration.detectable.detectable.executable.resolver;

import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;

public interface GradleResolver {
    ExecutableTarget resolveGradle(DetectableEnvironment environment) throws DetectableException;
}
