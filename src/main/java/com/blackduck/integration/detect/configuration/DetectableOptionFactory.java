package com.blackduck.integration.detect.configuration;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.blackduck.integration.detectable.detectables.cargo.CargoDetectableOptions;
import com.blackduck.integration.detectable.detectables.cargo.CargoDependencyType;
import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenMirrorConfig;
import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenSettingsParseException;
import com.blackduck.integration.detectable.detectables.maven.resolver.mirror.MavenSettingsParser;
import com.blackduck.integration.detectable.detectables.nuget.NugetDependencyType;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detect.workflow.diagnostic.DiagnosticSystem;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.bazel.BazelDetectableOptions;
import com.blackduck.integration.detectable.detectables.bazel.WorkspaceRule;
import com.blackduck.integration.detectable.detectables.bitbake.BitbakeDependencyType;
import com.blackduck.integration.detectable.detectables.bitbake.BitbakeDetectableOptions;
import com.blackduck.integration.detectable.detectables.clang.ClangDetectableOptions;
import com.blackduck.integration.detectable.detectables.conan.cli.config.ConanCliOptions;
import com.blackduck.integration.detectable.detectables.conan.cli.config.ConanDependencyType;
import com.blackduck.integration.detectable.detectables.conan.lockfile.ConanLockfileExtractorOptions;
import com.blackduck.integration.detectable.detectables.conda.CondaCliDetectableOptions;
import com.blackduck.integration.detectable.detectables.dart.pubdep.DartPubDependencyType;
import com.blackduck.integration.detectable.detectables.dart.pubdep.DartPubDepsDetectableOptions;
import com.blackduck.integration.detectable.detectables.docker.DockerDetectableOptions;
import com.blackduck.integration.detectable.detectables.go.gomod.GoModCliDetectableOptions;
import com.blackduck.integration.detectable.detectables.go.gomod.GoModDependencyType;
import com.blackduck.integration.detectable.detectables.go.gomodfile.GoModFileDetectableOptions;
import com.blackduck.integration.detectable.detectables.gradle.inspection.GradleConfigurationType;
import com.blackduck.integration.detectable.detectables.gradle.inspection.GradleInspectorOptions;
import com.blackduck.integration.detectable.detectables.gradle.inspection.inspector.GradleInspectorScriptOptions;
import com.blackduck.integration.detectable.detectables.lerna.LernaOptions;
import com.blackduck.integration.detectable.detectables.lerna.LernaPackageType;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenCliExtractorOptions;
import com.blackduck.integration.detectable.detectables.maven.resolver.MavenResolverOptions;
import com.blackduck.integration.detectable.detectables.npm.NpmDependencyType;
import com.blackduck.integration.detectable.detectables.npm.cli.NpmCliExtractorOptions;
import com.blackduck.integration.detectable.detectables.npm.lockfile.NpmLockfileOptions;
import com.blackduck.integration.detectable.detectables.npm.packagejson.NpmPackageJsonParseDetectableOptions;
import com.blackduck.integration.detectable.detectables.nuget.NugetInspectorOptions;
import com.blackduck.integration.detectable.detectables.packagist.ComposerLockDetectableOptions;
import com.blackduck.integration.detectable.detectables.packagist.PackagistDependencyType;
import com.blackduck.integration.detectable.detectables.pear.PearCliDetectableOptions;
import com.blackduck.integration.detectable.detectables.pear.PearDependencyType;
import com.blackduck.integration.detectable.detectables.pip.inspector.PipInspectorDetectableOptions;
import com.blackduck.integration.detectable.detectables.pip.parser.RequirementsFileDetectableOptions;
import com.blackduck.integration.detectable.detectables.pipenv.tbuild.PipenvDetectableOptions;
import com.blackduck.integration.detectable.detectables.pipenv.parse.PipenvDependencyType;
import com.blackduck.integration.detectable.detectables.pipenv.parse.PipfileLockDetectableOptions;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.PnpmLockOptions;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmDependencyType;
import com.blackduck.integration.detectable.detectables.poetry.PoetryOptions;
import com.blackduck.integration.detectable.detectables.projectinspector.ProjectInspectorOptions;
import com.blackduck.integration.detectable.detectables.rubygems.GemspecDependencyType;
import com.blackduck.integration.detectable.detectables.rubygems.gemspec.GemspecParseDetectableOptions;
import com.blackduck.integration.detectable.detectables.sbt.SbtDetectableOptions;
import com.blackduck.integration.detectable.detectables.yarn.YarnDependencyType;
import com.blackduck.integration.detectable.detectables.yarn.YarnLockOptions;
import com.blackduck.integration.log.LogLevel;
import com.blackduck.integration.rest.proxy.ProxyInfo;

public class DetectableOptionFactory {

    private static final Logger logger = LoggerFactory.getLogger(DetectableOptionFactory.class);

    private final DetectPropertyConfiguration detectConfiguration;
    @Nullable
    private final DiagnosticSystem diagnosticSystem;
    private final ProxyInfo proxyInfo;

    public DetectableOptionFactory(DetectPropertyConfiguration detectConfiguration, @Nullable DiagnosticSystem diagnosticSystem, ProxyInfo proxyInfo) {
        this.detectConfiguration = detectConfiguration;
        this.diagnosticSystem = diagnosticSystem;
        this.proxyInfo = proxyInfo;
    }

    public BazelDetectableOptions createBazelDetectableOptions() {
        String targetName = detectConfiguration.getNullableValue(DetectProperties.DETECT_BAZEL_TARGET);
        List<String> bazelCqueryAdditionalOptions = detectConfiguration.getValue(DetectProperties.DETECT_BAZEL_CQUERY_OPTIONS);
        Set<WorkspaceRule> workspaceRulesFromProperty = detectConfiguration.getValue(DetectProperties.DETECT_BAZEL_WORKSPACE_RULES).representedValueSet();
        return new BazelDetectableOptions(targetName, workspaceRulesFromProperty, bazelCqueryAdditionalOptions);
    }

    public BitbakeDetectableOptions createBitbakeDetectableOptions() {
        String buildEnvName = detectConfiguration.getValue(DetectProperties.DETECT_BITBAKE_BUILD_ENV_NAME);
        List<String> sourceArguments = detectConfiguration.getValue(DetectProperties.DETECT_BITBAKE_SOURCE_ARGUMENTS);
        List<String> packageNames = detectConfiguration.getValue(DetectProperties.DETECT_BITBAKE_PACKAGE_NAMES);
        Integer searchDepth = detectConfiguration.getValue(DetectProperties.DETECT_BITBAKE_SEARCH_DEPTH);
        Set<BitbakeDependencyType> excludedDependencyTypes = detectConfiguration.getValue(DetectProperties.DETECT_BITBAKE_DEPENDENCY_TYPES_EXCLUDED).representedValueSet();
        EnumListFilter<BitbakeDependencyType> dependencyTypeFilter = EnumListFilter.fromExcluded(excludedDependencyTypes);
        return new BitbakeDetectableOptions(buildEnvName, sourceArguments, packageNames, searchDepth, getFollowSymLinks(), dependencyTypeFilter);
    }

    public ClangDetectableOptions createClangDetectableOptions() {
        Boolean cleanup = detectConfiguration.getValue(DetectProperties.DETECT_CLEANUP);
        return new ClangDetectableOptions(cleanup);
    }

    public ComposerLockDetectableOptions createComposerLockDetectableOptions() {
        Set<PackagistDependencyType> excludedDependencyTypes = detectConfiguration.getValue(DetectProperties.DETECT_PACKAGIST_DEPENDENCY_TYPES_EXCLUDED).representedValueSet();
        EnumListFilter<PackagistDependencyType> packagistDependencyTypeFilter = EnumListFilter.fromExcluded(excludedDependencyTypes);
        return new ComposerLockDetectableOptions(packagistDependencyTypeFilter);
    }

    public CondaCliDetectableOptions createCondaOptions() {
        String environmentName = detectConfiguration.getNullableValue(DetectProperties.DETECT_CONDA_ENVIRONMENT_NAME);
        return new CondaCliDetectableOptions(environmentName);
    }

    public DartPubDepsDetectableOptions createDartPubDepsDetectableOptions() {
        Set<DartPubDependencyType> excludedDependencyTypes = detectConfiguration.getValue(DetectProperties.DETECT_PUB_DEPENDENCY_TYPES_EXCLUDED).representedValueSet();
        EnumListFilter<DartPubDependencyType> dependencyTypeFilter = EnumListFilter.fromExcluded(excludedDependencyTypes);
        return new DartPubDepsDetectableOptions(dependencyTypeFilter);
    }

    public DockerDetectableOptions createDockerDetectableOptions() {
        String suppliedDockerImage = detectConfiguration.getNullableValue(DetectProperties.DETECT_DOCKER_IMAGE);
        String dockerImageId = detectConfiguration.getNullableValue(DetectProperties.DETECT_DOCKER_IMAGE_ID);
        String suppliedDockerTar = detectConfiguration.getNullableValue(DetectProperties.DETECT_DOCKER_TAR);
        LogLevel dockerInspectorLoggingLevel = detectConfiguration.getValue(DetectProperties.LOGGING_LEVEL_DETECT);
        String dockerInspectorVersion = detectConfiguration.getNullableValue(DetectProperties.DETECT_DOCKER_INSPECTOR_VERSION);
        Map<String, String> additionalDockerProperties = detectConfiguration.getRaw(DetectProperties.DOCKER_PASSTHROUGH);
        if (diagnosticSystem != null) {
            additionalDockerProperties.putAll(diagnosticSystem.getAdditionalDockerProperties());
        }

        Path dockerInspectorPath = detectConfiguration.getPathOrNull(DetectProperties.DETECT_DOCKER_INSPECTOR_PATH);
        String dockerPlatformTopLayerId = detectConfiguration.getNullableValue(DetectProperties.DETECT_DOCKER_PLATFORM_TOP_LAYER_ID);
        return new DockerDetectableOptions(
            suppliedDockerImage,
            dockerImageId,
            suppliedDockerTar,
            dockerInspectorLoggingLevel,
            dockerInspectorVersion,
            additionalDockerProperties,
            dockerInspectorPath,
            dockerPlatformTopLayerId
        );
    }

    public GoModCliDetectableOptions createGoModCliDetectableOptions() {
        GoModDependencyType excludedDependencyType = detectConfiguration.getValue(DetectProperties.DETECT_GO_MOD_DEPENDENCY_TYPES_EXCLUDED);
        return new GoModCliDetectableOptions(excludedDependencyType);
    }

    public GoModFileDetectableOptions createGoModFileDetectableOptions() {
        String goForgeUrl = detectConfiguration.getNullableValue(DetectProperties.DETECT_GO_FORGE);
        if (goForgeUrl == null || goForgeUrl.isEmpty()) {
            goForgeUrl = "https://proxy.golang.org";
        }
        // if the URL ends with a trailing slash, remove it
        if (goForgeUrl.endsWith("/")) {
            goForgeUrl = goForgeUrl.substring(0, goForgeUrl.length() - 1);
        }
        int connectionTimeout = detectConfiguration.getValue(DetectProperties.DETECT_GO_FORGE_CONNECTION_TIMEOUT);
        if (connectionTimeout <= 0) {
            connectionTimeout = 30; // default to 30 seconds if not set
        }
        int readTimeout = detectConfiguration.getValue(DetectProperties.DETECT_GO_FORGE_READ_TIMEOUT);
        if (readTimeout <= 0) {
            readTimeout = 60; // default to 60 seconds if not set
        }
        return new GoModFileDetectableOptions(goForgeUrl, connectionTimeout, readTimeout);
    }

    public GradleInspectorOptions createGradleInspectorOptions() {
        List<String> excludedProjectNames = detectConfiguration.getValue(DetectProperties.DETECT_GRADLE_EXCLUDED_PROJECTS);
        List<String> includedProjectNames = detectConfiguration.getValue(DetectProperties.DETECT_GRADLE_INCLUDED_PROJECTS);
        List<String> excludedProjectPaths = detectConfiguration.getValue(DetectProperties.DETECT_GRADLE_EXCLUDED_PROJECT_PATHS);
        List<String> includedProjectPaths = detectConfiguration.getValue(DetectProperties.DETECT_GRADLE_INCLUDED_PROJECT_PATHS);
        List<String> excludedConfigurationNames = detectConfiguration.getValue(DetectProperties.DETECT_GRADLE_EXCLUDED_CONFIGURATIONS);
        List<String> includedConfigurationNames = detectConfiguration.getValue(DetectProperties.DETECT_GRADLE_INCLUDED_CONFIGURATIONS);
        boolean rootOnlyOption = detectConfiguration.getValue(DetectProperties.DETECT_GRADLE_ROOT_ONLY);

        Set<GradleConfigurationType> excludedConfigurationTypes = detectConfiguration.getValue(DetectProperties.DETECT_GRADLE_CONFIGURATION_TYPES_EXCLUDED).representedValueSet();
        EnumListFilter<GradleConfigurationType> dependencyTypeFilter = EnumListFilter.fromExcluded(excludedConfigurationTypes);

        GradleInspectorScriptOptions scriptOptions = new GradleInspectorScriptOptions(
            excludedProjectNames,
            includedProjectNames,
            excludedProjectPaths,
            includedProjectPaths,
            excludedConfigurationNames,
            includedConfigurationNames,
            rootOnlyOption
        );
        String gradleBuildCommand = detectConfiguration.getNullableValue(DetectProperties.DETECT_GRADLE_BUILD_COMMAND);
        return new GradleInspectorOptions(gradleBuildCommand, scriptOptions, proxyInfo, dependencyTypeFilter);
    }

    public LernaOptions createLernaOptions() {
        Set<LernaPackageType> excludedDependencyTypes = detectConfiguration.getValue(DetectProperties.DETECT_LERNA_PACKAGE_TYPES_EXCLUDED).representedValueSet();
        EnumListFilter<LernaPackageType> lernaPackageTypeFilter = EnumListFilter.fromExcluded(excludedDependencyTypes);

        List<String> excludedPackages = detectConfiguration.getValue(DetectProperties.DETECT_LERNA_EXCLUDED_PACKAGES);
        List<String> includedPackages = detectConfiguration.getValue(DetectProperties.DETECT_LERNA_INCLUDED_PACKAGES);
        return new LernaOptions(lernaPackageTypeFilter, excludedPackages, includedPackages);
    }

    public MavenCliExtractorOptions createMavenCliOptions() {
        String mavenBuildCommand = detectConfiguration.getNullableValue(DetectProperties.DETECT_MAVEN_BUILD_COMMAND);
        List<String> mavenExcludedScopes = detectConfiguration.getValue(DetectProperties.DETECT_MAVEN_EXCLUDED_SCOPES);
        List<String> mavenIncludedScopes = detectConfiguration.getValue(DetectProperties.DETECT_MAVEN_INCLUDED_SCOPES);
        List<String> mavenExcludedModules = detectConfiguration.getValue(DetectProperties.DETECT_MAVEN_EXCLUDED_MODULES);
        List<String> mavenIncludedModules = detectConfiguration.getValue(DetectProperties.DETECT_MAVEN_INCLUDED_MODULES);
        Boolean includeShadedDependencies = detectConfiguration.getValue(DetectProperties.DETECT_MAVEN_INCLUDE_SHADED_DEPENDENCIES);
        return new MavenCliExtractorOptions(mavenBuildCommand, mavenExcludedScopes, mavenIncludedScopes, mavenExcludedModules, mavenIncludedModules, includeShadedDependencies);
    }

    public MavenResolverOptions createMavenResolverOptions() {
        List<String> externalRepositories = detectConfiguration.getValue(DetectProperties.DETECT_MAVEN_INCLUDE_EXTERNAL_REPOSITORIES);

        // Extract proxy details from the global ProxyInfo (sourced from blackduck.proxy.* properties).
        // proxyHost is a bare hostname/IP — no http:// or https:// prefix.
        String proxyHost = proxyInfo.getHost().orElse(null);
        int proxyPort = proxyInfo.getPort();
        String proxyUsername = null;
        String proxyPassword = null;
        if (proxyInfo.getProxyCredentials().isPresent()) {
            com.blackduck.integration.rest.credentials.Credentials creds = proxyInfo.getProxyCredentials().get();
            proxyUsername = creds.getUsername().orElse(null);
            proxyPassword = creds.getPassword().orElse(null);
        }

        // Read the global proxy-bypass list (blackduck.proxy.ignored.hosts).
        List<String> proxyIgnoredHosts = detectConfiguration.getValue(DetectProperties.BLACKDUCK_PROXY_IGNORED_HOSTS);

        // Resolve mirror configurations using precedence logic:
        // Priority 1: CLI mirror properties
        // Priority 2: settings.xml file
        // Priority 3: No mirrors (empty list)
        List<MavenMirrorConfig> mirrorConfigurations = resolveMirrorConfigurations();

        return new MavenResolverOptions(externalRepositories, proxyHost, proxyPort, proxyUsername, proxyPassword, proxyIgnoredHosts, mirrorConfigurations);
    }

    /**
     * Resolves mirror configurations using the following precedence:
     *
     * <ol>
     *   <li><strong>Priority 1 - CLI Override:</strong> If detect.maven.buildless.mirror.url is provided,
     *       create a single mirror config from CLI properties. Settings.xml is ignored.</li>
     *   <li><strong>Priority 2 - settings.xml Fallback:</strong> If CLI URL is absent, parse settings.xml
     *       (from explicit path or default ~/.m2/settings.xml) for mirror configurations.</li>
     *   <li><strong>Priority 3 - Default:</strong> If no mirrors found, return empty list.</li>
     * </ol>
     *
     * @return List of mirror configurations, never null
     */
    private List<MavenMirrorConfig> resolveMirrorConfigurations() {
        // Priority 1: CLI Override
        String cliMirrorUrl = detectConfiguration.getNullableValue(DetectProperties.DETECT_MAVEN_BUILDLESS_MIRROR_URL);

        if (cliMirrorUrl != null && !cliMirrorUrl.trim().isEmpty()) {
            logger.info("Using CLI mirror configuration. Ignoring settings.xml mirrors.");

            String mirrorOf = detectConfiguration.getNullableValue(DetectProperties.DETECT_MAVEN_BUILDLESS_MIRROR_OF);
            // If mirror.url is provided but mirror.of is blank, default mirror.of to * (intercept everything)
            // to prevent unexpected bypasses.
            if (mirrorOf == null || mirrorOf.trim().isEmpty()) {
                mirrorOf = "*";
                logger.info("No mirrorOf pattern specified. Defaulting to '*' (intercept all repositories).");
            }

            String username = detectConfiguration.getNullableValue(DetectProperties.DETECT_MAVEN_BUILDLESS_MIRROR_USERNAME);
            String password = detectConfiguration.getNullableValue(DetectProperties.DETECT_MAVEN_BUILDLESS_MIRROR_PASSWORD);

            MavenMirrorConfig cliMirror = new MavenMirrorConfig(
                "detect-cli-mirror",
                cliMirrorUrl.trim(),
                mirrorOf.trim(),
                username,
                password
            );

            logger.info("CLI mirror configured: url='{}', mirrorOf='{}', hasAuth={}",
                cliMirrorUrl, mirrorOf, (username != null && password != null));

            return Collections.singletonList(cliMirror);
        }

        // Priority 2: settings.xml Fallback
        Path settingsFilePath = detectConfiguration.getPathOrNull(DetectProperties.DETECT_MAVEN_BUILDLESS_SETTINGS_FILE_PATH);
        MavenSettingsParser settingsParser = new MavenSettingsParser();

        try {
            List<MavenMirrorConfig> mirrorConfigs;

            if (settingsFilePath != null) {
                // Explicit path provided - fail if file doesn't exist
                logger.info("Parsing mirrors from specified settings.xml: {}", settingsFilePath);
                mirrorConfigs = settingsParser.parseSettingsFile(settingsFilePath);
            } else {
                // Use default path (~/.m2/settings.xml) - don't fail if missing
                mirrorConfigs = settingsParser.parseDefaultSettings();
            }

            if (!mirrorConfigs.isEmpty()) {
                logger.info("Loaded {} mirror(s) from settings.xml", mirrorConfigs.size());
            }

            return mirrorConfigs;

        } catch (MavenSettingsParseException e) {
            // If explicitly provided settings file fails to parse, we need to propagate this error.
            // Wrap in RuntimeException to fail the extraction - this will be caught by the detectable.
            logger.error("Failed to parse Maven settings.xml: {}", e.getMessage());
            throw new RuntimeException("Failed to parse Maven settings.xml for mirror configuration: " + e.getMessage(), e);
        }
    }

    public ConanCliOptions createConanCliOptions() {
        Path lockfilePath = detectConfiguration.getPathOrNull(DetectProperties.DETECT_CONAN_LOCKFILE_PATH);
        String additionalArguments = detectConfiguration.getNullableValue(DetectProperties.DETECT_CONAN_ARGUMENTS);
        Boolean preferLongFormExternalIds = detectConfiguration.getValue(DetectProperties.DETECT_CONAN_REQUIRE_PREV_MATCH);
        Set<ConanDependencyType> excludedDependencyTypes = detectConfiguration.getValue(DetectProperties.DETECT_CONAN_DEPENDENCY_TYPES_EXCLUDED).representedValueSet();
        EnumListFilter<ConanDependencyType> dependencyTypeFilter = EnumListFilter.fromExcluded(excludedDependencyTypes);
        return new ConanCliOptions(lockfilePath, additionalArguments, dependencyTypeFilter, preferLongFormExternalIds);
    }

    public ConanLockfileExtractorOptions createConanLockfileOptions() {
        Path lockfilePath = detectConfiguration.getPathOrNull(DetectProperties.DETECT_CONAN_LOCKFILE_PATH);
        Boolean preferLongFormExternalIds = detectConfiguration.getValue(DetectProperties.DETECT_CONAN_REQUIRE_PREV_MATCH);
        Set<ConanDependencyType> excludedDependencyTypes = detectConfiguration.getValue(DetectProperties.DETECT_CONAN_DEPENDENCY_TYPES_EXCLUDED).representedValueSet();
        EnumListFilter<ConanDependencyType> dependencyTypeFilter = EnumListFilter.fromExcluded(excludedDependencyTypes);
        return new ConanLockfileExtractorOptions(lockfilePath, dependencyTypeFilter, preferLongFormExternalIds);
    }

    public NpmCliExtractorOptions createNpmCliExtractorOptions() {
        EnumListFilter<NpmDependencyType> npmDependencyTypeFilter = createNpmDependencyTypeFilter();
        String npmArguments = detectConfiguration.getNullableValue(DetectProperties.DETECT_NPM_ARGUMENTS);
        return new NpmCliExtractorOptions(npmDependencyTypeFilter, npmArguments);
    }

    public NpmLockfileOptions createNpmLockfileOptions() {
        EnumListFilter<NpmDependencyType> npmDependencyTypeFilter = createNpmDependencyTypeFilter();
        return new NpmLockfileOptions(npmDependencyTypeFilter);
    }

    public NpmPackageJsonParseDetectableOptions createNpmPackageJsonParseDetectableOptions() {
        EnumListFilter<NpmDependencyType> npmDependencyTypeFilter = createNpmDependencyTypeFilter();
        return new NpmPackageJsonParseDetectableOptions(npmDependencyTypeFilter);
    }

    private EnumListFilter<NpmDependencyType> createNpmDependencyTypeFilter() {
        Set<NpmDependencyType> excludedDependencyTypes = detectConfiguration.getValue(DetectProperties.DETECT_NPM_DEPENDENCY_TYPES_EXCLUDED).representedValueSet();
        return EnumListFilter.fromExcluded(excludedDependencyTypes);
    }

    public PearCliDetectableOptions createPearCliDetectableOptions() {
        EnumListFilter<PearDependencyType> pearDependencyTypeFilter;
        Set<PearDependencyType> excludedDependencyTypes = detectConfiguration.getValue(DetectProperties.DETECT_PEAR_DEPENDENCY_TYPES_EXCLUDED).representedValueSet();
        pearDependencyTypeFilter = EnumListFilter.fromExcluded(excludedDependencyTypes);
        return new PearCliDetectableOptions(pearDependencyTypeFilter);
    }

    public CargoDetectableOptions createCargoDetectableOptions() {
        Set<CargoDependencyType> excludedDependencyTypes = detectConfiguration.getValue(DetectProperties.DETECT_CARGO_DEPENDENCY_TYPES_EXCLUDED).representedValueSet();
        EnumListFilter<CargoDependencyType> dependencyTypeFilter = EnumListFilter.fromExcluded(excludedDependencyTypes);
        return new CargoDetectableOptions(dependencyTypeFilter);
    }

    public PipenvDetectableOptions createPipenvDetectableOptions() {
        String pipProjectName = detectConfiguration.getNullableValue(DetectProperties.DETECT_PIP_PROJECT_NAME);
        String pipProjectVersionName = detectConfiguration.getNullableValue(DetectProperties.DETECT_PIP_PROJECT_VERSION_NAME);
        Boolean pipProjectTreeOnly = detectConfiguration.getValue(DetectProperties.DETECT_PIP_ONLY_PROJECT_TREE);
        return new PipenvDetectableOptions(pipProjectName, pipProjectVersionName, pipProjectTreeOnly);
    }

    public PipfileLockDetectableOptions createPipfileLockDetectableOptions() {
        Set<PipenvDependencyType> excludedDependencyTypes = detectConfiguration.getValue(DetectProperties.DETECT_PIPFILE_DEPENDENCY_TYPES_EXCLUDED).representedValueSet();
        EnumListFilter<PipenvDependencyType> dependencyTypeFilter = EnumListFilter.fromExcluded(excludedDependencyTypes);
        return new PipfileLockDetectableOptions(dependencyTypeFilter);
    }

    public PipInspectorDetectableOptions createPipInspectorDetectableOptions() {
        String pipProjectName = detectConfiguration.getNullableValue(DetectProperties.DETECT_PIP_PROJECT_NAME);
        List<Path> requirementsFilePath = detectConfiguration.getPaths(DetectProperties.DETECT_PIP_REQUIREMENTS_PATH);
        return new PipInspectorDetectableOptions(pipProjectName, requirementsFilePath);
    }

    public RequirementsFileDetectableOptions createRequirementsFileDetectableOptions() {
        String pipProjectName = detectConfiguration.getNullableValue(DetectProperties.DETECT_PIP_PROJECT_NAME);
        List<Path> requirementsFilePath = detectConfiguration.getPaths(DetectProperties.DETECT_PIP_REQUIREMENTS_PATH);
        return new RequirementsFileDetectableOptions(pipProjectName, requirementsFilePath);
    }

    public PnpmLockOptions createPnpmLockOptions() {
        Set<PnpmDependencyType> excludedDependencyTypes = detectConfiguration.getValue(DetectProperties.DETECT_PNPM_DEPENDENCY_TYPES_EXCLUDED).representedValueSet();
        EnumListFilter<PnpmDependencyType> dependencyTypeFilter = EnumListFilter.fromExcluded(excludedDependencyTypes);
        
        List<String> excludedDirectories = detectConfiguration.getValue(DetectProperties.DETECT_PNPM_EXCLUDED_DIRECTORIES);
        List<String> includedDirectories = detectConfiguration.getValue(DetectProperties.DETECT_PNPM_INCLUDED_DIRECTORIES);
        
        return new PnpmLockOptions(dependencyTypeFilter, excludedDirectories, includedDirectories);
    }

    public PoetryOptions createPoetryOptions() {
        List<String> excludedGroups = detectConfiguration.getValue(DetectProperties.DETECT_POETRY_DEPENDENCY_GROUPS_EXCLUDED);
        return new PoetryOptions(excludedGroups);
    }

    public ProjectInspectorOptions createProjectInspectorOptions() {
        String globalArguments = detectConfiguration.getNullableValue(DetectProperties.PROJECT_INSPECTOR_GLOBAL_ARGUMENTS);
        String additionalArguments = detectConfiguration.getNullableValue(DetectProperties.PROJECT_INSPECTOR_ARGUMENTS);
        Path projectInspectorZipPath = detectConfiguration.getPathOrNull(DetectProperties.DETECT_PROJECT_INSPECTOR_PATH);
        return new ProjectInspectorOptions(projectInspectorZipPath, additionalArguments, globalArguments);
    }

    public GemspecParseDetectableOptions createGemspecParseDetectableOptions() {
        Set<GemspecDependencyType> excludedDependencyTypes = detectConfiguration.getValue(DetectProperties.DETECT_RUBY_DEPENDENCY_TYPES_EXCLUDED).representedValueSet();
        EnumListFilter<GemspecDependencyType> dependencyTypeFilter = EnumListFilter.fromExcluded(excludedDependencyTypes);
        return new GemspecParseDetectableOptions(dependencyTypeFilter);
    }

    public SbtDetectableOptions createSbtDetectableOptions() {
        String sbtCommandAdditionalArguments = detectConfiguration.getNullableValue(DetectProperties.DETECT_SBT_ARGUMENTS);
        return new SbtDetectableOptions(sbtCommandAdditionalArguments);
    }

    public YarnLockOptions createYarnLockOptions() {
        Set<YarnDependencyType> excludedDependencyTypes = detectConfiguration.getValue(DetectProperties.DETECT_YARN_DEPENDENCY_TYPES_EXCLUDED).representedValueSet();
        Boolean yarnIgnoreAllWorkspacesMode = detectConfiguration.getValue(DetectProperties.DETECT_YARN_IGNORE_ALL_WORKSPACES_MODE);
        EnumListFilter<YarnDependencyType> yarnDependencyTypeFilter = EnumListFilter.fromExcluded(excludedDependencyTypes);

        List<String> excludedWorkspaces = detectConfiguration.getValue(DetectProperties.DETECT_YARN_EXCLUDED_WORKSPACES);
        List<String> includedWorkspaces = detectConfiguration.getValue(DetectProperties.DETECT_YARN_INCLUDED_WORKSPACES);
        return new YarnLockOptions(yarnDependencyTypeFilter, excludedWorkspaces, includedWorkspaces, yarnIgnoreAllWorkspacesMode);
    }

    public NugetInspectorOptions createNugetInspectorOptions() {
        Boolean ignoreFailures = detectConfiguration.getValue(DetectProperties.DETECT_NUGET_IGNORE_FAILURE);
        List<String> excludedModules = detectConfiguration.getValue(DetectProperties.DETECT_NUGET_EXCLUDED_MODULES);
        List<String> includedModules = detectConfiguration.getValue(DetectProperties.DETECT_NUGET_INCLUDED_MODULES);
        List<String> packagesRepoUrl = detectConfiguration.getValue(DetectProperties.DETECT_NUGET_PACKAGES_REPO_URL);
        Path nugetConfigPath = detectConfiguration.getPathOrNull(DetectProperties.DETECT_NUGET_CONFIG_PATH);
        Set<NugetDependencyType> nugetExcludedDependencyTypes = detectConfiguration.getValue(DetectProperties.DETECT_NUGET_DEPENDENCY_TYPES_EXCLUDED).representedValueSet();
        Path nugetArtifactsPath = detectConfiguration.getPathOrNull(DetectProperties.DETECT_NUGET_ARTIFACTS_PATH);
        return new NugetInspectorOptions(ignoreFailures, excludedModules, includedModules, packagesRepoUrl, nugetConfigPath, nugetExcludedDependencyTypes, nugetArtifactsPath);
    }

    private boolean getFollowSymLinks() {
        return detectConfiguration.getValue(DetectProperties.DETECT_FOLLOW_SYMLINKS);
    }

    public UVDetectorOptions createUVDetectorOptions() {
        List<String> excludedDependencyGroups = detectConfiguration.getValue(DetectProperties.DETECT_UV_DEPENDENCY_GROUPS_EXCLUDED);
        List<String> includedWorkSpaceMembers = detectConfiguration.getValue(DetectProperties.DETECT_UV_INCLUDED_WORKSPACE_MEMBERS);
        List<String> excludeWorkSpaceMembers = detectConfiguration.getValue(DetectProperties.DETECT_UV_EXCLUDED_WORKSPACE_MEMBERS);

        return new UVDetectorOptions(excludedDependencyGroups, includedWorkSpaceMembers, excludeWorkSpaceMembers);
    }
}
