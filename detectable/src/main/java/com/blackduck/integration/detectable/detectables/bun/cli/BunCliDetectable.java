package com.blackduck.integration.detectable.detectables.bun.cli;

import java.io.File;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.BunResolver;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.FilesNotFoundDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(
    name = "Bun CLI",
    language = "Node JS",
    forge = "npmjs",
    accuracy = DetectableAccuracyType.HIGH,
    requirementsMarkdown = "Files: package.json and one of package-lock.json, yarn.lock, or pnpm-lock.yaml. Executable: bun."
)
public class BunCliDetectable extends Detectable {
    public static final String PACKAGE_JSON_FILENAME = "package.json";
    // bun pm list --all can auto-migrate from npm, yarn, and pnpm lockfiles.
    // bun.lock is intentionally excluded: if it exists, BunLockfileDetectable handles it at higher priority.
    private static final String NPM_LOCK_FILENAME = "package-lock.json";
    private static final String YARN_LOCK_FILENAME = "yarn.lock";
    private static final String PNPM_LOCK_FILENAME = "pnpm-lock.yaml";

    private final FileFinder fileFinder;
    private final BunResolver bunResolver;
    private final BunCliExtractor bunCliExtractor;

    private File packageJson;
    private ExecutableTarget bunExe;

    public BunCliDetectable(DetectableEnvironment environment, FileFinder fileFinder, BunResolver bunResolver, BunCliExtractor bunCliExtractor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.bunResolver = bunResolver;
        this.bunCliExtractor = bunCliExtractor;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        packageJson = requirements.file(PACKAGE_JSON_FILENAME);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        boolean hasLockFile =
            fileFinder.findFile(environment.getDirectory(), NPM_LOCK_FILENAME) != null
            || fileFinder.findFile(environment.getDirectory(), YARN_LOCK_FILENAME) != null
            || fileFinder.findFile(environment.getDirectory(), PNPM_LOCK_FILENAME) != null;

        if (!hasLockFile) {
            return new FilesNotFoundDetectableResult(NPM_LOCK_FILENAME, YARN_LOCK_FILENAME, PNPM_LOCK_FILENAME);
        }

        Requirements requirements = new Requirements(fileFinder, environment);
        bunExe = requirements.executable(() -> bunResolver.resolveBun(), "bun");
        return requirements.result();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        return bunCliExtractor.extract(environment.getDirectory(), packageJson, bunExe);
    }
}
