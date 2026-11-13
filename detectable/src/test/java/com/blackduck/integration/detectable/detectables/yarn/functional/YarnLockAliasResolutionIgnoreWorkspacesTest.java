package com.blackduck.integration.detectable.detectables.yarn.functional;

import java.io.IOException;

import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;

/**
 * Same lockfile as {@link YarnLockAliasResolutionTest}, through the graph builder used when all workspaces are ignored.
 * That builder lists every lockfile entry as a direct dependency, so only alias identity and children are asserted.
 */
public class YarnLockAliasResolutionIgnoreWorkspacesTest extends YarnLockAliasResolutionTest {

    public YarnLockAliasResolutionIgnoreWorkspacesTest() throws IOException {
        super();
    }

    @Override
    protected boolean ignoreAllWorkspaces() {
        return true;
    }

    @Override
    protected void assertModeSpecific(NameVersionGraphAssert graphAssert) {
        graphAssert.hasRootDependency("string-width", "4.2.3");
        graphAssert.hasRootDependency("left-pad", "1.3.0");
        graphAssert.hasRootDependency("@scope/real", "1.0.0");
        graphAssert.hasRootDependency("consumer", "1.0.0");
    }
}
