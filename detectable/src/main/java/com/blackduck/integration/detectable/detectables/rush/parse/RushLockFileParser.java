package com.blackduck.integration.detectable.detectables.rush.parse;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfilePackager;
import com.blackduck.integration.detectable.detectables.npm.lockfile.result.NpmPackagerResult;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLinkedPackageResolver;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLockYamlParserInitial;
import com.blackduck.integration.detectable.detectables.rush.RushOptions;
import com.blackduck.integration.detectable.detectables.rush.RushProjectType;
import com.blackduck.integration.detectable.detectables.rush.model.RushJsonParseResult;
import com.blackduck.integration.detectable.detectables.yarn.YarnPackager;
import com.blackduck.integration.detectable.detectables.yarn.YarnResult;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.NullSafePackageJson;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLock;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockParser;
import com.blackduck.integration.detectable.detectables.yarn.workspace.YarnWorkspaces;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.ExcludedIncludedWildcardFilter;
import com.blackduck.integration.util.NameVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RushLockFileParser {
    private final Logger logger = LoggerFactory.getLogger(RushLockFileParser.class);
    private final NpmLockfilePackager npmLockfilePackager;
    private final YarnLockParser yarnLockParser;
    private final PnpmLockYamlParserInitial pnpmLockYamlParserInitial;
    private final RushOptions rushOptions;
    private final YarnPackager yarnPackager;
    private final PackageJsonFiles packageJsonFiles;
    private NullSafePackageJson rootPackageJson = null;



    public RushLockFileParser(NpmLockfilePackager npmLockfilePackager, PnpmLockYamlParserInitial pnpmLockYamlParserInitial, YarnPackager yarnPackager, PackageJsonFiles packageJsonFiles, YarnLockParser yarnLockParser, RushOptions rushOptions) {
        this.npmLockfilePackager = npmLockfilePackager;
        this.pnpmLockYamlParserInitial = pnpmLockYamlParserInitial;
        this.yarnPackager = yarnPackager;
        this.packageJsonFiles = packageJsonFiles;
        this.yarnLockParser = yarnLockParser;
        this.rushOptions = rushOptions;
    }

    public List<CodeLocation> parse(FileFinder fileFinder, File projectDirectory, RushJsonParseResult rushJsonParseResult) throws IOException, IntegrationException {
        List<CodeLocation> codeLocations = new ArrayList<>();

        RushProjectType rushProjectType = rushJsonParseResult.getProjectType();
        rushJsonParseResult.readAllPackageJsons(fileFinder, projectDirectory, packageJsonFiles);
        List<NameVersion> allPackages = rushJsonParseResult.findAllProjects();

        parseRootJsonFile(projectDirectory, fileFinder);

        if (rushProjectType == RushProjectType.NPM) {
            NpmPackagerResult npmPackagerResult = npmLockfilePackager.parseAndTransform(null, null, rushJsonParseResult.getNpmLockFile(), allPackages);

            codeLocations.add(npmPackagerResult.getCodeLocation());
        } else if (rushProjectType == RushProjectType.YARN) {
            List<NullSafePackageJson> allPackageJsons = rushJsonParseResult.findAllProjectsPackages();
            YarnLock yarnLock = yarnLockParser.parseYarnLock(rushJsonParseResult.getYarnLockContents());

            for (NullSafePackageJson packageJson : allPackageJsons) {
                YarnResult yarnResult = yarnPackager.generateCodeLocation(packageJson, YarnWorkspaces.EMPTY, yarnLock, allPackages, ExcludedIncludedWildcardFilter.EMPTY);
                if(yarnResult.getCodeLocations() != null) {
                    codeLocations.addAll(yarnResult.getCodeLocations());
                }
            }

//            if (rootPackageJson != null) {
//                YarnResult yarnResult = yarnPackager.generateCodeLocation(rootPackageJson, YarnWorkspaces.EMPTY, yarnLock, allPackages, ExcludedIncludedWildcardFilter.EMPTY);
//                if(yarnResult.getCodeLocations() != null) {
//                    codeLocations.addAll(yarnResult.getCodeLocations());
//                }
//            }
        } else if (rushProjectType == RushProjectType.PNPM) {
            PnpmLinkedPackageResolver pnpmLinkedPackageResolver = new PnpmLinkedPackageResolver(projectDirectory, packageJsonFiles);

            ExcludedIncludedWildcardFilter subspacesFilter = createSubspaceFilter();

            for (File pnpmLockFile: rushJsonParseResult.getPnpmSubspacesLockFiles()) {
                File subspaceDirectory = pnpmLockFile.getParentFile();
                String subspaceName = subspaceDirectory.getName();

                if (subspacesFilter != null && !subspaceName.isEmpty() && !subspacesFilter.shouldInclude(subspaceName)) {
                    continue;
                }

                codeLocations.addAll(pnpmLockYamlParserInitial.parse(pnpmLockFile, null, pnpmLinkedPackageResolver));
            }
        }

        return codeLocations;
    }

    public Optional<NameVersion> parseNameVersion() {
        if (rootPackageJson != null && rootPackageJson.getName().isPresent() && rootPackageJson.getVersion().isPresent()) {
            return Optional.of(new NameVersion(rootPackageJson.getNameString(), rootPackageJson.getVersionString()));
        }
        return Optional.empty();
    }

    private void parseRootJsonFile(File sourceDirectory, FileFinder fileFinder) {
        try {
            File packageJsonFile = fileFinder.findFile(sourceDirectory, "package.json");
            if (packageJsonFile != null) {
                rootPackageJson = packageJsonFiles.read(packageJsonFile);
            }
        } catch (IOException e) {
            logger.warn("Failed to find root package json file. Please make sure you have a root package.json before scanning the project", e);
        }
    }

    private ExcludedIncludedWildcardFilter createSubspaceFilter() {
        List<String> includedSubspaces = rushOptions.getIncludedSubspaces();
        List<String> excludedSubspaces = rushOptions.getExcludedSubspaces();
        ExcludedIncludedWildcardFilter subspacesFilter;

        if(!excludedSubspaces.isEmpty() && !includedSubspaces.isEmpty()) {
            subspacesFilter = null;
        } else {
            subspacesFilter = ExcludedIncludedWildcardFilter.fromCollections(excludedSubspaces, includedSubspaces);
        }

        return subspacesFilter;
    }

}
