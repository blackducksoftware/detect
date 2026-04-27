package com.blackduck.integration.detectable.detectables.maven.resolver.module;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.maven.resolver.MavenResolverOptions;
import com.blackduck.integration.detectable.detectables.maven.resolver.ShadedDependencyScanner;
import com.blackduck.integration.detectable.detectables.maven.resolver.graph.MavenGraphParser;
import com.blackduck.integration.detectable.detectables.maven.resolver.graph.MavenGraphTransformer;
import com.blackduck.integration.detectable.detectables.maven.resolver.output.CodeLocationFactory;
import com.blackduck.integration.detectable.detectables.maven.resolver.output.DependencyTreeFileWriter;
import com.blackduck.integration.detectable.detectables.maven.resolver.output.MavenCoordinateFormatter;
import com.blackduck.integration.detectable.detectables.maven.resolver.pom.ProjectBuilder;
import com.blackduck.integration.detectable.detectables.maven.resolver.resolution.MavenDependencyResolver;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Context object encapsulating all state and dependencies needed for Maven module processing.
 *
 * <p>This context object groups together the various components and state needed to process
 * multi-module Maven projects, reducing parameter count and making the processing logic cleaner.
 *
 * <p><strong>Design Pattern:</strong> Context Object pattern - reduces parameter proliferation
 * by bundling related parameters into a cohesive object.
 *
 * <p><strong>Thread Safety:</strong> This class is NOT thread-safe. The mutable collections
 * (codeLocations, visitedModulePomPaths) are shared across recursive calls.
 */
public class MavenModuleProcessingContext {

    // Builders and resolvers
    private final ProjectBuilder projectBuilder;
    private final MavenDependencyResolver dependencyResolver;
    private final MavenGraphParser graphParser;
    private final MavenGraphTransformer graphTransformer;

    // Helper utilities
    private final DependencyTreeFileWriter treeWriter;
    private final CodeLocationFactory codeLocationFactory;
    private final MavenCoordinateFormatter coordinateFormatter;

    // Paths and directories
    private final Path localRepoPath;
    private final File outputDir;

    // Configuration
    private final boolean includeTestScope;

    // Shared mutable state (modified during processing)
    private final List<CodeLocation> codeLocations;
    private final Set<String> visitedModulePomPaths;

    // Maven scope constants
    private final String compileScope;
    private final String testScope;

    // Shaded dependency detection
    @Nullable
    private final ShadedDependencyScanner shadedDependencyScanner;
    private final Map<String, DependencyGraph> shadedSubTreeCache;
    private final boolean includeShadedDependencies;
    @Nullable
    private final MavenResolverOptions mavenResolverOptions;
    private final Path downloadDir;

    /**
     * Private constructor - use Builder to create instances.
     */
    private MavenModuleProcessingContext(
        ProjectBuilder projectBuilder,
        MavenDependencyResolver dependencyResolver,
        MavenGraphParser graphParser,
        MavenGraphTransformer graphTransformer,
        DependencyTreeFileWriter treeWriter,
        CodeLocationFactory codeLocationFactory,
        MavenCoordinateFormatter coordinateFormatter,
        Path localRepoPath,
        File outputDir,
        boolean includeTestScope,
        List<CodeLocation> codeLocations,
        Set<String> visitedModulePomPaths,
        String compileScope,
        String testScope,
        @Nullable ShadedDependencyScanner shadedDependencyScanner,
        Map<String, DependencyGraph> shadedSubTreeCache,
        boolean includeShadedDependencies,
        @Nullable MavenResolverOptions mavenResolverOptions,
        Path downloadDir
    ) {
        this.projectBuilder = projectBuilder;
        this.dependencyResolver = dependencyResolver;
        this.graphParser = graphParser;
        this.graphTransformer = graphTransformer;
        this.treeWriter = treeWriter;
        this.codeLocationFactory = codeLocationFactory;
        this.coordinateFormatter = coordinateFormatter;
        this.localRepoPath = localRepoPath;
        this.outputDir = outputDir;
        this.includeTestScope = includeTestScope;
        this.codeLocations = codeLocations;
        this.visitedModulePomPaths = visitedModulePomPaths;
        this.compileScope = compileScope;
        this.testScope = testScope;
        this.shadedDependencyScanner = shadedDependencyScanner;
        this.shadedSubTreeCache = shadedSubTreeCache;
        this.includeShadedDependencies = includeShadedDependencies;
        this.mavenResolverOptions = mavenResolverOptions;
        this.downloadDir = downloadDir;
    }

    // Getters
    public ProjectBuilder getProjectBuilder() { return projectBuilder; }
    public MavenDependencyResolver getDependencyResolver() { return dependencyResolver; }
    public MavenGraphParser getGraphParser() { return graphParser; }
    public MavenGraphTransformer getGraphTransformer() { return graphTransformer; }
    public DependencyTreeFileWriter getTreeWriter() { return treeWriter; }
    public CodeLocationFactory getCodeLocationFactory() { return codeLocationFactory; }
    public MavenCoordinateFormatter getCoordinateFormatter() { return coordinateFormatter; }
    public Path getLocalRepoPath() { return localRepoPath; }
    public File getOutputDir() { return outputDir; }
    public boolean isIncludeTestScope() { return includeTestScope; }
    public List<CodeLocation> getCodeLocations() { return codeLocations; }
    public Set<String> getVisitedModulePomPaths() { return visitedModulePomPaths; }
    public String getCompileScope() { return compileScope; }
    public String getTestScope() { return testScope; }
    @Nullable
    public ShadedDependencyScanner getShadedDependencyScanner() { return shadedDependencyScanner; }
    public Map<String, DependencyGraph> getShadedSubTreeCache() { return shadedSubTreeCache; }
    public boolean isIncludeShadedDependencies() { return includeShadedDependencies; }
    @Nullable
    public MavenResolverOptions getMavenResolverOptions() { return mavenResolverOptions; }
    public Path getDownloadDir() { return downloadDir; }

    /**
     * Builder for creating MavenModuleProcessingContext instances.
     */
    public static class Builder {
        private ProjectBuilder projectBuilder;
        private MavenDependencyResolver dependencyResolver;
        private MavenGraphParser graphParser;
        private MavenGraphTransformer graphTransformer;
        private DependencyTreeFileWriter treeWriter;
        private CodeLocationFactory codeLocationFactory;
        private MavenCoordinateFormatter coordinateFormatter;
        private Path localRepoPath;
        private File outputDir;
        private boolean includeTestScope = true;
        private List<CodeLocation> codeLocations;
        private String compileScope = "compile";
        private String testScope = "test";
        private ShadedDependencyScanner shadedDependencyScanner;
        private Map<String, DependencyGraph> shadedSubTreeCache;
        private boolean includeShadedDependencies;
        private MavenResolverOptions mavenResolverOptions;
        private Path downloadDir;

        public Builder projectBuilder(ProjectBuilder projectBuilder) {
            this.projectBuilder = projectBuilder;
            return this;
        }

        public Builder dependencyResolver(MavenDependencyResolver dependencyResolver) {
            this.dependencyResolver = dependencyResolver;
            return this;
        }

        public Builder graphParser(MavenGraphParser graphParser) {
            this.graphParser = graphParser;
            return this;
        }

        public Builder graphTransformer(MavenGraphTransformer graphTransformer) {
            this.graphTransformer = graphTransformer;
            return this;
        }

        public Builder treeWriter(DependencyTreeFileWriter treeWriter) {
            this.treeWriter = treeWriter;
            return this;
        }

        public Builder codeLocationFactory(CodeLocationFactory codeLocationFactory) {
            this.codeLocationFactory = codeLocationFactory;
            return this;
        }

        public Builder coordinateFormatter(MavenCoordinateFormatter coordinateFormatter) {
            this.coordinateFormatter = coordinateFormatter;
            return this;
        }

        public Builder localRepoPath(Path localRepoPath) {
            this.localRepoPath = localRepoPath;
            return this;
        }

        public Builder outputDir(File outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        public Builder includeTestScope(boolean includeTestScope) {
            this.includeTestScope = includeTestScope;
            return this;
        }

        public Builder codeLocations(List<CodeLocation> codeLocations) {
            this.codeLocations = codeLocations;
            return this;
        }

        public Builder compileScope(String compileScope) {
            this.compileScope = compileScope;
            return this;
        }

        public Builder testScope(String testScope) {
            this.testScope = testScope;
            return this;
        }

        public Builder shadedDependencyScanner(ShadedDependencyScanner shadedDependencyScanner) {
            this.shadedDependencyScanner = shadedDependencyScanner;
            return this;
        }

        public Builder shadedSubTreeCache(Map<String, DependencyGraph> shadedSubTreeCache) {
            this.shadedSubTreeCache = shadedSubTreeCache;
            return this;
        }

        public Builder includeShadedDependencies(boolean includeShadedDependencies) {
            this.includeShadedDependencies = includeShadedDependencies;
            return this;
        }

        public Builder mavenResolverOptions(MavenResolverOptions mavenResolverOptions) {
            this.mavenResolverOptions = mavenResolverOptions;
            return this;
        }

        public Builder downloadDir(Path downloadDir) {
            this.downloadDir = downloadDir;
            return this;
        }

        public MavenModuleProcessingContext build() {
            Set<String> visitedPaths = new HashSet<>();

            return new MavenModuleProcessingContext(
                projectBuilder,
                dependencyResolver,
                graphParser,
                graphTransformer,
                treeWriter,
                codeLocationFactory,
                coordinateFormatter,
                localRepoPath,
                outputDir,
                includeTestScope,
                codeLocations,
                visitedPaths,
                compileScope,
                testScope,
                shadedDependencyScanner,
                shadedSubTreeCache != null ? shadedSubTreeCache : new HashMap<>(),
                includeShadedDependencies,
                mavenResolverOptions,
                downloadDir
            );
        }
    }
}

