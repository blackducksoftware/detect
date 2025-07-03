package com.blackduck.integration.detectable.detectables.npm.lockfile.model;

import java.util.List;

public class NpmProject { //TODO: I hate that this is a 'model' and it is mutable. - jp
    private final String name;
    private final String version;

    private final List<NpmRequires> declaredDevDependencies;
    private final List<NpmRequires> declaredPeerDependencies;
    private final List<NpmRequires> declaredOptionalDependencies;
    private final List<NpmRequires> declaredDependencies;
    private final List<NpmDependency> resolvedDependencies;

    public NpmProject(
        String name,
        String version,
        List<NpmRequires> declaredDevDependencies,
        List<NpmRequires> declaredPeerDependencies,
        List<NpmRequires> declaredDependencies,
        List<NpmRequires> declaredOptionalDependencies, 
        List<NpmDependency> resolvedDependencies
    ) {
        this.name = name;
        this.version = version;
        this.declaredDevDependencies = declaredDevDependencies;
        this.declaredPeerDependencies = declaredPeerDependencies;
        this.declaredOptionalDependencies = declaredOptionalDependencies;
        this.declaredDependencies = declaredDependencies;
        this.resolvedDependencies = resolvedDependencies;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public List<NpmRequires> getDeclaredDependencies() {
        return declaredDependencies;
    }

    public List<NpmRequires> getDeclaredDevDependencies() {
        return declaredDevDependencies;
    }

    public List<NpmRequires> getDeclaredPeerDependencies() {
        return declaredPeerDependencies;
    }
    
    public List<NpmRequires> getDeclaredOptionalDependencies() {
        return declaredOptionalDependencies;
    }

    public List<NpmDependency> getResolvedDependencies() {
        return resolvedDependencies;
    }
}
