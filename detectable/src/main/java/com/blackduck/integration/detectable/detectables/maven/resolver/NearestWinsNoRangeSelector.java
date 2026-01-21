package com.blackduck.integration.detectable.detectables.maven.resolver;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.VersionRange;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class NearestWinsNoRangeSelector implements DependencySelector {

    private static final Logger logger = LoggerFactory.getLogger(NearestWinsNoRangeSelector.class);

    private static final VersionScheme VERSION_SCHEME =
            new GenericVersionScheme();

    private final Set<String> seen;
    private final int depth;

    // Configurable flags (hardcoded defaults)
    private final boolean preventReentry; // if true, avoid re-entering same GA
    private final boolean enableLogging;  // if true, emit debug logs

    public NearestWinsNoRangeSelector() {
        // Default: only transitive-range skipping is enforced; other behaviors off
        this(new HashSet<>(), 0, false, true);
    }

    // Full constructor used internally to propagate flags and state
    private NearestWinsNoRangeSelector(Set<String> seen, int depth, boolean preventReentry, boolean enableLogging) {
        this.seen = seen;
        this.depth = depth;
        this.preventReentry = preventReentry;
        this.enableLogging = enableLogging;
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        if (dependency == null || dependency.getArtifact() == null) return true;

        Artifact artifact = dependency.getArtifact();
        String version = artifact.getVersion();
        String ga = artifact.getGroupId() + ":" + artifact.getArtifactId();

        if (depth > 1 && version != null && isRangeSyntax(version)) {
            try {
                VersionConstraint constraint = VERSION_SCHEME.parseVersionConstraint(version);
                VersionRange range = constraint.getRange();

                if (range != null) {
                    boolean hasLower = range.getLowerBound() != null;
                    boolean hasUpper = range.getUpperBound() != null;

                    // Strategy 1: Block open-ended ranges (e.g., [1.0, ) or (, 2.0])
                    if (!hasLower || !hasUpper) {
                        logger.info("Blocking open-ended range {} for {} at depth {}", version, ga, depth);
                        return false;
                    }

                    // Strategy 2: Block even bounded ranges if they are too deep
                    if (depth > 3) {
                        logger.info("Blocking deep bounded range {} for {} at depth {}", version, ga, depth);
                        return false;
                    }
                }
            } catch (Exception e) {
                // Be lenient: if we can't parse it, let Maven try its default resolution
                return true;
            }
        }
        return true;
    }

    private boolean isRangeSyntax(String version) {
        return version.contains("[") || version.contains("(") || version.contains(",");
    }


    @Override
    public DependencySelector deriveChildSelector(
            DependencyCollectionContext context) {

        Set<String> nextSeen = new HashSet<>(seen);

        if (preventReentry && context != null && context.getDependency() != null) {
            Artifact a = context.getDependency().getArtifact();
            if (a != null) {
                nextSeen.add((a.getGroupId() == null ? "" : a.getGroupId()) + ":" + (a.getArtifactId() == null ? "" : a.getArtifactId()));
            }
        }

        return new NearestWinsNoRangeSelector(nextSeen, depth + 1, preventReentry, enableLogging);
    }
}
