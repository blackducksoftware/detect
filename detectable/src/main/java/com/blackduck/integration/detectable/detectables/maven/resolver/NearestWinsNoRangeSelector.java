package com.blackduck.integration.detectable.detectables.maven.resolver;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;
import org.eclipse.aether.util.version.GenericVersionScheme;

import java.util.HashSet;
import java.util.Set;

public class NearestWinsNoRangeSelector implements DependencySelector {

    private static final VersionScheme VERSION_SCHEME =
            new GenericVersionScheme();

    private final Set<String> seen;
    private final int depth;

    public NearestWinsNoRangeSelector() {
        this(new HashSet<>(), 0);
    }

    private NearestWinsNoRangeSelector(Set<String> seen, int depth) {
        this.seen = seen;
        this.depth = depth;
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        if (dependency == null || dependency.getArtifact() == null) {
            return true;
        }

        Artifact artifact = dependency.getArtifact();

        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String ga = groupId + ":" + artifactId;

        System.out.println("[NearestWinsNoRangeSelector] Depth: " + depth + ", Checking dependency: " + ga + ":" + artifact.getVersion());

        if (seen.contains(ga)) {
            return false;
        }

        if (depth > 1) {
            String version = artifact.getVersion();
            if (version != null) {
                try {
                    VersionConstraint constraint =
                            VERSION_SCHEME.parseVersionConstraint(version);

                    if (constraint.getRange() != null) {
                        return false;
                    }
                } catch (Exception e) {
                    // If version parsing fails, be conservative and allow
                    return true;
                }
            }
        }

        return true;
    }

    @Override
    public DependencySelector deriveChildSelector(
            DependencyCollectionContext context) {

        Set<String> nextSeen = new HashSet<>(seen);

        if (context.getDependency() != null) {
            Artifact a = context.getDependency().getArtifact();
            if (a != null) {
                nextSeen.add(a.getGroupId() + ":" + a.getArtifactId());
            }
        }

        return new NearestWinsNoRangeSelector(nextSeen, depth + 1);
    }
}
