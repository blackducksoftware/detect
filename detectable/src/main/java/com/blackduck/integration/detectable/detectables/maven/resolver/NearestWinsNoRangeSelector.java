package com.blackduck.integration.detectable.detectables.maven.resolver;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;
import org.eclipse.aether.util.version.GenericVersionScheme;

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
        if (dependency == null || dependency.getArtifact() == null) {
            return true;
        }

        Artifact artifact = dependency.getArtifact();

        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String ga = (groupId == null ? "" : groupId) + ":" + (artifactId == null ? "" : artifactId);

        if (enableLogging) {
            logger.debug("[NearestWinsNoRangeSelector] Depth: {}, Checking dependency: {}:{}", depth, ga, artifact.getVersion());
        }

        // Conditional GA re-entry prevention (configurable)
        if (preventReentry && seen.contains(ga)) {
            if (enableLogging) {
                logger.debug("Skipping re-entered GA: {}", ga);
            }
            return false;
        }

        // Always-on: Skip transitive version ranges (depth > 1)
        if (depth > 1) {
            String version = artifact.getVersion();
            if (version != null) {
                // cheap pre-check for common range characters
                if (version.indexOf('[') >= 0 || version.indexOf('(') >= 0 || version.indexOf(',') >= 0 || version.indexOf(']') >= 0 || version.indexOf(')') >= 0) {
                    try {
                        VersionConstraint constraint = VERSION_SCHEME.parseVersionConstraint(version);
                        if (constraint != null && constraint.getRange() != null) {
                            if (enableLogging) {
                                logger.info("Skipping transitive ranged version for {}: {}", ga, version);
                            }
                            return false;
                        }
                    } catch (Exception e) {
                        // If parsing fails, be conservative and allow the dependency
                        if (enableLogging) {
                            logger.info("Version parsing failed for {}:{} -> allowing. Error: {}", ga, version, e.getMessage());
                        }
                        return true;
                    }
                }
            }
        }

        return true;
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
