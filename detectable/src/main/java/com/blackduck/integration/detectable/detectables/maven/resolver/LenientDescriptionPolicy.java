package com.blackduck.integration.detectable.detectables.maven.resolver;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.resolution.ArtifactDescriptorPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorPolicyRequest;

public class LenientDescriptionPolicy implements ArtifactDescriptorPolicy {

    @Override
    public int getPolicy(RepositorySystemSession session, ArtifactDescriptorPolicyRequest request) {
        return ArtifactDescriptorPolicy.IGNORE_ERRORS; // Lenient policy
    }
}
