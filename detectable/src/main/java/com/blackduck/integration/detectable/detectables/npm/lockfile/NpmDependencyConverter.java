package com.blackduck.integration.detectable.detectables.npm.lockfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MultiValuedMap;
import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.NpmDependency;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.NpmProject;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.NpmRequires;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.PackageLock;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.PackageLockDependency;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.PackageLockPackage;
import com.blackduck.integration.detectable.detectables.npm.packagejson.CombinedPackageJson;

public class NpmDependencyConverter {
    private final ExternalIdFactory externalIdFactory;

    public NpmDependencyConverter(ExternalIdFactory externalIdFactory) {this.externalIdFactory = externalIdFactory;}

    public NpmProject convertLockFile(PackageLock packageLock, @Nullable CombinedPackageJson combinedPackageJson) {
        List<NpmRequires> declaredDevDependencies = new ArrayList<>();
        List<NpmRequires> declaredPeerDependencies = new ArrayList<>();
        List<NpmRequires> declaredOptionalDependencies = new ArrayList<>();
        List<NpmRequires> declaredDependencies = new ArrayList<>();
        List<NpmDependency> resolvedDependencies = new ArrayList<>();

        if (packageLock.packages != null) {
            List<NpmDependency> children = convertLockPackagesToNpmDependencies(null, packageLock.packages);
            resolvedDependencies.addAll(children);
        } else if (packageLock.dependencies != null) {
            List<NpmDependency> children = convertLockDependenciesToNpmDependencies(null, packageLock.dependencies);
            resolvedDependencies.addAll(children);
        }

        if (combinedPackageJson != null) {
            if (!combinedPackageJson.getDependencies().isEmpty()) {
                List<NpmRequires> rootRequires = convertNameVersionMapToRequires(combinedPackageJson.getDependencies());
                declaredDependencies.addAll(rootRequires);
            }

            if (!combinedPackageJson.getDevDependencies().isEmpty()) {
                List<NpmRequires> rootDevRequires = convertNameVersionMapToRequires(combinedPackageJson.getDevDependencies());
                declaredDevDependencies.addAll(rootDevRequires);
            }

            if (!combinedPackageJson.getPeerDependencies().isEmpty()) {
                List<NpmRequires> rootPeerRequires = convertNameVersionMapToRequires(combinedPackageJson.getPeerDependencies());
                declaredPeerDependencies.addAll(rootPeerRequires);
            }
            
            if (!combinedPackageJson.getOptionalDependencies().isEmpty()) {
                List<NpmRequires> rootOptionalRequires = convertNameVersionMapToRequires(combinedPackageJson.getOptionalDependencies());
                declaredOptionalDependencies.addAll(rootOptionalRequires);
            }
        }

        return new NpmProject(packageLock.name, packageLock.version, declaredDevDependencies, declaredPeerDependencies, declaredDependencies, declaredOptionalDependencies, resolvedDependencies);
    }

    public List<NpmDependency> convertLockPackagesToNpmDependencies(NpmDependency parent, Map<String, PackageLockPackage> packages) {
        List<NpmDependency> children = new ArrayList<>();

        if (packages == null || packages.size() == 0) {
            return children;
        }

        for (Map.Entry<String, PackageLockPackage> packageEntry : packages.entrySet()) {
            String packageName = packageEntry.getKey();
            PackageLockPackage packageLockDependency = packageEntry.getValue();

            NpmDependency dependency = createNpmDependency(packageName, packageLockDependency.version, packageLockDependency.dev, packageLockDependency.peer, packageLockDependency.optional);
            dependency.setParent(parent);
            children.add(dependency);

            List<NpmRequires> requires = convertNameVersionMapToRequires(packageLockDependency.dependencies);
            dependency.addAllRequires(requires);

            List<NpmDependency> grandChildren = convertLockPackagesToNpmDependencies(dependency, packageLockDependency.packages);
            dependency.addAllDependencies(grandChildren);
        }
        return children;
    }
    
    public List<NpmDependency> convertLockDependenciesToNpmDependencies(NpmDependency parent, Map<String, PackageLockDependency> packageLockDependencyMap) {
        List<NpmDependency> children = new ArrayList<>();

        if (packageLockDependencyMap == null || packageLockDependencyMap.size() == 0) {
            return children;
        }

        for (Map.Entry<String, PackageLockDependency> packageEntry : packageLockDependencyMap.entrySet()) {
            String packageName = packageEntry.getKey();
            PackageLockDependency packageLockDependency = packageEntry.getValue();

            NpmDependency dependency = createNpmDependency(packageName, packageLockDependency.version, packageLockDependency.dev, packageLockDependency.peer, packageLockDependency.optional);
            dependency.setParent(parent);
            children.add(dependency);

            List<NpmRequires> requires = convertNameVersionMapToRequires(packageLockDependency.requires);
            dependency.addAllRequires(requires);

            List<NpmDependency> grandChildren = convertLockDependenciesToNpmDependencies(dependency, packageLockDependency.dependencies);
            dependency.addAllDependencies(grandChildren);
        }
        return children;
    }

    private NpmDependency createNpmDependency(String name, String version, Boolean isDev, Boolean isPeer, Boolean isOptional) {
        boolean dev = isDev != null && isDev;
        boolean peer = isPeer != null && isPeer;
        boolean optional = isOptional != null && isOptional;
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, name, version);
        return new NpmDependency(name, version, externalId, dev, peer, optional);
    }

    public List<NpmRequires> convertNameVersionMapToRequires(MultiValuedMap<String, String> requires) {
        if (requires == null || requires.size() == 0) {
            return Collections.emptyList();
        }
        return requires.entries().stream()
            .map(entry -> new NpmRequires(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }
    
    public List<NpmRequires> convertNameVersionMapToRequires(Map<String, String> requires) {
        if (requires == null || requires.size() == 0) {
            return Collections.emptyList();
        }
        return requires.entrySet().stream()
            .map(entry -> new NpmRequires(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    public void linkPackagesDependencies(PackageLock packageLock) {        
        Set<String> packagesToRemove = new HashSet<>();
        
        if (packageLock.packages == null) {
            // The linkage phase is only necessary for v2/v3 lockfiles. v1 lockfiles
            // have redundant information in the dependencies object that removes the need for this step.
            return;
        }
                
        for(String packageName : packageLock.packages.keySet()) {
            if (packageName.isEmpty()) {
                packagesToRemove.add(packageName);
            }

            gatherAllDependencies(packageLock, packageName);
            
            // Look for any relationships previously nested in the dependencies object prior to
            // npm 9. In the packages object these are stored at the root of the object in a parent/child and
            // perhaps even /grandchild relationship. Look for the * in the packageName that we have inserted 
            // before loading data into packageLock. This character is not allowed in npm package names and
            // indicates we should link up this set of packages.
            if (packageName.contains("*")) { 
                // This packageName contains one or more *'s indicating a parent/child relationship.
                // The parent will be the portion of the package name up to and not including the final *.
                PackageLockPackage parentPackage = 
                        packageLock.packages.get(packageName.substring(0, packageName.lastIndexOf("*")));

                // `parentPackage` can be null if the package is marked as `extraneous`, so we'll skip it.
                // `Extraneous` packages are those present in the node_modules folder but not listed as dependencies in any package's dependency list.
                // In this case, the `parentPackage` may not be present in the packages object, so we can't link it to the child, 
                // resulting in `parentPackage == null`.
                // These packages are not part of the dependency tree and are unnecessary for graph construction.
                // It's recommended to run `npm prune` to remove them: https://docs.npmjs.com/cli/v7/commands/npm-prune
                if (parentPackage == null && packageLock.packages.get(packageName) != null && packageLock.packages.get(packageName).extraneous) {
                    packagesToRemove.add(packageName);
                    continue;
                }
                if (parentPackage != null) {
                    linkChildBackToParent(parentPackage, packageName, packagesToRemove, packageLock);
                }
            }
        }
            
        // Now that they are processed, get rid of any packages containing a parent/child relationship. 
        // This makes the final packages structure more like the previous dependencies structure so most of the
        // detector code does not need to be altered to support npm 9 and later.
        packageLock.packages.keySet().removeAll(packagesToRemove);
    }

    private void linkChildBackToParent(PackageLockPackage parentPackage, String packageName,Set<String> packagesToRemove, PackageLock packageLock) {
        if (parentPackage.packages == null) {
            parentPackage.packages = new HashMap<String, PackageLockPackage>();
        }

        // Link the child back to the parent. The child will be the leaf of the overall packageName.
        String childPackageName = packageName.substring(packageName.lastIndexOf("*") + 1, packageName.length());
        parentPackage.packages.put(childPackageName, packageLock.packages.get(packageName));

        packagesToRemove.add(packageName);
    }

    /**
     * We need to reconstruct the new packages/dependencies setup to look like the old dependencies/requires 
     * setup so the later graph construction works. This is just a dump of all dependencies + devDependencies 
     * + peerDependencies + the new optionalDependences. This happens regardless of filtering with 
     * detect.npm.dependency.types.excluded.
     * @param packageLock
     * @param packageName
     */
    private void gatherAllDependencies(PackageLock packageLock, String packageName) {
        PackageLockPackage packageLockPackage = packageLock.packages.get(packageName);
        if (packageLockPackage.dependencies != null) {
            if (packageLockPackage.devDependencies != null) {
                packageLockPackage.dependencies.putAll(packageLockPackage.devDependencies);
            }
            if (packageLockPackage.peerDependencies != null) {
                packageLockPackage.dependencies.putAll(packageLockPackage.peerDependencies);
            }
            if (packageLockPackage.optionalDependencies != null) {
                packageLockPackage.dependencies.putAll(packageLockPackage.optionalDependencies);
            } 
        }
    }
}
