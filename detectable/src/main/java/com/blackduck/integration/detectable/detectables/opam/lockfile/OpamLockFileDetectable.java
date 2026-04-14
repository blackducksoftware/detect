package com.blackduck.integration.detectable.detectables.opam.lockfile;

import com.blackduck.integration.detectable.detectables.opam.lockfile.parse.OpamLockFileParser;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.executable.ExecutableRunnerException;

import java.io.File;
import java.util.List;

@DetectableInfo(name = "Opam Lock", language = "OCaml", forge = "opam", accuracy = DetectableAccuracyType.LOW, requirementsMarkdown = "File: opam files with extensions .opam and .opam.locked.")
public class OpamLockFileDetectable extends Detectable {

    private static final String OPAM_FILE = "*.opam";
    private static final String OPAM_LOCKED_FILE = "*.opam.locked";
    private final FileFinder fileFinder;
    private final OpamLockFileExtractor opamLockFileExtractor;


    public OpamLockFileDetectable(DetectableEnvironment environment, FileFinder fileFinder, OpamLockFileExtractor opamLockFileExtractor){
        super(environment);
        this.fileFinder = fileFinder;
        this.opamLockFileExtractor = opamLockFileExtractor;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        requirements.file(OPAM_LOCKED_FILE);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        Requirements requirements = new Requirements(fileFinder, environment);
        requirements.file(OPAM_FILE);
        return requirements.result();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) throws ExecutableRunnerException {
        List<File> opamFiles = fileFinder.findFiles(environment.getDirectory(),OPAM_FILE);

        List<File> opamLockFiles = fileFinder.findFiles(environment.getDirectory(), OPAM_LOCKED_FILE);
        OpamLockFileParser opamLockFileParser = new OpamLockFileParser(opamLockFiles);

        return opamLockFileExtractor.extract(opamFiles, opamLockFileParser);
    }

}
