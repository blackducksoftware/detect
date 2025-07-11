package com.blackduck.integration.detectable.detectables.cargo.transform;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.graph.builder.LazyExternalIdDependencyGraphBuilder;
import com.blackduck.integration.bdio.graph.builder.LazyId;
import com.blackduck.integration.bdio.graph.builder.MissingExternalIdException;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.dependency.DependencyFactory;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.cargo.model.CargoLockPackage;
import com.blackduck.integration.detectable.util.NameOptionalVersion;
import com.blackduck.integration.util.NameVersion;

public class CargoLockPackageTransformer {
    private final ExternalIdFactory externalIdFactory = new ExternalIdFactory();
    private final DependencyFactory dependencyFactory = new DependencyFactory(externalIdFactory);

    public DependencyGraph transformToGraph(
            List<CargoLockPackage> lockPackages,
            Set<NameVersion> rootDependencies) throws MissingExternalIdException, DetectableException {
        verifyNoDuplicatePackages(lockPackages);

        LazyExternalIdDependencyGraphBuilder graph = new LazyExternalIdDependencyGraphBuilder();
        for (CargoLockPackage lockPackage : lockPackages) {
            String name = lockPackage.getPackageNameVersion().getName();
            String version = lockPackage.getPackageNameVersion().getVersion();
            LazyId id = LazyId.fromNameAndVersion(name, version);
            Dependency dependency = dependencyFactory.createNameVersionDependency(Forge.CRATES, name, version);

            // Root dependencies empty means that Cargo.toml was not used, so we add all packages as root.
            // If rootDependencies is not empty, we only add the package if it is in the set.
            if (rootDependencies.isEmpty() || rootDependencies.contains(new NameVersion(name, version))) {
                graph.addChildToRoot(id);
            }

            graph.setDependencyInfo(id, dependency.getName(), dependency.getVersion(), dependency.getExternalId());
            graph.setDependencyAsAlias(id, LazyId.fromName(name));

            for (NameOptionalVersion child : lockPackage.getDependencies()) {
                Optional<String> optionalVersion = child.getVersion();
                LazyId childId = optionalVersion.map(s -> LazyId.fromNameAndVersion(child.getName(), s))
                        .orElseGet(() -> LazyId.fromName(child.getName()));
                graph.addChildWithParent(childId, id);
            }
        }
        return graph.build();
    }

    private void verifyNoDuplicatePackages(List<CargoLockPackage> lockPackages) throws DetectableException {
        for (CargoLockPackage cargoLockPackage : lockPackages) {
            for (NameOptionalVersion dependency : cargoLockPackage.getDependencies()) {
                if (!dependency.getVersion().isPresent()) {
                    long matchingPackages = lockPackages.stream()
                        .filter(filteringPackage -> dependency.getName().equals(filteringPackage.getPackageNameVersion().getName()))
                        .count();
                    if (matchingPackages > 1) {
                        throw new DetectableException("Multiple packages with the same name cannot be reconciled to a single version.");
                    }
                }
            }
        }
    }
}
