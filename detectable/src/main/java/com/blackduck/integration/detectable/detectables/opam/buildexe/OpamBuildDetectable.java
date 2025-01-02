package com.blackduck.integration.detectable.detectables.opam.buildexe;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.OpamResolver;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.ExecutableNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.executable.ExecutableRunnerException;

import java.io.File;
import java.util.List;


@DetectableInfo(name = "Opam CLI", language = "OCaml", forge = "opam", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: an opam file with .opam extension. Executable: opam.")
public class OpamBuildDetectable extends Detectable {

    private static final String OPAM_FILE = "*.opam";
    private final FileFinder fileFinder;
    private final OpamResolver opamResolver;
    private final OpamBuildExtractor opamBuildExtractor;

    private ExecutableTarget opamExe;

    public OpamBuildDetectable(DetectableEnvironment environment, FileFinder fileFinder, OpamResolver opamResolver, OpamBuildExtractor opamBuildExtractor){
        super(environment);
        this.fileFinder = fileFinder;
        this.opamResolver = opamResolver;
        this.opamBuildExtractor = opamBuildExtractor;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        requirements.file(OPAM_FILE);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        opamExe = opamResolver.resolveOpam();

        if(opamExe == null) {
            return new ExecutableNotFoundDetectableResult("opam");
        }

        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) throws ExecutableRunnerException {
        List<File> opamFiles = fileFinder.findFiles(environment.getDirectory(),OPAM_FILE);
        return opamBuildExtractor.extract(opamFiles, opamExe, extractionEnvironment.getOutputDirectory());
    }

}
