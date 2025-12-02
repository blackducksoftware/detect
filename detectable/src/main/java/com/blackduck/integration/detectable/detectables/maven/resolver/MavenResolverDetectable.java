package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.FileNotFoundDetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;
import com.blackduck.integration.detectable.detectables.maven.resolver.result.MavenParseResult;
import com.blackduck.integration.util.NameVersion;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@DetectableInfo(
        name = "Maven Resolver",
        language = "Java",
        forge = "Maven Central",
        accuracy = DetectableAccuracyType.HIGH,
        requirementsMarkdown = "File: pom.xml"
)
public class MavenResolverDetectable extends Detectable {

    private static final String POM_XML_FILENAME = "pom.xml";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final FileFinder fileFinder;
    private final ExternalIdFactory externalIdFactory;

    private File pomFile;

    public MavenResolverDetectable(
            DetectableEnvironment environment,
            FileFinder fileFinder,
            ExternalIdFactory externalIdFactory
    ) {
        super(environment);
        this.fileFinder = fileFinder;
        this.externalIdFactory = externalIdFactory;
    }

    @Override
    public DetectableResult applicable() {
        pomFile = fileFinder.findFile(environment.getDirectory(), POM_XML_FILENAME);
        if (pomFile == null) {
            return new FileNotFoundDetectableResult(POM_XML_FILENAME);
        }
        return new PassedDetectableResult();
    }

    @Override
    public DetectableResult extractable() {
        // For now, we are keeping this simple. We can add more checks later,
        // for example, to ensure network connectivity or resolver-specific prerequisites.
        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        try {
            // 1. Build the effective POM using the new ProjectBuilder.
            Path downloadDir = extractionEnvironment.getOutputDirectory().toPath().resolve("downloads");
            Files.createDirectories(downloadDir);
            ProjectBuilder projectBuilder = new ProjectBuilder(downloadDir);
            MavenProject mavenProject = projectBuilder.buildProject(pomFile);

            // Log the MavenProject details for verification.
            logger.info("Constructed MavenProject model for: {}", mavenProject.getPomFile());
            logger.info("  Coordinates: {}:{}:{}", mavenProject.getCoordinates().getGroupId(), mavenProject.getCoordinates().getArtifactId(), mavenProject.getCoordinates().getVersion());

            logger.info("  Dependencies found: {}", mavenProject.getDependencies().size());
            mavenProject.getDependencies().forEach(dep ->
                logger.info("    - Dependency: {}:{}:{} (Scope: {})", dep.getCoordinates().getGroupId(), dep.getCoordinates().getArtifactId(), dep.getCoordinates().getVersion(), dep.getScope())
            );

            logger.info("  Managed dependencies found: {}", mavenProject.getDependencyManagement().size());
//            mavenProject.getDependencyManagement().forEach(dep ->
//                logger.info("    - Managed Dependency: {}:{}:{} (Scope: {})", dep.getCoordinates().getGroupId(), dep.getCoordinates().getArtifactId(), dep.getCoordinates().getVersion(), dep.getScope())
//            );

            // 2. Resolve dependencies using the Aether-based resolver for the root pom.
            MavenDependencyResolver dependencyResolver = new MavenDependencyResolver();
            Path localRepoPath = extractionEnvironment.getOutputDirectory().toPath().resolve("local-repo");

            // TODO: expose a configuration flag `includeTestScope` later; for now we enable two-phase collection (compile + test)
            boolean includeTestScope = true; // TODO: make configurable

            // Perform compile-phase collection
            CollectResult collectResultCompile = dependencyResolver.resolveDependencies(pomFile, mavenProject, localRepoPath.toFile(), "compile");

            // Perform test-phase collection (only if enabled)
            CollectResult collectResultTest = null;
            if (includeTestScope) {
                collectResultTest = dependencyResolver.resolveDependencies(pomFile, mavenProject, localRepoPath.toFile(), "test");
            }

            // 3. Write the dependency trees to files for inspection.
            File dependencyTreeCompileFile = new File(extractionEnvironment.getOutputDirectory(), "dependency-tree-compile.txt");
            try (PrintStream printStream = new PrintStream(dependencyTreeCompileFile)) {
                collectResultCompile.getRoot().accept(new DependencyGraphDumper(printStream::println));
            }
            logger.info("Compile dependency tree saved to: {}", dependencyTreeCompileFile.getAbsolutePath());

            File dependencyTreeTestFile = null;
            if (includeTestScope && collectResultTest != null) {
                dependencyTreeTestFile = new File(extractionEnvironment.getOutputDirectory(), "dependency-tree-test.txt");
                try (PrintStream printStream = new PrintStream(dependencyTreeTestFile)) {
                    collectResultTest.getRoot().accept(new DependencyGraphDumper(printStream::println));
                }
                logger.info("Test dependency tree saved to: {}", dependencyTreeTestFile.getAbsolutePath());
            }

            // 4. Transform the Aether graphs to Detect DependencyGraphs
            MavenGraphParser mavenGraphParser = new MavenGraphParser();

            MavenParseResult parseResultCompile = mavenGraphParser.parse(collectResultCompile);
            MavenGraphTransformer mavenGraphTransformer = new MavenGraphTransformer(externalIdFactory);
            DependencyGraph dependencyGraphCompile = mavenGraphTransformer.transform(parseResultCompile);

            DependencyGraph dependencyGraphTest = null;
            if (includeTestScope && collectResultTest != null) {
                MavenParseResult parseResultTest = mavenGraphParser.parse(collectResultTest);
                dependencyGraphTest = mavenGraphTransformer.transform(parseResultTest);
            }

            // Prepare code locations list. Create a CodeLocation for the root project for both compile and test (if available).
            List<CodeLocation> codeLocations = new ArrayList<>();
            try {
                com.blackduck.integration.bdio.model.externalid.ExternalId rootExternalId = externalIdFactory.createMavenExternalId(
                    mavenProject.getCoordinates().getGroupId(),
                    mavenProject.getCoordinates().getArtifactId(),
                    mavenProject.getCoordinates().getVersion()
                );
                File rootSourcePath = pomFile.getParentFile();
                codeLocations.add(new CodeLocation(dependencyGraphCompile, rootExternalId, rootSourcePath));
                if (dependencyGraphTest != null) {
                    // Create a separate codelocation for test-scope
                    codeLocations.add(new CodeLocation(dependencyGraphTest, rootExternalId, rootSourcePath));
                }
            } catch (Exception e) {
                logger.debug("Failed to create root external id for code location: {}", e.getMessage());
                codeLocations.add(new CodeLocation(dependencyGraphCompile));
                if (dependencyGraphTest != null) {
                    codeLocations.add(new CodeLocation(dependencyGraphTest));
                }
            }

            // 4b. Process modules (if any) and create separate code locations for each module (compile + test)
            if (mavenProject.getModules() != null && !mavenProject.getModules().isEmpty()) {
                File rootDir = pomFile.getParentFile();
                for (String module : mavenProject.getModules()) {
                    try {
                        if (module == null || module.trim().isEmpty()) {
                            continue;
                        }
                        String modulePathStr = module.trim();
                        File modulePom = new File(rootDir, modulePathStr);
                        // If module points to a directory, append pom.xml
                        if (modulePom.isDirectory()) {
                            modulePom = new File(modulePom, POM_XML_FILENAME);
                        }
                        // If module string is not a path but a folder name, ensure we check common layout
                        if (!modulePom.exists()) {
                            File alternative = new File(rootDir, modulePathStr + File.separator + POM_XML_FILENAME);
                            if (alternative.exists()) {
                                modulePom = alternative;
                            }
                        }

                        if (!modulePom.exists()) {
                            logger.warn("Module POM not found for module '{}' at expected path '{}'. Skipping module.", modulePathStr, modulePom.getAbsolutePath());
                            continue;
                        }

                        logger.info("Processing module POM: {}", modulePom.getAbsolutePath());
                        MavenProject moduleProject = projectBuilder.buildProject(modulePom);

                        // two-phase for module
                        CollectResult moduleCollectCompile = dependencyResolver.resolveDependencies(modulePom, moduleProject, localRepoPath.toFile(), "compile");
                        CollectResult moduleCollectTest = null;
                        if (includeTestScope) {
                            moduleCollectTest = dependencyResolver.resolveDependencies(modulePom, moduleProject, localRepoPath.toFile(), "test");
                        }

                        MavenParseResult moduleParseCompile = mavenGraphParser.parse(moduleCollectCompile);
                        DependencyGraph moduleGraphCompile = mavenGraphTransformer.transform(moduleParseCompile);

                        DependencyGraph moduleGraphTest = null;
                        if (includeTestScope && moduleCollectTest != null) {
                            MavenParseResult moduleParseTest = mavenGraphParser.parse(moduleCollectTest);
                            moduleGraphTest = mavenGraphTransformer.transform(moduleParseTest);
                        }

                        // Create a code location for the module and add to the list (do not merge into root)
                        try {
                            com.blackduck.integration.bdio.model.externalid.ExternalId moduleExternalId = externalIdFactory.createMavenExternalId(
                                moduleProject.getCoordinates().getGroupId(),
                                moduleProject.getCoordinates().getArtifactId(),
                                moduleProject.getCoordinates().getVersion()
                            );
                            File moduleSourcePath = modulePom.getParentFile();
                            codeLocations.add(new CodeLocation(moduleGraphCompile, moduleExternalId, moduleSourcePath));
                            if (moduleGraphTest != null) {
                                codeLocations.add(new CodeLocation(moduleGraphTest, moduleExternalId, moduleSourcePath));
                            }
                        } catch (Exception e) {
                            logger.debug("Failed to create module external id for module '{}': {}", modulePathStr, e.getMessage());
                            codeLocations.add(new CodeLocation(moduleGraphCompile));
                            if (moduleGraphTest != null) {
                                codeLocations.add(new CodeLocation(moduleGraphTest));
                            }
                        }

                    } catch (Exception e) {
                        logger.warn("Failed processing module '{}': {}", module, e.getMessage());
                    }
                }
            }

            // 5. Return all code locations (root + modules)
            String projectName = mavenProject.getCoordinates().getGroupId() + ":" + mavenProject.getCoordinates().getArtifactId();
            NameVersion nameVersion = new NameVersion(projectName, mavenProject.getCoordinates().getVersion());

            return new Extraction.Builder().success(codeLocations).nameVersion(nameVersion).build();

        } catch (Exception e) {
            logger.error("Failed to resolve dependencies for pom.xml: {}", e.getMessage());
            return new Extraction.Builder().exception(e).build();
        }
    }
}
