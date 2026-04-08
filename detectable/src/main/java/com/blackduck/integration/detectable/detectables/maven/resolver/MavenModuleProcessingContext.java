package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.JavaRepository;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
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
class MavenModuleProcessingContext {

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
    private final MavenResolverOptions mavenResolverOptions;

    // Shaded dependency support
    private final ExternalIdFactory externalIdFactory;
    private final Path downloadDir;
    private final List<JavaRepository> rootRepositories;

    // Shared mutable state (modified during processing)
    private final List<CodeLocation> codeLocations;
    private final Set<String> visitedModulePomPaths;

    // Shaded sub-tree resolution cache: keyed on "groupId:artifactId:version",
    // shared across all modules to avoid re-resolving the same GAV.
    private final Map<String, DependencyGraph> shadedSubTreeCache;

    // Maven scope constants
    private final String compileScope;
    private final String testScope;

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
        MavenResolverOptions mavenResolverOptions,
        ExternalIdFactory externalIdFactory,
        Path downloadDir,
        List<JavaRepository> rootRepositories,
        List<CodeLocation> codeLocations,
        Set<String> visitedModulePomPaths,
        String compileScope,
        String testScope,
        Map<String, DependencyGraph> shadedSubTreeCache
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
        this.mavenResolverOptions = mavenResolverOptions;
        this.externalIdFactory = externalIdFactory;
        this.downloadDir = downloadDir;
        this.rootRepositories = rootRepositories;
        this.codeLocations = codeLocations;
        this.visitedModulePomPaths = visitedModulePomPaths;
        this.compileScope = compileScope;
        this.testScope = testScope;
        this.shadedSubTreeCache = shadedSubTreeCache;
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
    public MavenResolverOptions getMavenResolverOptions() { return mavenResolverOptions; }
    public ExternalIdFactory getExternalIdFactory() { return externalIdFactory; }
    public Path getDownloadDir() { return downloadDir; }
    public List<JavaRepository> getRootRepositories() { return rootRepositories; }
    public List<CodeLocation> getCodeLocations() { return codeLocations; }
    public Set<String> getVisitedModulePomPaths() { return visitedModulePomPaths; }
    public Map<String, DependencyGraph> getShadedSubTreeCache() { return shadedSubTreeCache; }
    public String getCompileScope() { return compileScope; }
    public String getTestScope() { return testScope; }

    /**
     * Builder for creating MavenModuleProcessingContext instances.
     */
    static class Builder {
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
        private MavenResolverOptions mavenResolverOptions;
        private ExternalIdFactory externalIdFactory;
        private Path downloadDir;
        private List<JavaRepository> rootRepositories;
        private List<CodeLocation> codeLocations;
        private String compileScope = "compile";
        private String testScope = "test";

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

        public Builder mavenResolverOptions(MavenResolverOptions mavenResolverOptions) {
            this.mavenResolverOptions = mavenResolverOptions;
            return this;
        }

        public Builder externalIdFactory(ExternalIdFactory externalIdFactory) {
            this.externalIdFactory = externalIdFactory;
            return this;
        }

        public Builder downloadDir(Path downloadDir) {
            this.downloadDir = downloadDir;
            return this;
        }

        public Builder rootRepositories(List<JavaRepository> rootRepositories) {
            this.rootRepositories = rootRepositories;
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

        public MavenModuleProcessingContext build() {
            // Initialize visitedModulePomPaths if needed
            Set<String> visitedPaths = new HashSet<>();

            // Initialize rootRepositories if not set
            List<JavaRepository> repos = rootRepositories != null ? rootRepositories : new ArrayList<>();

            // Initialize shaded sub-tree cache (shared across all module recursive calls)
            Map<String, DependencyGraph> cache = new HashMap<>();

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
                mavenResolverOptions,
                externalIdFactory,
                downloadDir,
                repos,
                codeLocations,
                visitedPaths,
                compileScope,
                testScope,
                cache
            );
        }
    }
}

