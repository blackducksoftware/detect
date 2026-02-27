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
import com.blackduck.integration.detectable.detectables.bazel.DependencySource;
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
import java.io.File;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Objects;

/**
 * Detectable implementation for Bazel projects using Bazel CLI V2.
 */
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
        BazelGraphProber create(BazelCommandExecutor bazelCmd, String target, BazelEnvironmentAnalyzer.Mode mode);
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
            BazelGraphProber::new
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

        // Determine mode (either via override or auto-detection)
        BazelEnvironmentAnalyzer.Mode mode = determineMode(bazelCmd);

        // Determine pipelines (either from properties or by probing)
        Set<DependencySource> pipelines = resolvePipelines(bazelCmd, target, mode);

        // Fail if no supported pipelines are found
        if (pipelines == null || pipelines.isEmpty()) {
            throw new DetectableException("No supported Bazel dependency sources found for target '" + target + "'. To override, use detect.bazel.dependency.sources property.");
        }

        // Run the extraction using the determined pipelines
        BazelV2Extractor extractor = new BazelV2Extractor(externalIdFactory, bazelVariableSubstitutor, haskellParser, projectNameGenerator);
        Extraction extraction = extractor.run(bazelCmd, pipelines, target, mode);
        logger.info("Bazel V2 detectable finished.");
        return extraction;
    }

    // Helper to determine Bazel mode; extracted to reduce cognitive complexity in extract().
    private BazelEnvironmentAnalyzer.Mode determineMode(BazelCommandExecutor bazelCmd) throws DetectableException {
        Optional<BazelEnvironmentAnalyzer.Mode> modeOverride = options.getModeOverride();
        if (modeOverride.isPresent()) {
            BazelEnvironmentAnalyzer.Mode mode = modeOverride.get();
            logger.info("Using Bazel mode from property override: {}", mode);
            return mode;
        }

        File workspaceDir = environment.getDirectory();
        File moduleFile = new File(workspaceDir, "MODULE.bazel");
        if (!moduleFile.exists()) {
            logger.info("No MODULE.bazel found at {}. Skipping 'bazel mod graph' to avoid file generation; assuming WORKSPACE mode.", workspaceDir.getAbsolutePath());
            return BazelEnvironmentAnalyzer.Mode.WORKSPACE;
        }

        BazelEnvironmentAnalyzer envAnalyzer = new BazelEnvironmentAnalyzer(bazelCmd);
        BazelEnvironmentAnalyzer.Mode mode = envAnalyzer.getMode();

        if (mode == BazelEnvironmentAnalyzer.Mode.UNKNOWN) {
            throw new DetectableException(
                "Unable to determine Bazel mode automatically. " +
                "The 'bazel mod show_repo' command failed with an unexpected error. " +
                "Please set the 'detect.bazel.mode' property to either 'WORKSPACE' or 'BZLMOD' to proceed. " +
                "Example: --detect.bazel.mode=WORKSPACE"
            );
        }

        logger.info("Using Bazel mode from auto-detection: {}", mode);
        return mode;
    }

    // Helper to map legacy workspace rules to DependencySource
    private Set<DependencySource> mapWorkspaceRulesToSources(Set<WorkspaceRule> workspaceRules) {
        if (workspaceRules == null || workspaceRules.isEmpty()) {
            return Collections.emptySet();
        }
        return workspaceRules.stream()
            .map(rule -> {
                switch (rule) {
                    case MAVEN_JAR:
                        return DependencySource.MAVEN_JAR;
                    case MAVEN_INSTALL:
                        return DependencySource.MAVEN_INSTALL;
                    case HASKELL_CABAL_LIBRARY:
                        return DependencySource.HASKELL_CABAL_LIBRARY;
                    case HTTP_ARCHIVE:
                        return DependencySource.HTTP_ARCHIVE;
                    default:
                        logger.warn("Unrecognized workspace rule '{}' in detect.bazel.workspace.rules; skipping.", rule.getName());
                        return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    // Helper to resolve pipelines either from properties or by probing
    private Set<DependencySource> resolvePipelines(BazelCommandExecutor bazelCmd, String target, BazelEnvironmentAnalyzer.Mode mode) {
        Set<WorkspaceRule> workspaceRulesFromProperty = options.getWorkspaceRulesFromProperty();
        Set<DependencySource> sourcesFromProperty = options.getDependencySourcesFromProperty();

        if (!workspaceRulesFromProperty.isEmpty() && (sourcesFromProperty == null || sourcesFromProperty.isEmpty())) {
            logger.warn("Deprecated property `detect.bazel.workspace.rules` detected. Mapped to `detect.bazel.dependency.sources`. Please migrate to the new property. Alias will be removed in a future release.");
            sourcesFromProperty = mapWorkspaceRulesToSources(workspaceRulesFromProperty);
        }

        if (sourcesFromProperty != null && !sourcesFromProperty.isEmpty()) {
            logger.info("Using detect.bazel.dependency.sources override; skipping graph probing. Pipelines: {}", sourcesFromProperty);
            return sourcesFromProperty;
        }

        BazelGraphProber prober = bazelGraphProberFactory.create(bazelCmd, target, mode);
        return prober.decidePipelines();
    }
}
