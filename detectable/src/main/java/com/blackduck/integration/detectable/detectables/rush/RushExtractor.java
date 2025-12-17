package com.blackduck.integration.detectable.detectables.rush;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfilePackager;
import com.blackduck.integration.detectable.detectables.npm.lockfile.result.NpmPackagerResult;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLinkedPackageResolver;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLockYamlParserInitial;
import com.blackduck.integration.detectable.detectables.rush.model.RushJsonParseResult;
import com.blackduck.integration.detectable.detectables.rush.parse.RushJsonParser;
import com.blackduck.integration.detectable.detectables.yarn.YarnPackager;
import com.blackduck.integration.detectable.detectables.yarn.YarnResult;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.NullSafePackageJson;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLock;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockParser;
import com.blackduck.integration.detectable.detectables.yarn.workspace.YarnWorkspaces;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.ExcludedIncludedWildcardFilter;
import com.blackduck.integration.util.NameVersion;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RushExtractor {

    private final RushJsonParser rushJsonParser;

    private final NpmLockfilePackager npmLockfilePackager;

    private final PnpmLockYamlParserInitial pnpmLockYamlParserInitial;

    private final YarnPackager yarnPackager;

    private final RushOptions rushOptions;
    private final PackageJsonFiles packageJsonFiles;
    private final YarnLockParser yarnLockParser;

    public RushExtractor(RushJsonParser rushJsonParser, NpmLockfilePackager npmLockfilePackager, RushOptions rushOptions, PnpmLockYamlParserInitial pnpmLockYamlParserInitial, YarnPackager yarnPackager, PackageJsonFiles packageJsonFiles, YarnLockParser yarnLockParser) {
        this.rushJsonParser = rushJsonParser;
        this.npmLockfilePackager = npmLockfilePackager;
        this.rushOptions = rushOptions;
        this.pnpmLockYamlParserInitial = pnpmLockYamlParserInitial;
        this.yarnPackager = yarnPackager;
        this.packageJsonFiles = packageJsonFiles;
        this.yarnLockParser = yarnLockParser;
    }

    public Extraction extract(File projectDirectory, File rushJsonFle, FileFinder fileFinder) {
        RushJsonParseResult rushJsonParseResult = rushJsonParser.parseRushJsonFile(rushJsonFle);

        try {
            rushJsonParseResult.findAllLockFiles(projectDirectory, fileFinder);
        } catch (IOException e) {
            return new Extraction.Builder().exception(e).build();
        }

        RushProjectType rushProjectType = rushJsonParseResult.getProjectType();
        rushJsonParseResult.readAllPackageJsons(fileFinder, projectDirectory, packageJsonFiles);
        List<NameVersion> allPackages = rushJsonParseResult.findAllProjects();

        if (rushProjectType == RushProjectType.NPM) {
            try {
                NpmPackagerResult npmPackagerResult = npmLockfilePackager.parseAndTransform(null, null, rushJsonParseResult.getNpmLockFile(), allPackages);
                NameVersion nameVersion = new NameVersion(npmPackagerResult.getProjectName(), npmPackagerResult.getProjectVersion());
                return new Extraction.Builder()
                        .success(npmPackagerResult.getCodeLocation())
                        .nameVersionIfPresent(Optional.of(nameVersion))
                        .build();
            } catch (Exception e) {
                return  new Extraction.Builder()
                        .exception(e)
                        .build();
            }
        } else if (rushProjectType == RushProjectType.YARN) {
            YarnLock yarnLock = yarnLockParser.parseYarnLock(rushJsonParseResult.getYarnLockContents());
            YarnResult yarnResult = yarnPackager.generateCodeLocation(null, YarnWorkspaces.EMPTY, yarnLock, allPackages, ExcludedIncludedWildcardFilter.EMPTY);
            NameVersion nameVersion = new NameVersion(yarnResult.getProjectName(), yarnResult.getProjectVersionName());

            return new Extraction.Builder()
                    .success(yarnResult.getCodeLocations())
                    .nameVersionIfPresent(Optional.of(nameVersion))
                    .build();

        } else if (rushProjectType == RushProjectType.PNPM) {
            List<CodeLocation> codeLocations = new ArrayList<>();
            PnpmLinkedPackageResolver pnpmLinkedPackageResolver = new PnpmLinkedPackageResolver(projectDirectory, packageJsonFiles);
            for (File pnpmLockFile: rushJsonParseResult.getPnpmSubspacesLockFiles()) {
                try {
                    codeLocations.addAll(pnpmLockYamlParserInitial.parse(pnpmLockFile, null, pnpmLinkedPackageResolver));
                } catch (Exception e) {
                    return  new Extraction.Builder()
                            .exception(e)
                            .build();
                }
            }

            return new Extraction.Builder()
                    .success(codeLocations)
                    .build();
        }

        return Extraction.failure();
    }
}
