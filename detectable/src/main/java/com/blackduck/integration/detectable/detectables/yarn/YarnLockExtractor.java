package com.blackduck.integration.detectable.detectables.yarn;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectables.yarn.packagejson.NullSafePackageJson;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLock;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockParser;
import com.blackduck.integration.detectable.detectables.yarn.workspace.YarnWorkspace;
import com.blackduck.integration.detectable.detectables.yarn.workspace.YarnWorkspaces;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.util.ExcludedIncludedWildcardFilter;

public class YarnLockExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final YarnLockParser yarnLockParser;
    private final YarnPackager yarnPackager;
    private final PackageJsonFiles packageJsonFiles;
    private final YarnLockOptions yarnLockOptions;

    public YarnLockExtractor(YarnLockParser yarnLockParser, YarnPackager yarnPackager, PackageJsonFiles packageJsonFiles, YarnLockOptions yarnLockOptions) {
        this.yarnLockParser = yarnLockParser;
        this.yarnPackager = yarnPackager;
        this.packageJsonFiles = packageJsonFiles;
        this.yarnLockOptions = yarnLockOptions;
    }

    public Extraction extract(File projectDir, File yarnLockFile, File rootPackageJsonFile) {
        try {
            NullSafePackageJson rootPackageJson = packageJsonFiles.read(rootPackageJsonFile);
            String projectName = rootPackageJson.getName().orElse("null");
            logger.debug("Extracting Yarn project {} in {}", projectName, projectDir.getAbsolutePath());
            YarnLock yarnLock = readYarnLock(yarnLockFile);

            YarnResult yarnResult;
            if (yarnLockOptions.getYarnIgnoreAllWorkspacesMode()) {
                yarnResult = yarnPackager.generateCodeLocation(rootPackageJson, yarnLock, new ArrayList<>());
            } else {
                YarnWorkspaces workspaceData = collectWorkspaceData(projectDir);
                ExcludedIncludedWildcardFilter workspacesFilter = deriveExcludedIncludedWildcardFilter();
                yarnResult = yarnPackager.generateCodeLocation(rootPackageJson, workspaceData, yarnLock, new ArrayList<>(), workspacesFilter);
            }

            Optional<Exception> yarnException = yarnResult.getException();
            if (yarnException.isPresent()) {
                throw yarnException.get();
            }

            return new Extraction.Builder()
                .projectName(yarnResult.getProjectName())
                .projectVersion(yarnResult.getProjectVersionName())
                .success(yarnResult.getCodeLocations())
                .build();
        } catch (Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }

    private YarnLock readYarnLock(File yarnLockFile) throws IOException {
        List<String> yarnLockLines = FileUtils.readLines(yarnLockFile, StandardCharsets.UTF_8);
        return yarnLockParser.parseYarnLock(yarnLockLines);
    }

    @Nullable
    private ExcludedIncludedWildcardFilter deriveExcludedIncludedWildcardFilter() {
        ExcludedIncludedWildcardFilter workspacesFilter;
        if (yarnLockOptions.getExcludedWorkspaceNamePatterns().isEmpty() && yarnLockOptions.getIncludedWorkspaceNamePatterns().isEmpty()) {
            workspacesFilter = null; // Include all
        } else {
            workspacesFilter = ExcludedIncludedWildcardFilter.fromCollections(
                yarnLockOptions.getExcludedWorkspaceNamePatterns(),
                yarnLockOptions.getIncludedWorkspaceNamePatterns()
            );
        }
        return workspacesFilter;
    }

    @NotNull
    private YarnWorkspaces collectWorkspaceData(File dir) throws IOException {
        Collection<YarnWorkspace> curLevelWorkspaces = packageJsonFiles.readWorkspacePackageJsonFiles(dir);
        Collection<YarnWorkspace> allWorkspaces = new LinkedList<>(curLevelWorkspaces);
        for (YarnWorkspace workspace : curLevelWorkspaces) {
            Collection<YarnWorkspace> treeBranchWorkspacePackageJsons = packageJsonFiles.readWorkspacePackageJsonFiles(workspace.getWorkspacePackageJson().getDir());
            allWorkspaces.addAll(treeBranchWorkspacePackageJsons);
        }
        return new YarnWorkspaces(allWorkspaces);
    }
}
