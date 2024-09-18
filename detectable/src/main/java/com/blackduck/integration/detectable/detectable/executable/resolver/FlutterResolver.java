package com.blackduck.integration.detectable.detectable.executable.resolver;

import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import org.jetbrains.annotations.Nullable;

public interface FlutterResolver {
    @Nullable
    ExecutableTarget resolveFlutter() throws DetectableException;
}
