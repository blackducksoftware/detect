package com.blackduck.integration.detectable.detectables.maven.resolver;

import java.util.Arrays;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.internal.impl.scope.OptionalDependencySelector;

/**
 * SessionBuilderSupplier variant that enables TEST scope in the scope selector and
 * filters transitive dependencies (test/provided/optional) via TransitiveScopeFilteringSelector.
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

        DependencySelector optional = OptionalDependencySelector.fromDirect();
        DependencySelector exclusion = new ExclusionDependencySelector();
        DependencySelector transitiveFilter = new TransitiveScopeFilteringSelector();
        DependencySelector nearestWinsNoRange = new NearestWinsNoRangeSelector();

        // Compose selectors: scope allows TEST, optional/exclusion keep defaults, transitiveFilter removes unwanted transitives
        return new AndDependencySelector(scopeSelector, optional, exclusion, transitiveFilter, nearestWinsNoRange);
    }
}
