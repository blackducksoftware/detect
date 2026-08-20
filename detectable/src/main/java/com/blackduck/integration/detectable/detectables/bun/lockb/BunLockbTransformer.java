package com.blackduck.integration.detectable.detectables.bun.lockb;

import java.util.List;

import com.blackduck.integration.bdio.graph.builder.MissingExternalIdException;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.yarn.YarnTransformer;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockResult;
import com.blackduck.integration.util.NameVersion;

public class BunLockbTransformer {
    private final YarnTransformer delegate;

    public BunLockbTransformer(YarnTransformer delegate) {
        this.delegate = delegate;
    }

    public List<CodeLocation> generateCodeLocations(YarnLockResult yarnLockResult, List<NameVersion> externalDependencies) throws MissingExternalIdException {
        // Pass null workspace filter so only the root package.json seeds direct deps;
        // the 2-arg overload would unconditionally mark every lock entry as a direct dependency.
        return delegate.generateCodeLocations(yarnLockResult, externalDependencies, null);
    }
}
