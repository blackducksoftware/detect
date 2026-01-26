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
import com.blackduck.integration.detectable.util.ToolVersionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
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

    // Factory interface to create BazelGraphProber instances; injectable for tests.
    public interface BazelGraphProberFactory {
        BazelGraphProber create(BazelCommandExecutor bazelCmd, String target, BazelEnvironmentAnalyzer.Mode mode, int httpProbeLimit);
    }

    private final BazelGraphProberFactory bazelGraphProberFactory;

    /**
     * Constructor for BazelV2Detectable
     * @param environment The detectable environment
     * @param fileFinder File finder utility
     * @param executableRunner Executable runner
     * @param externalIdFactory External ID factory
     * @param bazelResolver Bazel executable resolver
     * @param options Bazel detectable options
     * @param bazelVariableSubstitutor Variable substitutor for Bazel
     * @param haskellParser Haskell cabal library parser
     * @param projectNameGenerator Project name generator
     */
    public BazelV2Detectable(DetectableEnvironment environment,
                              FileFinder fileFinder,
                              DetectableExecutableRunner executableRunner,
                              ExternalIdFactory externalIdFactory,
                              BazelResolver bazelResolver,
                              BazelDetectableOptions options,
                              BazelVariableSubstitutor bazelVariableSubstitutor,
                              HaskellCabalLibraryJsonProtoParser haskellParser,
                              BazelProjectNameGenerator projectNameGenerator) {
        this(environment, fileFinder, executableRunner, externalIdFactory, bazelResolver, options, bazelVariableSubstitutor, haskellParser, projectNameGenerator,
            (bazelCmd, target, mode, httpProbeLimit) -> new BazelGraphProber(bazelCmd, target, mode, httpProbeLimit)
        );
    }

    /**
     * Testable constructor allowing injection of a custom BazelGraphProberFactory.
     */
    public BazelV2Detectable(DetectableEnvironment environment,
                              FileFinder fileFinder,
                              DetectableExecutableRunner executableRunner,
                              ExternalIdFactory externalIdFactory,
                              BazelResolver bazelResolver,
                              BazelDetectableOptions options,
                              BazelVariableSubstitutor bazelVariableSubstitutor,
                              HaskellCabalLibraryJsonProtoParser haskellParser,
                              BazelProjectNameGenerator projectNameGenerator,
                              BazelGraphProberFactory bazelGraphProberFactory) {
        super(environment);
        this.fileFinder = fileFinder;
        this.executableRunner = executableRunner;
        this.externalIdFactory = externalIdFactory;
        this.bazelResolver = bazelResolver;
        this.options = options;
        this.bazelVariableSubstitutor = bazelVariableSubstitutor;
        this.haskellParser = haskellParser;
        this.projectNameGenerator = projectNameGenerator;
        this.bazelGraphProberFactory = bazelGraphProberFactory;
    }

    /**
     * Checks if the detectable is applicable by verifying the Bazel target property is present.
     */
    @Override
    public DetectableResult applicable() {
        if (options.getTargetName().isPresent()) {
            return new PassedDetectableResult(new PropertyProvided("Bazel Target"));
        }
        return new PropertyInsufficientDetectableResult();
    }

    /**
     * Checks if the Bazel executable is available and sets it for later use.
     */
    @Override
    public DetectableResult extractable() throws DetectableException {
        Requirements req = new Requirements(fileFinder, environment);
        bazelExe = req.executable(bazelResolver::resolveBazel, "bazel");
        return req.result();
    }

    /**
     * Main extraction logic for Bazel V2 detectable.
     * Probes the Bazel environment, determines which dependency pipelines to use, and runs extraction.
     */
    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) throws DetectableException, ExecutableFailedException {
        // Get the Bazel target from options or throw if missing
        String target = options.getTargetName().orElseThrow(() -> new DetectableException("Missing detect.bazel.target"));
        logger.info("Bazel V2 detectable starting. Target: {}", target);
        // Log Bazel tool version similar to v1 behavior
        new ToolVersionLogger(executableRunner).log(environment.getDirectory(), bazelExe, "version");

        // Set up Bazel command executor and determine environment mode
        BazelCommandExecutor bazelCmd = new BazelCommandExecutor(executableRunner, environment.getDirectory(), bazelExe);
        BazelEnvironmentAnalyzer.Mode mode;

        // Check for mode override property first
        Optional<BazelEnvironmentAnalyzer.Mode> modeOverride = options.getModeOverride();
        if (modeOverride.isPresent()) {
            mode = modeOverride.get();
            logger.info("Using Bazel mode from property override: {}", mode);
        } else {
            // Auto-detect mode if not overridden
            BazelEnvironmentAnalyzer envAnalyzer = new BazelEnvironmentAnalyzer(bazelCmd);
            mode = envAnalyzer.getMode();
            logger.info("Using Bazel mode from auto-detection: {}", mode);
        }

        Set<WorkspaceRule> pipelines;
        // Check if workspace rules are provided via property; if not, probe the Bazel graph
        Set<WorkspaceRule> rulesFromProperty = options.getWorkspaceRulesFromProperty();
        if (rulesFromProperty != null && !rulesFromProperty.isEmpty()) {
            logger.info("Using detect.bazel.workspace.rules override; skipping graph probing. Pipelines: {}", rulesFromProperty);
            pipelines = rulesFromProperty;
        } else {
            // Probe the Bazel dependency graph to determine enabled pipelines
            BazelGraphProber prober = bazelGraphProberFactory.create(bazelCmd, target, mode, options.getHttpProbeLimit());
            pipelines = prober.decidePipelines();
        }

        // Fail if no supported pipelines are found
        if (pipelines == null || pipelines.isEmpty()) {
            throw new DetectableException("No supported Bazel dependency sources found for target '" + target + "'. To override, use detect.bazel.workspace.rules property.");
        }

        // Run the extraction using the determined pipelines
        BazelV2Extractor extractor = new BazelV2Extractor(externalIdFactory, bazelVariableSubstitutor, haskellParser, projectNameGenerator);
        Extraction extraction = extractor.run(bazelCmd, pipelines, target, mode);
        logger.info("Bazel V2 detectable finished.");
        return extraction;
    }
}
