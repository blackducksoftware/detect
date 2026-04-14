package com.blackduck.integration.detectable.detectables.conda.cli;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.CondaResolver;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectables.conda.CondaCliDetectableOptions;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(name = "Conda CLI", language = "Python", forge = "Anaconda", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: environment.yml or environment.yaml. Executable: conda.")
public class CondaCliDetectable extends Detectable {
    public static final String ENVIRONMENT_YML = "environment.yml";
    public static final String ENVIRONMENT_YAML = "environment.yaml";
    private final FileFinder fileFinder;
    private final CondaResolver condaResolver;
    private final CondaCliExtractor condaExtractor;
    private final CondaCliDetectableOptions condaCliDetectableOptions;

    private ExecutableTarget condaExe;

    public CondaCliDetectable(
        DetectableEnvironment environment,
        FileFinder fileFinder,
        CondaResolver condaResolver,
        CondaCliExtractor condaExtractor,
        CondaCliDetectableOptions condaCliDetectableOptions
    ) {
        super(environment);
        this.fileFinder = fileFinder;
        this.condaResolver = condaResolver;
        this.condaExtractor = condaExtractor;
        this.condaCliDetectableOptions = condaCliDetectableOptions;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        requirements.eitherFile(ENVIRONMENT_YML, ENVIRONMENT_YAML);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        Requirements requirements = new Requirements(fileFinder, environment);
        condaExe = requirements.executable(condaResolver::resolveConda, "conda");
        return requirements.result();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        return condaExtractor.extract(
            environment.getDirectory(),
            condaExe,
            extractionEnvironment.getOutputDirectory(),
            condaCliDetectableOptions.getCondaEnvironmentName().orElse("")
        );
    }

}
