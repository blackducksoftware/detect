package com.blackduck.integration.detectable.detectables.maven.resolver;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.internal.impl.scope.OptionalDependencySelector;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;

import java.util.Arrays;

public class BaseSessionBuilderSupplier extends SessionBuilderSupplier {

    public BaseSessionBuilderSupplier(RepositorySystem repositorySystem) {
        super(repositorySystem);
    }

    @Override
    protected DependencySelector getDependencySelector() {
        return new AndDependencySelector(
                new ScopeDependencySelector(
                        Arrays.asList(
                                JavaScopes.COMPILE,
                                JavaScopes.RUNTIME,
                                JavaScopes.PROVIDED,
                                JavaScopes.SYSTEM
                        ),
                        null
                ),
                OptionalDependencySelector.fromDirect(),
                new ExclusionDependencySelector(),
                new TransitiveScopeFilteringSelector(),
                new NearestWinsNoRangeSelector()
        );
    }
}
