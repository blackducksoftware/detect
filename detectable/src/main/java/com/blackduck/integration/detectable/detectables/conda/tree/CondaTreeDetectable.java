package com.blackduck.integration.detectable.detectables.conda.tree;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.resolver.CondaTreeResolver;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectables.conda.CondaCliDetectableOptions;
import com.blackduck.integration.detectable.detectables.conda.cli.CondaCliExtractor;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(name = "Conda Tree", language = "Python", forge = "Anaconda", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: environment.yml or environment.yaml. Executable: conda-tree.")
public class CondaTreeDetectable extends Detectable {

    public static final String ENVIRONMENT_YML = "environment.yml";
    public static final String ENVIRONMENT_YAML = "environment.yaml";
    private final FileFinder fileFinder;
    private final CondaTreeResolver condaTreeResolver;
    private final CondaTreeExtractor condaTreeExtractor;
    private final CondaCliDetectableOptions condaCliDetectableOptions;

    private ExecutableTarget condaTreeExe;

    public CondaTreeDetectable (
            DetectableEnvironment environment,
            FileFinder fileFinder,
            CondaTreeResolver condaTreeResolver,
            CondaTreeExtractor condaTreeExtractor,
            CondaCliDetectableOptions condaCliDetectableOptions
    ) {
        super(environment);
        this.fileFinder = fileFinder;
        this.condaTreeResolver = condaTreeResolver;
        this.condaTreeExtractor = condaTreeExtractor;
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
        condaTreeExe = requirements.executable(condaTreeResolver::resolveCondaTree, "conda-tree");
        return requirements.result();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        return condaTreeExtractor.extract(
                environment.getDirectory(),
                condaTreeExe,
                extractionEnvironment.getOutputDirectory(),
                condaCliDetectableOptions.getCondaEnvironmentName().orElse("")
        );
    }
}
