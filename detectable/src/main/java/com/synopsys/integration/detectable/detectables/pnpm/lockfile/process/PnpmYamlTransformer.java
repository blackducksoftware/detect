package com.synopsys.integration.detectable.detectables.pnpm.lockfile.process;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.graph.BasicDependencyGraph;
import com.synopsys.integration.bdio.graph.DependencyGraph;
import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.dependency.Dependency;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.detectable.detectable.codelocation.CodeLocation;
import com.synopsys.integration.detectable.detectable.exception.DetectableException;
import com.synopsys.integration.detectable.detectable.util.EnumListFilter;
import com.synopsys.integration.detectable.detectables.pnpm.lockfile.model.PnpmDependencyInfo;
import com.synopsys.integration.detectable.detectables.pnpm.lockfile.model.PnpmDependencyType;
import com.synopsys.integration.detectable.detectables.pnpm.lockfile.model.PnpmLockYaml;
import com.synopsys.integration.detectable.detectables.pnpm.lockfile.model.PnpmPackageInfo;
import com.synopsys.integration.detectable.detectables.pnpm.lockfile.model.PnpmProjectPackage;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.util.NameVersion;

public class PnpmYamlTransformer {
    private static final String LINKED_PACKAGE_PREFIX = "link:";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final EnumListFilter<PnpmDependencyType> dependencyTypeFilter;
    
    private final Double lockfileVersion;

    public PnpmYamlTransformer(EnumListFilter<PnpmDependencyType> dependencyTypeFilter, String lockfileVersion) {
        this.dependencyTypeFilter = dependencyTypeFilter;
        this.lockfileVersion = Double.valueOf(lockfileVersion);
    }

    public CodeLocation generateCodeLocation(File sourcePath, PnpmLockYaml pnpmLockYaml, @Nullable NameVersion projectNameVersion, PnpmLinkedPackageResolver linkedPackageResolver)
        throws IntegrationException {
        return generateCodeLocation(sourcePath, convertPnpmLockYamlToPnpmProjectPackage(pnpmLockYaml), null, projectNameVersion, pnpmLockYaml.packages, linkedPackageResolver, pnpmLockYaml.snapshots);
    }

    public CodeLocation generateCodeLocation(
        File sourcePath,
        PnpmProjectPackage projectPackage,
        @Nullable String reportingProjectPackagePath,
        @Nullable NameVersion projectNameVersion,
        @Nullable Map<String, PnpmPackageInfo> packageMap,
        PnpmLinkedPackageResolver linkedPackageResolver, 
        @Nullable Map<String, PnpmPackageInfo> snapshots
    ) throws IntegrationException {
        DependencyGraph dependencyGraph = new BasicDependencyGraph();
        List<String> rootPackageIds = extractRootPackageIds(projectPackage, reportingProjectPackagePath, linkedPackageResolver);        
        buildGraph(dependencyGraph, rootPackageIds, packageMap, linkedPackageResolver, reportingProjectPackagePath, snapshots);

        if (projectNameVersion != null) {
            return new CodeLocation(
                dependencyGraph,
                ExternalId.FACTORY.createNameVersionExternalId(Forge.NPMJS, projectNameVersion.getName(), projectNameVersion.getVersion()),
                sourcePath
            );
        }
        return new CodeLocation(dependencyGraph, sourcePath);
    }

    private void buildGraph(
        DependencyGraph graphBuilder,
        List<String> rootPackageIds,
        @Nullable Map<String, PnpmPackageInfo> packageMap,
        PnpmLinkedPackageResolver linkedPackageResolver,
        @Nullable String reportingProjectPackagePath, 
        @Nullable Map<String, PnpmPackageInfo> snapshots
    ) throws IntegrationException {
        if (packageMap == null) {
            throw new DetectableException("Could not parse 'packages' section of the pnpm-lock.yaml file.");
        }
        for (Map.Entry<String, PnpmPackageInfo> packageEntry : packageMap.entrySet()) {
            String packageId = packageEntry.getKey();
            Optional<Dependency> pnpmPackage = buildDependencyFromPackageEntry(packageEntry);
            if (!pnpmPackage.isPresent()) {
                logger.debug(String.format("Could not add package %s to the graph.", packageId));
                continue;
            }
            
            // Process direct dependencies
            if (isRootPackage(packageId, rootPackageIds)) {
                graphBuilder.addChildToRoot(pnpmPackage.get());
            }

            PnpmPackageInfo packageInfo = getDependencyInformation(packageEntry, snapshots);  
            
            // Give up if we can't find any dependencies for this package and move onto processing others.
            if (packageInfo == null) {
                continue;
            }
            
            processTransitiveDependencies(graphBuilder, linkedPackageResolver, reportingProjectPackagePath, pnpmPackage,
                    packageInfo);
        }
    }

    private PnpmPackageInfo getDependencyInformation(Entry<String, PnpmPackageInfo> packageEntry, Map<String, PnpmPackageInfo> snapshots) {
        PnpmPackageInfo packageWithDependencyInfo = packageEntry.getValue();
        
        // In the v9 lockfile dependencies are in the snapshots object, look there.
        if (packageWithDependencyInfo.getDependencies().isEmpty() && packageWithDependencyInfo.getDevDependencies().isEmpty() && packageWithDependencyInfo.getOptionalDependencies().isEmpty()) {
            return getFlexibleValue(packageEntry.getKey(), snapshots);
        }
        
        return packageWithDependencyInfo;
    }

    private PnpmPackageInfo getFlexibleValue(String desiredKey, Map<String, PnpmPackageInfo> snapshots) {
        if (snapshots != null) {
            for (String key: snapshots.keySet()) {
                if (key.startsWith(desiredKey)) {
                    return snapshots.get(key);
                }
            }
        }

        return null;
    }

    private void processTransitiveDependencies(DependencyGraph graphBuilder,
            PnpmLinkedPackageResolver linkedPackageResolver, String reportingProjectPackagePath,
            Optional<Dependency> pnpmPackage, PnpmPackageInfo packageInfo) {
        if (!packageInfo.getDependencyType().isPresent() || dependencyTypeFilter.shouldInclude(packageInfo.getDependencyType().get())) {
            for (Map.Entry<String, String> packageDependency : packageInfo.getDependencies().entrySet()) {
                addTransitiveDependencyToGraph(graphBuilder, linkedPackageResolver, reportingProjectPackagePath,
                        pnpmPackage, packageDependency);
            }
            
            if (dependencyTypeFilter.shouldInclude(PnpmDependencyType.DEV)) {
                for (Map.Entry<String, String> packageDependency : packageInfo.getDevDependencies().entrySet()) {
                    addTransitiveDependencyToGraph(graphBuilder, linkedPackageResolver, reportingProjectPackagePath,
                            pnpmPackage, packageDependency);
                }
            }
            
            if (dependencyTypeFilter.shouldInclude(PnpmDependencyType.OPTIONAL)) {
                for (Map.Entry<String, String> packageDependency : packageInfo.getOptionalDependencies().entrySet()) {
                    addTransitiveDependencyToGraph(graphBuilder, linkedPackageResolver, reportingProjectPackagePath,
                            pnpmPackage, packageDependency);
                }
            }
        }
    }

    private void addTransitiveDependencyToGraph(DependencyGraph graphBuilder,
            PnpmLinkedPackageResolver linkedPackageResolver, String reportingProjectPackagePath,
            Optional<Dependency> pnpmPackage, Map.Entry<String, String> packageDependency) {
        String dependencyPackageId = convertRawEntryToPackageId(packageDependency.getKey(), packageDependency.getValue(), linkedPackageResolver, reportingProjectPackagePath);
        Optional<Dependency> child = buildDependencyFromPackageId(dependencyPackageId);
        child.ifPresent(c -> graphBuilder.addChildWithParent(child.get(), pnpmPackage.get()));
    }

    private PnpmProjectPackage convertPnpmLockYamlToPnpmProjectPackage(PnpmLockYaml pnpmLockYaml) {
        PnpmProjectPackage pnpmProjectPackage = new PnpmProjectPackage();

        pnpmProjectPackage.dependencies = pnpmLockYaml.dependencies;
        pnpmProjectPackage.devDependencies = pnpmLockYaml.devDependencies;
        pnpmProjectPackage.optionalDependencies = pnpmLockYaml.optionalDependencies;

        return pnpmProjectPackage;
    }

    private List<String> extractRootPackageIds(
        PnpmProjectPackage pnpmProjectPackage,
        @Nullable String reportingProjectPackagePath,
        PnpmLinkedPackageResolver linkedPackageResolver
    ) {
        Map<String, PnpmDependencyInfo> rawPackageInfo = new HashMap<>();
        if (pnpmProjectPackage.dependencies != null) {
            rawPackageInfo.putAll(pnpmProjectPackage.dependencies);
        }
        dependencyTypeFilter.ifShouldInclude(PnpmDependencyType.DEV, pnpmProjectPackage.devDependencies, rawPackageInfo::putAll);
        dependencyTypeFilter.ifShouldInclude(PnpmDependencyType.OPTIONAL, pnpmProjectPackage.optionalDependencies, rawPackageInfo::putAll);

        return rawPackageInfo.entrySet().stream()
            .map(entry -> convertRawEntryToPackageId(entry.getKey(), entry.getValue().version, linkedPackageResolver, reportingProjectPackagePath))
            .collect(Collectors.toList());
    }

    private String convertRawEntryToPackageId(String name, String version, PnpmLinkedPackageResolver linkedPackageResolver, @Nullable String reportingProjectPackagePath) {
        name = StringUtils.strip(name, "'");
        if (version.startsWith(LINKED_PACKAGE_PREFIX)) {
            // a linked project package's version will be referenced in the format: <linkPrefix><pathToLinkedPackageRelativeToReportingProjectPackage>
            version = linkedPackageResolver.resolveVersionOfLinkedPackage(reportingProjectPackagePath, version.replace(LINKED_PACKAGE_PREFIX, ""));
        }
        
        // Remove extra information from the version string, there will often be peer
        // dependency information that we do not support and is not related to the package version.
        if (version != null && version.contains("(")) {
            version = version.split("\\(")[0];
        }
        
        // v6 needs a leading / to find packages, v9 does not.
        String packageFormat = "%s@%s";
        
        if (lockfileVersion.intValue() == 6) {
            packageFormat = "/%s@%s";
        }

        return String.format(packageFormat, name, version);
    }

    private Optional<NameVersion> parseNameVersionFromId(String id) {
        // ids follow format: /name@version in v6, name@version in v9
        try {
            // It seems critical not to send this extra information in () or the kb will fail matching it.
            if (id.contains("(")) {
                id = id.split("\\(")[0];
            }

            int indexOfLastSlash = id.lastIndexOf("@");
            // v9 lockfile does not have names starting with /, v 6 does
            String name = "";
            if (id.startsWith("/")) {
                name = id.substring(1, indexOfLastSlash);
            } else {
                name = id.substring(0, indexOfLastSlash);
            }

            String version = id.substring(indexOfLastSlash + 1);
            return Optional.of(new NameVersion(name, version));
        } catch (Exception e) {
            logger.debug(String.format("There was an issue parsing package id: %s.  This is likely an unsupported format.", id));
            return Optional.empty();
        }
    }

    private Optional<Dependency> buildDependencyFromPackageEntry(Map.Entry<String, PnpmPackageInfo> packageEntry) {
        PnpmPackageInfo packageInfo = packageEntry.getValue();
        if (packageInfo.name != null) {
            return Optional.of(Dependency.FACTORY.createNameVersionDependency(Forge.NPMJS, packageInfo.name, packageInfo.version));
        }
        return buildDependencyFromPackageId(packageEntry.getKey());
    }

    private Optional<Dependency> buildDependencyFromPackageId(String packageId) {
        return parseNameVersionFromId(packageId)
            .map(nameVersion -> Dependency.FACTORY.createNameVersionDependency(Forge.NPMJS, nameVersion.getName(), nameVersion.getVersion()));
    }

    private boolean isRootPackage(String id, List<String> rootIds) { 
        return rootIds.contains(id) ||
            rootIds.stream()
                .map(this::parseNameVersionFromId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(NameVersion::getVersion)
                .anyMatch(id::equals); // for file dependencies, they are declared as <name> : <fileIdAsReportedInPackagesSection>
    }
 
}