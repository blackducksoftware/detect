package com.blackduck.integration.detectable.detectables.maven.resolver;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters transitives: rejects test, provided, and optional=true dependencies beyond the first level.
 * Keeps all direct dependencies (depth 1) regardless of scope, so declared test deps remain.
 */
public final class TransitiveScopeFilteringSelector implements DependencySelector {
    private static final Logger logger = LoggerFactory.getLogger(TransitiveScopeFilteringSelector.class);
    private final int depth;

    public TransitiveScopeFilteringSelector() {
        this(0);
        logger.debug("TransitiveScopeFilteringSelector initialized at depth {}", depth);
    }

    private TransitiveScopeFilteringSelector(int depth) {
        this.depth = depth;
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        // depth meaning: 0=root, 1=direct children, >=2=transitives
        if (depth <= 1) {
            // Direct dependencies always allowed
            return true;
        }
        String scope = dependency.getScope();
        boolean optional = dependency.isOptional();
        if (optional) {
            logger.info("Filtering OPTIONAL transitive dependency: {}:{}:{} scope={} depth={}",
                safe(dependency.getArtifact().getGroupId()),
                safe(dependency.getArtifact().getArtifactId()),
                safe(dependency.getArtifact().getVersion()),
                scope,
                depth);
            return false;
        }
        if (scope != null) {
            String s = scope.toLowerCase();
            if ("test".equals(s) || "provided".equals(s)) {
                logger.info("Filtering transitive dependency by scope: {}:{}:{} scope={} depth={}",
                    safe(dependency.getArtifact().getGroupId()),
                    safe(dependency.getArtifact().getArtifactId()),
                    safe(dependency.getArtifact().getVersion()),
                    s,
                    depth);
                return false;
            }
        }
        return true;
    }

    @Override
    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        int nextDepth = depth + 1;
        Object current = null;
        try {
            current = context != null ? context.getDependency() : null;
        } catch (Throwable ignore) {
            // API differences across Aether versions; be resilient
        }
        logger.debug("Deriving child selector: current depth={}, next depth={} for context dependency={}", depth, nextDepth, current);
        return new TransitiveScopeFilteringSelector(nextDepth);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
