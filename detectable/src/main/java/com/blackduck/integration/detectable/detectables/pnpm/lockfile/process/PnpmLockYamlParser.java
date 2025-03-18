package com.blackduck.integration.detectable.detectables.pnpm.lockfile.process;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmLockYaml;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmProjectPackage;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.ExcludedIncludedWildcardFilter;
import com.blackduck.integration.util.NameVersion;

public class PnpmLockYamlParser {
    private static final Predicate<String> isNodeRoot = "."::equals;

    private PnpmYamlTransformer pnpmTransformer;

    public PnpmLockYamlParser(PnpmYamlTransformer pnpmTransformer) {
        this.pnpmTransformer = pnpmTransformer;
    }

    public List<CodeLocation> parse(File parentFile, PnpmLockYaml pnpmLockYaml,
            PnpmLinkedPackageResolver linkedPackageResolver, @Nullable NameVersion projectNameVersion, List<String> excludedDirectories, List<String> includedDirectories)
            throws IntegrationException {
        List<CodeLocation> codeLocationsFromImports = createCodeLocationsFromImports(parentFile, pnpmLockYaml,
                linkedPackageResolver, projectNameVersion, excludedDirectories, includedDirectories);
        if (codeLocationsFromImports.isEmpty()) {
            return createCodeLocationsFromRoot(parentFile, pnpmLockYaml, projectNameVersion, linkedPackageResolver);
        }
        return codeLocationsFromImports;
    }

    private List<CodeLocation> createCodeLocationsFromRoot(File sourcePath, PnpmLockYaml pnpmLockYaml,
            @Nullable NameVersion projectNameVersion, PnpmLinkedPackageResolver linkedPackageResolver)
            throws IntegrationException {
        CodeLocation codeLocation = pnpmTransformer.generateCodeLocation(sourcePath, pnpmLockYaml, projectNameVersion,
                linkedPackageResolver);
        return Collections.singletonList(codeLocation);
    }

    private List<CodeLocation> createCodeLocationsFromImports(File sourcePath, PnpmLockYaml pnpmLockYaml,
            PnpmLinkedPackageResolver linkedPackageResolver, @Nullable NameVersion projectNameVersion, List<String> excludedDirectories, List<String> includedDirectories)
            throws IntegrationException {
        if (MapUtils.isEmpty(pnpmLockYaml.importers)) {
            return Collections.emptyList();
        }
        
        ExcludedIncludedWildcardFilter workspacesFilter;
        if (excludedDirectories.isEmpty() && includedDirectories.isEmpty()) {
            workspacesFilter = null; // Include all
        } else {
            workspacesFilter = ExcludedIncludedWildcardFilter.fromCollections(excludedDirectories, includedDirectories);
        }

        List<CodeLocation> codeLocations = new LinkedList<>();
        for (Map.Entry<String, PnpmProjectPackage> projectPackageInfo : pnpmLockYaml.importers.entrySet()) {
            String projectKey = projectPackageInfo.getKey();

            if ((workspacesFilter != null) && !workspacesFilter.shouldInclude(projectKey)) {
                // skip as the user specified filters and this projectKey is not something they want
                continue;
            }
            
            PnpmProjectPackage projectPackage = projectPackageInfo.getValue();
            NameVersion extractedNameVersion = extractProjectInfo(projectPackageInfo, linkedPackageResolver,
                    projectNameVersion);

            String reportingProjectPackagePath = null;
            if (!isNodeRoot.evaluate(projectKey)) {
                reportingProjectPackagePath = projectKey;
            }
            File generatedSourcePath = generateCodeLocationSourcePath(sourcePath, reportingProjectPackagePath);

            codeLocations.add(pnpmTransformer.generateCodeLocation(generatedSourcePath, projectPackage,
                    reportingProjectPackagePath, extractedNameVersion, pnpmLockYaml.packages, linkedPackageResolver, pnpmLockYaml.snapshots));
        }

        return codeLocations;
    }

    private NameVersion extractProjectInfo(Map.Entry<String, PnpmProjectPackage> projectPackageInfo,
            PnpmLinkedPackageResolver linkedPackageResolver, @Nullable NameVersion projectNameVersion) {
        if (isNodeRoot.evaluate(projectPackageInfo.getKey()) && projectNameVersion != null
                && projectNameVersion.getName() != null) {
            // resolve "." package to project root
            return projectNameVersion;
        }

        String projectPackageName = projectPackageInfo.getKey();
        String projectPackageVersion = linkedPackageResolver.resolveVersionOfLinkedPackage(null, projectPackageName);
        return new NameVersion(projectPackageName, projectPackageVersion);
    }

    private File generateCodeLocationSourcePath(File sourcePath, @Nullable String reportingProjectPackagePath) {
        if (StringUtils.isNotEmpty(reportingProjectPackagePath)) {
            return new File(sourcePath, reportingProjectPackagePath);
        }
        return sourcePath;
    }
}
