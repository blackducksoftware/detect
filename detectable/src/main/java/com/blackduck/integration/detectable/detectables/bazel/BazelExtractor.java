package com.blackduck.integration.detectable.detectables.bazel;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.Pipelines;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.DependencySourceChooser;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelVariableSubstitutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.HaskellCabalLibraryJsonProtoParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.util.ToolVersionLogger;
import com.blackduck.integration.detectable.detectables.bazel.v2.BazelEnvironmentAnalyzer;

public class BazelExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DetectableExecutableRunner executableRunner;
    private final ExternalIdFactory externalIdFactory;
    private final BazelWorkspaceFileParser bazelWorkspaceFileParser;
    private final DependencySourceChooser dependencySourceChooser;
    private final ToolVersionLogger toolVersionLogger;
    private final HaskellCabalLibraryJsonProtoParser haskellCabalLibraryJsonProtoParser;
    private final String bazelTarget;
    private final Set<DependencySource> dependencySourcesFromProperty;
    private final BazelVariableSubstitutor bazelVariableSubstitutor;
    private final BazelProjectNameGenerator bazelProjectNameGenerator;

    public BazelExtractor(
        DetectableExecutableRunner executableRunner,
        ExternalIdFactory externalIdFactory,
        BazelWorkspaceFileParser bazelWorkspaceFileParser,
        DependencySourceChooser dependencySourceChooser,
        ToolVersionLogger toolVersionLogger,
        HaskellCabalLibraryJsonProtoParser haskellCabalLibraryJsonProtoParser,
        String bazelTarget,
        Set<DependencySource> dependencySourcesFromProperty,
        BazelVariableSubstitutor bazelVariableSubstitutor,
        BazelProjectNameGenerator bazelProjectNameGenerator
    ) {
        this.executableRunner = executableRunner;
        this.externalIdFactory = externalIdFactory;
        this.dependencySourceChooser = dependencySourceChooser;
        this.bazelWorkspaceFileParser = bazelWorkspaceFileParser;
        this.toolVersionLogger = toolVersionLogger;
        this.haskellCabalLibraryJsonProtoParser = haskellCabalLibraryJsonProtoParser;
        this.bazelTarget = bazelTarget;
        this.dependencySourcesFromProperty = dependencySourcesFromProperty;
        this.bazelVariableSubstitutor = bazelVariableSubstitutor;
        this.bazelProjectNameGenerator = bazelProjectNameGenerator;
    }

    public Extraction extract(ExecutableTarget bazelExe, File workspaceDir, File workspaceFile) throws ExecutableFailedException, DetectableException {
        toolVersionLogger.log(workspaceDir, bazelExe, "version");
        BazelCommandExecutor bazelCommandExecutor = new BazelCommandExecutor(executableRunner, workspaceDir, bazelExe);
        // Detect Bazel mode once and pass it to Pipelines for correct HTTP variant selection.
        BazelEnvironmentAnalyzer.Mode mode = new BazelEnvironmentAnalyzer(bazelCommandExecutor).getMode();
        Pipelines pipelines = new Pipelines(bazelCommandExecutor, bazelVariableSubstitutor, externalIdFactory, haskellCabalLibraryJsonProtoParser, mode);
        Set<DependencySource> dependencySourcesFromFile = parseDependencySourcesFromFile(workspaceFile);
        Set<DependencySource> dependencySourcesToQuery = dependencySourceChooser.choose(dependencySourcesFromFile, dependencySourcesFromProperty);
        CodeLocation codeLocation = generateCodelocation(pipelines, dependencySourcesToQuery);
        return buildResults(codeLocation, bazelProjectNameGenerator.generateFromBazelTarget(bazelTarget));
    }

    private Set<DependencySource> parseDependencySourcesFromFile(File workspaceFile) {
        List<String> workspaceFileLines;
        try {
            workspaceFileLines = FileUtils.readLines(workspaceFile, StandardCharsets.UTF_8);
            return bazelWorkspaceFileParser.parseDependencySources(workspaceFileLines);
        } catch (IOException e) {
            logger.warn("Unable to read WORKSPACE file {}: {}", workspaceFile.getAbsolutePath(), e.getMessage());
            return new HashSet<>(0);
        }
    }

    private Extraction buildResults(CodeLocation codeLocation, String projectName) {
        List<CodeLocation> codeLocations = Collections.singletonList(codeLocation);
        Extraction.Builder builder = new Extraction.Builder()
            .success(codeLocations)
            .projectName(projectName);
        return builder.build();
    }

    @NotNull
    private CodeLocation generateCodelocation(Pipelines pipelines, Set<DependencySource> dependencySources) throws DetectableException, ExecutableFailedException {
        List<Dependency> aggregatedDependencies = new ArrayList<>();
        // Make sure the order of processing deterministic
        List<DependencySource> sortedDependencySources = dependencySources.stream()
            .sorted(Comparator.naturalOrder())
            .collect(Collectors.toList());

        for (DependencySource dependencySource : sortedDependencySources) {
            logger.info("Running processing pipeline for dependency source {}", dependencySource);
            List<Dependency> ruleDependencies = pipelines.get(dependencySource).run();
            logger.info("Number of dependencies discovered for dependency source {}: {}", dependencySource, ruleDependencies.size());
            logger.debug("Dependencies discovered for dependency source {}: {}", dependencySource, ruleDependencies);
            aggregatedDependencies.addAll(ruleDependencies);
        }

        DependencyGraph dependencyGraph = new BasicDependencyGraph();
        dependencyGraph.addChildrenToRoot(aggregatedDependencies);
        return new CodeLocation(dependencyGraph);
    }
}
