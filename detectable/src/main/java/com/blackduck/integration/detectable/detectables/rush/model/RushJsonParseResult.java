package com.blackduck.integration.detectable.detectables.rush.model;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.detectables.rush.RushDetectable;
import com.blackduck.integration.detectable.detectables.rush.RushProjectType;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.NullSafePackageJson;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.blackduck.integration.util.NameVersion;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;


public class RushJsonParseResult {

    private final RushProjectType projectType;

    private final List<RushProject> rushProjects;
    private List<File> pnpmSubspacesLockFiles;
    private List<String> yarnLockContents;
    private String npmLockContents;
    private static final String PACKAGE_JSON = "package.json";

    public RushJsonParseResult(RushProjectType projectType, List<RushProject> rushProjects) {
        this.projectType = projectType;
        this.rushProjects = rushProjects;
    }

    public RushProjectType getProjectType() {
        return projectType;
    }

    public List<File> getPnpmSubspacesLockFiles() {
        return pnpmSubspacesLockFiles;
    }

    public List<String> getYarnLockContents() {
        return yarnLockContents;
    }

    public String getNpmLockFile() {
        return npmLockContents;
    }

    public void findAllLockFiles(File sourceDirectory, FileFinder fileFinder) throws IOException {
        File lockFilePath = new File(sourceDirectory.getAbsolutePath() + "/common/config/rush");
        File npmLockFile = fileFinder.findFile(lockFilePath, RushDetectable.SHRINKWRAP_JSON);
        if (npmLockFile != null) {
            npmLockContents = FileUtils.readFileToString(npmLockFile, StandardCharsets.UTF_8);
            return;
        }

        File yarnLockFile = fileFinder.findFile(lockFilePath, RushDetectable.YARN_LOCK);
        if (yarnLockFile != null) {
            yarnLockContents = FileUtils.readLines(yarnLockFile, StandardCharsets.UTF_8);
            return;
        }

        File subspacesDir = new File(sourceDirectory.getAbsolutePath() + "/common/config/subspaces");
        if (subspacesDir.exists()) {
            pnpmSubspacesLockFiles = fileFinder.findFiles(subspacesDir, RushDetectable.PNPM_LOCK, false, 2);
        } else {
            pnpmSubspacesLockFiles = fileFinder.findFiles(lockFilePath, RushDetectable.PNPM_LOCK);
        }
    }

    public void readAllPackageJsons(FileFinder fileFinder, File sourceDirectory, PackageJsonFiles packageJsonFiles) {
        rushProjects.forEach(rushProject -> {
           File projectPath = new File(sourceDirectory + "/" + rushProject.getProjectFolder());
           File packageJson = fileFinder.findFile(projectPath, PACKAGE_JSON);
           if (packageJson != null) {
               NullSafePackageJson packageJsonFile = null;
               try {
                   packageJsonFile = packageJsonFiles.read(packageJson);
               } catch (IOException e) {
                   throw new RuntimeException(e);
               }
               rushProject.setPackageJson(packageJsonFile);
           }
        });
    }

    public List<NameVersion> getAllProjects() {
        return rushProjects.stream().map(rushProject -> new NameVersion(rushProject.getPackageName(), rushProject.getPackageJson().getVersionString()))
                .collect(Collectors.toList());
    }

    public List<NullSafePackageJson> getAllPackageJsons() {
        return rushProjects.stream().map(RushProject::getPackageJson).collect(Collectors.toList());
    }

}
