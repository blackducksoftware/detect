package com.blackduck.integration.detectable.detectables.rush;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.FilesNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectables.npm.lockfile.NpmShrinkwrapDetectable;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.PnpmLockDetectable;
import com.blackduck.integration.detectable.detectables.yarn.YarnLockDetectable;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.executable.ExecutableRunnerException;

import java.io.File;
import java.io.IOException;

@DetectableInfo(name = "Rush Lock", language = "Node JS", forge = "npmjs", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: " + RushDetectable.RUSH_JSON + " and one of the following: " + RushDetectable.SHRINKWRAP_JSON + ", "
        + RushDetectable.YARN_LOCK + "," + RushDetectable.PNPM_LOCK + ".")
public class RushDetectable extends Detectable {

    public static final String RUSH_JSON = "rush.json";
    public static final String SHRINKWRAP_JSON = NpmShrinkwrapDetectable.SHRINKWRAP_JSON;
    public static final String YARN_LOCK = YarnLockDetectable.YARN_LOCK_FILENAME;
    public static final String PNPM_LOCK = PnpmLockDetectable.PNPM_LOCK_YAML_FILENAME;
    public File rushJson;
    private final FileFinder fileFinder;
    private final RushExtractor rushExtractor;
    public RushDetectable(DetectableEnvironment environment, FileFinder fileFinder, RushExtractor rushExtractor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.rushExtractor = rushExtractor;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        rushJson = requirements.file(RUSH_JSON);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        Requirements requirements = new Requirements(fileFinder, environment);

        File lockFilePath = new File(environment.getDirectory(), "/common/config/rush");

        // Rush is used in conjunction with traditional NPM projects, Yarn projects or PNPM projects.
        File shrinkwrapJson = fileFinder.findFile(lockFilePath, SHRINKWRAP_JSON);
        File yarnLock = fileFinder.findFile(lockFilePath, YARN_LOCK);
        File pnpmLock = fileFinder.findFile(lockFilePath, PNPM_LOCK);
        if (pnpmLock == null && shrinkwrapJson == null && yarnLock == null) {
            File subspacesPath = new File(environment.getDirectory(), "/common/config/subspaces/default");
            pnpmLock = fileFinder.findFile(subspacesPath, PNPM_LOCK);

            if (pnpmLock == null) {
                return new FilesNotFoundDetectableResult(SHRINKWRAP_JSON, PNPM_LOCK, YARN_LOCK);
            }
        }
        requirements.explainNullableFile(pnpmLock);
        requirements.explainNullableFile(shrinkwrapJson);
        requirements.explainNullableFile(yarnLock);

        // TODO: Use SearchPattern (Maybe make an easier to construct one???)
        //        requirements.anyFile(
        //            "let me pass in a bunch of string patterns, and the requirements already has a directory, so default to that"
        //            new SearchPattern(environment.getDirectory(), PACKAGE_LOCK_JSON, lockFile -> packageLockFile = lockFile)
        //        );

        return requirements.result();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) throws ExecutableRunnerException, IOException {
        return rushExtractor.extract(environment.getDirectory(), rushJson, fileFinder);
    }

}
