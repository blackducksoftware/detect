package com.blackduck.integration.detectable.detectable.executable.resolver;

import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;

public interface SwiftResolver {
    ExecutableTarget resolveSwift() throws DetectableException;
}