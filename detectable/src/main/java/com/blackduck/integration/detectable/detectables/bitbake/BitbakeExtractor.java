package com.blackduck.integration.detectable.detectables.bitbake;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.bitbake.collect.BitbakeCommandRunner;
import com.blackduck.integration.detectable.detectables.bitbake.collect.BuildFileFinder;
import com.blackduck.integration.detectable.detectables.bitbake.data.BitbakeEnvironment;
import com.blackduck.integration.detectable.detectables.bitbake.data.ShowRecipesResults;
import com.blackduck.integration.detectable.detectables.bitbake.model.BitbakeGraph;
import com.blackduck.integration.detectable.detectables.bitbake.parse.BitbakeEnvironmentParser;
import com.blackduck.integration.detectable.detectables.bitbake.parse.BitbakeRecipesParser;
import com.blackduck.integration.detectable.detectables.bitbake.parse.LicenseManifestParser;
import com.blackduck.integration.detectable.detectables.bitbake.parse.PwdOutputParser;
import com.blackduck.integration.detectable.detectables.bitbake.transform.BitbakeDependencyGraphTransformer;
import com.blackduck.integration.detectable.detectables.bitbake.transform.BitbakeGraphTransformer;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.util.ToolVersionLogger;
import com.blackduck.integration.exception.IntegrationException;

public class BitbakeExtractor {
    public static final String DEFAULT_BUILD_DIR_NAME = "build";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ToolVersionLogger toolVersionLogger;
    private final BitbakeCommandRunner bitbakeCommandRunner;
    private final BuildFileFinder buildFileFinder;
    private final PwdOutputParser pwdOutputParser;
    private final BitbakeEnvironmentParser bitbakeEnvironmentParser;
    private final BitbakeRecipesParser bitbakeRecipesParser;
    private final LicenseManifestParser licenseManifestParser;
    private final BitbakeGraphTransformer bitbakeGraphTransformer;
    private final BitbakeDependencyGraphTransformer bitbakeDependencyGraphTransformer;
    private final List<String> packageNames;
    private final EnumListFilter<BitbakeDependencyType> dependencyTypeFilter;

    public BitbakeExtractor(
        ToolVersionLogger toolVersionLogger,
        BitbakeCommandRunner bitbakeCommandRunner,
        BuildFileFinder buildFileFinder,
        PwdOutputParser pwdOutputParser,
        BitbakeEnvironmentParser bitbakeEnvironmentParser,
        BitbakeRecipesParser bitbakeRecipesParser,
        LicenseManifestParser licenseManifestParser,
        BitbakeGraphTransformer bitbakeGraphTransformer,
        BitbakeDependencyGraphTransformer bitbakeDependencyGraphTransformer,
        List<String> packageNames,
        EnumListFilter<BitbakeDependencyType> dependencyTypeFilter
    ) {
        this.toolVersionLogger = toolVersionLogger;
        this.bitbakeCommandRunner = bitbakeCommandRunner;
        this.buildFileFinder = buildFileFinder;
        this.pwdOutputParser = pwdOutputParser;
        this.bitbakeEnvironmentParser = bitbakeEnvironmentParser;
        this.bitbakeRecipesParser = bitbakeRecipesParser;
        this.licenseManifestParser = licenseManifestParser;
        this.bitbakeGraphTransformer = bitbakeGraphTransformer;
        this.bitbakeDependencyGraphTransformer = bitbakeDependencyGraphTransformer;
        this.packageNames = packageNames;
        this.dependencyTypeFilter = dependencyTypeFilter;
    }

    public Extraction extract(File sourceDirectory, ExecutableTarget bashExecutable, File buildEnvironmentFile) throws ExecutableFailedException, IOException {
        toolVersionLogger.log(() -> bitbakeCommandRunner.runBitbakeVersion(sourceDirectory, bashExecutable, buildEnvironmentFile));

        File buildDirectory = determineBuildDirectory(sourceDirectory, bashExecutable, buildEnvironmentFile);
        BitbakeEnvironment bitbakeEnvironment = executeBitbakeForEnvironment(sourceDirectory, bashExecutable, buildEnvironmentFile);
        ShowRecipesResults showRecipesResults = executeBitbakeForRecipeLayerCatalog(sourceDirectory, bashExecutable, buildEnvironmentFile);

        List<CodeLocation> codeLocations = packageNames.stream()
            .map(targetPackage -> generateCodeLocationForTargetPackage(
                targetPackage,
                sourceDirectory,
                bashExecutable,
                buildEnvironmentFile,
                buildDirectory,
                showRecipesResults,
                bitbakeEnvironment
            ))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        if (codeLocations.isEmpty()) {
            return Extraction.failure("No Code Locations were generated during extraction");
        } else {
            return Extraction.success(codeLocations);
        }
    }

    private Optional<CodeLocation> generateCodeLocationForTargetPackage(
        String targetPackage,
        File sourceDirectory,
        ExecutableTarget bashExecutable,
        File buildEnvironmentFile,
        File buildDirectory,
        ShowRecipesResults showRecipesResults,
        BitbakeEnvironment bitbakeEnvironment
    ) {
        try {
            BitbakeGraph bitbakeGraph = generateBitbakeGraph(
                sourceDirectory,
                bashExecutable,
                buildEnvironmentFile,
                buildDirectory,
                targetPackage,
                showRecipesResults.getLayerNames()
            );
            Map<String, String> imageRecipes = null;
            if (dependencyTypeFilter.shouldExclude(BitbakeDependencyType.BUILD)) {
                imageRecipes = readImageRecipes(buildDirectory, targetPackage, bitbakeEnvironment);
            }
            DependencyGraph dependencyGraph = bitbakeDependencyGraphTransformer.transform(bitbakeGraph, showRecipesResults.getRecipesWithLayers(), imageRecipes);
            return Optional.of(new CodeLocation(dependencyGraph));
        } catch (ExecutableFailedException | IntegrationException | IOException e) {
            logger.error("Failed to extract a Code Location while running Bitbake against package '{}': {}", targetPackage, e.getMessage());
            logger.debug(e.getMessage(), e);
            return Optional.empty();
        }
    }

    private File determineBuildDirectory(File sourceDirectory, ExecutableTarget bashExecutable, File buildEnvironmentFile) {
        File fallbackBuildDir = new File(sourceDirectory, DEFAULT_BUILD_DIR_NAME);
        File derivedBuildDirectory;
        try {
            List<String> pwdOutput = bitbakeCommandRunner.runPwdCommand(sourceDirectory, bashExecutable, buildEnvironmentFile);
            derivedBuildDirectory = pwdOutputParser.deriveBuildDirectory(pwdOutput);
        } catch (ExecutableFailedException | IOException e) {

            logger.warn("Unable to determine build directory location due to error: {}; ; using {} for build dir", e.getMessage(), fallbackBuildDir.getAbsolutePath());
            return fallbackBuildDir;
        }

        if (derivedBuildDirectory.isDirectory()) {
            logger.debug("Derived build dir: {}", derivedBuildDirectory.getAbsolutePath());
            return derivedBuildDirectory;
        } else {
            logger.warn("Derived build dir {} is not a directory; using {} for build dir", derivedBuildDirectory.getAbsolutePath(), fallbackBuildDir.getAbsolutePath());
            return fallbackBuildDir;
        }
    }

    private BitbakeEnvironment executeBitbakeForEnvironment(File sourceDirectory, ExecutableTarget bashExecutable, File buildEnvironmentFile) {
        try {
            List<String> bitbakeEnvironmentOutput = bitbakeCommandRunner.runBitbakeEnvironment(sourceDirectory, bashExecutable, buildEnvironmentFile);
            return bitbakeEnvironmentParser.parse(bitbakeEnvironmentOutput);
        } catch (ExecutableFailedException | IOException e) {
            logger.warn("Unable to get bitbake environment due to error executing {}: {}", "bitbake --environment", e.getMessage());
            return new BitbakeEnvironment(null, null, null);
        }
    }

    private ShowRecipesResults executeBitbakeForRecipeLayerCatalog(File sourceDirectory, ExecutableTarget bashExecutable, File buildEnvironmentFile)
        throws ExecutableFailedException, IOException {
        List<String> showRecipesOutput = bitbakeCommandRunner.runBitbakeLayersShowRecipes(sourceDirectory, bashExecutable, buildEnvironmentFile);
        return bitbakeRecipesParser.parseShowRecipes(showRecipesOutput);
    }

    private Map<String, String> readImageRecipes(File buildDirectory, String targetImageName, BitbakeEnvironment bitbakeEnvironment) throws IntegrationException, IOException {
        Optional<File> licenseManifestFile = buildFileFinder.findLicenseManifestFile(buildDirectory, targetImageName, bitbakeEnvironment);
        if (licenseManifestFile.isPresent()) {
            List<String> licenseManifestLines = FileUtils.readLines(licenseManifestFile.get(), StandardCharsets.UTF_8);
            return licenseManifestParser.collectImageRecipes(licenseManifestLines);
        } else {
            logger.info("No license.manifest file found for target image {}; every dependency will be considered a BUILD dependency.", targetImageName);
            return new HashMap<>(0);
        }
    }

    private BitbakeGraph generateBitbakeGraph(
        File sourceDirectory,
        ExecutableTarget bashExecutable,
        File buildEnvironmentFile,
        File buildDirectory,
        String targetPackage,
        Set<String> knownLayers
    ) throws IOException, IntegrationException, ExecutableFailedException {
        bitbakeCommandRunner.runBitbakeGraph(sourceDirectory, bashExecutable, buildEnvironmentFile, targetPackage);
        File taskDependsFile = buildFileFinder.findTaskDependsFile(sourceDirectory, buildDirectory);
        if (logger.isTraceEnabled()) {
            logger.trace(FileUtils.readFileToString(taskDependsFile, Charset.defaultCharset()));
        }
        MutableGraph mutableGraph = new Parser().read(taskDependsFile);
        return bitbakeGraphTransformer.transform(mutableGraph, knownLayers);
    }
}
