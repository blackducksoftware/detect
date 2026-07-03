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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmLockYamlv5;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmProjectPackagev5;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;

public class PnpmLockYamlParserv5 {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private static final Predicate<String> isNodeRoot = "."::equals;

    private PnpmYamlTransformerv5 pnpmTransformer;

    public PnpmLockYamlParserv5(PnpmYamlTransformerv5 pnpmTransformer) {
        this.pnpmTransformer = pnpmTransformer;
    }

    public List<CodeLocation> parse(File parentFile, PnpmLockYamlv5 pnpmLockYaml,
            PnpmLinkedPackageResolver linkedPackageResolver, @Nullable NameVersion projectNameVersion) throws IntegrationException {
        logger.warn("Please update your pnpm-lock.yaml file by running pnpm install using a newer version of pnpm. Support for the v5 file will be removed in the next major release.");
        List<CodeLocation> codeLocationsFromImports = createCodeLocationsFromImports(parentFile, pnpmLockYaml, linkedPackageResolver, projectNameVersion);
        if (codeLocationsFromImports.isEmpty()) {
            return createCodeLocationsFromRoot(parentFile, pnpmLockYaml, projectNameVersion, linkedPackageResolver);
        }
        return codeLocationsFromImports;
    }
    

    private List<CodeLocation> createCodeLocationsFromRoot(
        File sourcePath,
        PnpmLockYamlv5 pnpmLockYaml,
        @Nullable NameVersion projectNameVersion,
        PnpmLinkedPackageResolver linkedPackageResolver
    ) throws IntegrationException {
        CodeLocation codeLocation = pnpmTransformer.generateCodeLocation(sourcePath, pnpmLockYaml, projectNameVersion, linkedPackageResolver);
        return Collections.singletonList(codeLocation);
    }

    private List<CodeLocation> createCodeLocationsFromImports(
        File sourcePath,
        PnpmLockYamlv5 pnpmLockYaml,
        PnpmLinkedPackageResolver linkedPackageResolver,
        @Nullable NameVersion projectNameVersion
    ) throws IntegrationException {
        if (MapUtils.isEmpty(pnpmLockYaml.importers)) {
            return Collections.emptyList();
        }

        List<CodeLocation> codeLocations = new LinkedList<>();
        for (Map.Entry<String, PnpmProjectPackagev5> projectPackageInfo : pnpmLockYaml.importers.entrySet()) {
            String projectKey = projectPackageInfo.getKey();
            PnpmProjectPackagev5 projectPackage = projectPackageInfo.getValue();
            NameVersion extractedNameVersion = extractProjectInfo(projectPackageInfo, linkedPackageResolver, projectNameVersion);

            String reportingProjectPackagePath = null;
            if (!isNodeRoot.evaluate(projectKey)) {
                reportingProjectPackagePath = projectKey;
            }
            File generatedSourcePath = generateCodeLocationSourcePath(sourcePath, reportingProjectPackagePath);

            codeLocations.add(pnpmTransformer.generateCodeLocation(
                generatedSourcePath,
                projectPackage,
                reportingProjectPackagePath,
                extractedNameVersion,
                pnpmLockYaml.packages,
                linkedPackageResolver
            ));
        }

        return codeLocations;
    }

    private NameVersion extractProjectInfo(
        Map.Entry<String, PnpmProjectPackagev5> projectPackageInfo,
        PnpmLinkedPackageResolver linkedPackageResolver,
        @Nullable NameVersion projectNameVersion
    ) {
        if (isNodeRoot.evaluate(projectPackageInfo.getKey()) && projectNameVersion != null && projectNameVersion.getName() != null) {
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
