package com.blackduck.integration.detectable.detectables.npm.lockfile.parse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.npm.NpmAliasParser;
import com.blackduck.integration.detectable.detectables.npm.lockfile.NpmDependencyConverter;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.NpmProject;
import com.blackduck.integration.detectable.detectables.npm.lockfile.model.PackageLock;
import com.blackduck.integration.detectable.detectables.npm.lockfile.result.NpmPackagerResult;
import com.blackduck.integration.detectable.detectables.npm.packagejson.CombinedPackageJson;
import com.blackduck.integration.detectable.detectables.npm.packagejson.CombinedPackageJsonExtractor;
import com.blackduck.integration.util.NameVersion;

public class NpmLockfilePackager {
    private final Gson gson;
    private final ExternalIdFactory externalIdFactory;
    private final NpmLockFileProjectIdTransformer projectIdTransformer;
    private final NpmLockfileGraphTransformer graphTransformer;

    public NpmLockfilePackager(Gson gson, ExternalIdFactory externalIdFactory, NpmLockFileProjectIdTransformer projectIdTransformer, NpmLockfileGraphTransformer graphTransformer) {
        this.gson = gson;
        this.externalIdFactory = externalIdFactory;
        this.projectIdTransformer = projectIdTransformer;
        this.graphTransformer = graphTransformer;
    }

    public NpmPackagerResult parseAndTransform(@Nullable String rootJsonPath, @Nullable String packageJsonText, String lockFileText) throws IOException {
        return parseAndTransform(rootJsonPath, packageJsonText, lockFileText, new ArrayList<>());
    }

    public NpmPackagerResult parseAndTransform(@Nullable String rootJsonPath, @Nullable String packageJsonText, String lockFileText, List<NameVersion> externalDependencies) throws IOException {
        CombinedPackageJsonExtractor extractor = new CombinedPackageJsonExtractor(gson);
        CombinedPackageJson combinedPackageJson = extractor.constructCombinedPackageJson(rootJsonPath, packageJsonText);

        lockFileText = removePathInfoFromPackageName(lockFileText);

        PackageLock packageLock = gson.fromJson(lockFileText, PackageLock.class);

        NpmDependencyConverter dependencyConverter = new NpmDependencyConverter(externalIdFactory);

        // Link up any subpackages that no longer have a nice nested relationship in the packages areas of the
        // lock file
        dependencyConverter.linkPackagesDependencies(packageLock);

        NpmProject project = dependencyConverter.convertLockFile(packageLock, combinedPackageJson);

        // Build alias mapping from package.json to resolve aliased package names
        Map<String, String> aliasMapping = combinedPackageJson != null
            ? NpmAliasParser.buildAliasMapping(
                combinedPackageJson.getDependencies(),
                combinedPackageJson.getDevDependencies(),
                combinedPackageJson.getPeerDependencies(),
                combinedPackageJson.getOptionalDependencies()
            )
            : new HashMap<>();

        DependencyGraph dependencyGraph = graphTransformer.transform(packageLock, project, externalDependencies,
                combinedPackageJson == null ? null : combinedPackageJson.getRelativeWorkspaces(), aliasMapping);
        ExternalId projectId = projectIdTransformer.transform(combinedPackageJson, packageLock);
        CodeLocation codeLocation = new CodeLocation(dependencyGraph, projectId);
        return new NpmPackagerResult(projectId.getName(), projectId.getVersion(), codeLocation);
    }

    public String removePathInfoFromPackageName(String lockFileText) {
        List<String> searchList = new ArrayList<>(Arrays.asList("/node_modules/", "node_modules/"));
        List<String> replaceList = new ArrayList<>(Arrays.asList("*", ""));

        // Flatten the lock file, removing node_modules from the package names. The code expects them in this
        // format as it aligns with the previous dependencies section of the lock file that was removed in npm9. 
        // For any package names that contain /node_modules/ not at the beginning of the path, insert a * to 
        // indicate a parent/child relationship. We'll link up later in the call to linkPackagesDependencies.
        lockFileText = StringUtils.replaceEach(lockFileText, 
                searchList.toArray(new String[searchList.size()]), 
                replaceList.toArray(new String[replaceList.size()]));
        return lockFileText;
    }
}
