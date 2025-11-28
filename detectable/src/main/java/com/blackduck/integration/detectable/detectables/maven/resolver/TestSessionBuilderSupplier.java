package com.blackduck.integration.detectable.detectables.maven.resolver;

import java.util.Arrays;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;

/**
 * SessionBuilderSupplier variant that enables TEST scope in the scope selector.
 * This implementation composes a ScopeDependencySelector that includes TEST with
 * OptionalDependencySelector.fromDirect() and ExclusionDependencySelector so the
 * session preserves optional/exclusion semantics while allowing TEST traversal.
 */
public class TestSessionBuilderSupplier extends SessionBuilderSupplier {
    public TestSessionBuilderSupplier(RepositorySystem repositorySystem) {
        super(repositorySystem);
    }

    @Override
    protected DependencySelector getDependencySelector() {
        ScopeDependencySelector scopeSelector = new ScopeDependencySelector(
            Arrays.asList(
                JavaScopes.COMPILE,
                JavaScopes.RUNTIME,
                JavaScopes.PROVIDED,
                JavaScopes.SYSTEM,
                JavaScopes.TEST
            ),
            null
        );

        // Use the same optional/exclusion behavior the default builder uses to avoid
        // removing important filtering (OptionalDependencySelector + ExclusionDependencySelector)
        org.eclipse.aether.collection.DependencySelector optional = org.eclipse.aether.internal.impl.scope.OptionalDependencySelector.fromDirect();
        org.eclipse.aether.util.graph.selector.ExclusionDependencySelector exclusion = new org.eclipse.aether.util.graph.selector.ExclusionDependencySelector();

        return new org.eclipse.aether.util.graph.selector.AndDependencySelector(scopeSelector, optional, exclusion);
    }
}
