package com.blackduck.integration.detectable.detectables.bun.lockb;

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
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(name = "Bun Lock Binary", language = "Node JS", forge = "npmjs", accuracy = DetectableAccuracyType.HIGH,
    requirementsMarkdown = "Files: package.json and (bun.lockb or bun.lock). Executable: bun.")
public class BunLockbDetectable extends Detectable {
    public static final String BUN_LOCKB_FILENAME = "bun.lockb";
    public static final String BUN_LOCK_FILENAME = "bun.lock";
    public static final String PACKAGE_JSON_FILENAME = "package.json";

    private final FileFinder fileFinder;
    private final BunResolver bunResolver;
    private final BunLockbExtractor bunLockbExtractor;

    private File packageJson;
    private ExecutableTarget bunExe;

    public BunLockbDetectable(DetectableEnvironment environment, FileFinder fileFinder, BunResolver bunResolver, BunLockbExtractor bunLockbExtractor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.bunResolver = bunResolver;
        this.bunLockbExtractor = bunLockbExtractor;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        packageJson = requirements.file(PACKAGE_JSON_FILENAME);
        requirements.eitherFile(BUN_LOCKB_FILENAME, BUN_LOCK_FILENAME);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        Requirements requirements = new Requirements(fileFinder, environment);
        bunExe = requirements.executable(() -> bunResolver.resolveBun(), "bun");
        // TODO: Uncomment this version guard once BunCliDetectable is implemented.
        //
        // DetectorRuleFactory will register entry points in order:
        //   1. BunCliDetectable  (bun >= 1.2) — "bun pm list --all"
        //   2. BunLockbDetectable (bun < 1.2) — "bun bun.lockb"       ← this class
        //   3. BunLockDetectable  (no bun exe) — parse bun.lock JSONC
        //
        // This guard ensures that when bun >= 1.2 is installed but BunCliDetectable fails
        // for some reason, we do NOT fall through here — we let BunLockDetectable handle it.
        // Requires injecting DetectableExecutableRunner to run "bun --version".
        //
        // if (requirements.result().getPassed()) {
        //     String version = getBunVersion(bunExe);  // run: bun --version, trim stdout
        //     if (version != null && !isBunVersionBelow12(version)) {
        //         return new ExecutableVersionMismatchDetectableResult("bun", "< 1.2", version);
        //     }
        // }
        return requirements.result();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        return bunLockbExtractor.extract(environment.getDirectory(), packageJson, bunExe);
    }
}
