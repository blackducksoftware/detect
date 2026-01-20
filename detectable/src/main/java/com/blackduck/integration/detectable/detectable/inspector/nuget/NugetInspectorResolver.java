package com.blackduck.integration.detectable.detectable.inspector.nuget;

import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.nuget.NugetInspectorOptions;

public interface NugetInspectorResolver {
    ExecutableTarget resolveNugetInspector(NugetInspectorOptions nugetInspectorOptions) throws DetectableException;
}
