package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectable.executable.resolver.BazelResolver;
import com.blackduck.integration.detectable.detectable.explanation.PropertyProvided;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PropertyInsufficientDetectableResult;
import com.blackduck.integration.detectable.detectables.bazel.BazelDetectableOptions;
import com.blackduck.integration.detectable.detectables.bazel.BazelProjectNameGenerator;
import com.blackduck.integration.detectable.detectables.bazel.WorkspaceRule;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelVariableSubstitutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.HaskellCabalLibraryJsonProtoParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@DetectableInfo(name = "Bazel CLI V2", language = "various", forge = "Maven Central", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "Executable: bazel. Property: detect.bazel.target.")
public class BazelV2Detectable extends Detectable {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final FileFinder fileFinder;
    private final DetectableExecutableRunner executableRunner;
    private final ExternalIdFactory externalIdFactory;
    private final BazelResolver bazelResolver;
    private final BazelDetectableOptions options;
    private final BazelVariableSubstitutor bazelVariableSubstitutor;
    private final HaskellCabalLibraryJsonProtoParser haskellParser;
    private final BazelProjectNameGenerator projectNameGenerator;

    private ExecutableTarget bazelExe;

    public BazelV2Detectable(DetectableEnvironment environment,
                              FileFinder fileFinder,
                              DetectableExecutableRunner executableRunner,
                              ExternalIdFactory externalIdFactory,
                              BazelResolver bazelResolver,
                              BazelDetectableOptions options,
                              BazelVariableSubstitutor bazelVariableSubstitutor,
                              HaskellCabalLibraryJsonProtoParser haskellParser,
                              BazelProjectNameGenerator projectNameGenerator) {
        super(environment);
        this.fileFinder = fileFinder;
        this.executableRunner = executableRunner;
        this.externalIdFactory = externalIdFactory;
        this.bazelResolver = bazelResolver;
        this.options = options;
        this.bazelVariableSubstitutor = bazelVariableSubstitutor;
        this.haskellParser = haskellParser;
        this.projectNameGenerator = projectNameGenerator;
    }

    @Override
    public DetectableResult applicable() {
        if (options.getTargetName().isPresent()) {
            return new PassedDetectableResult(new PropertyProvided("Bazel Target"));
        }
        return new PropertyInsufficientDetectableResult();
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        Requirements req = new Requirements(fileFinder, environment);
        bazelExe = req.executable(bazelResolver::resolveBazel, "bazel");
        return req.result();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) throws DetectableException, ExecutableFailedException {
        String target = options.getTargetName().orElseThrow(() -> new DetectableException("Missing detect.bazel.target"));
        logger.info("Bazel V2 detectable starting. Target: {}", target);
        BazelCommandExecutor bazelCmd = new BazelCommandExecutor(executableRunner, environment.getDirectory(), bazelExe);
        BazelGraphProber prober = new BazelGraphProber(bazelCmd, target, 20);
        // Prober now logs and continues on individual probe failures; do not abort extraction here.
        Set<WorkspaceRule> pipelines = prober.decidePipelines();
        // If no pipelines are detected, fail with a clear v2-specific message.
        if (pipelines == null || pipelines.isEmpty()) {
            throw new DetectableException("No supported Bazel dependency pipelines detected for given target.");
        }
        BazelV2Extractor extractor = new BazelV2Extractor(externalIdFactory, bazelVariableSubstitutor, haskellParser, projectNameGenerator);
        Extraction extraction = extractor.run(bazelCmd, pipelines, target);
        logger.info("Bazel V2 detectable finished.");
        return extraction;
    }
}
