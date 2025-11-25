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
            mavenProject.getDependencyManagement().forEach(dep ->
                logger.info("    - Managed Dependency: {}:{}:{} (Scope: {})", dep.getCoordinates().getGroupId(), dep.getCoordinates().getArtifactId(), dep.getCoordinates().getVersion(), dep.getScope())
            );

            // 2. Resolve dependencies using the Aether-based resolver for the root pom.
            MavenDependencyResolver dependencyResolver = new MavenDependencyResolver();
            Path localRepoPath = extractionEnvironment.getOutputDirectory().toPath().resolve("local-repo");
            CollectResult collectResult = dependencyResolver.resolveDependencies(pomFile, mavenProject, localRepoPath.toFile());

            // 3. Write the dependency tree to a file for inspection.
            File dependencyTreeFile = new File(extractionEnvironment.getOutputDirectory(), "dependency-tree.txt");
            try (PrintStream printStream = new PrintStream(dependencyTreeFile)) {
                collectResult.getRoot().accept(new DependencyGraphDumper(printStream::println));
            }
            logger.info("Dependency tree saved to: {}", dependencyTreeFile.getAbsolutePath());

            // 4. Transform the Aether graph to a Detect DependencyGraph
            MavenGraphParser mavenGraphParser = new MavenGraphParser();
            MavenParseResult parseResult = mavenGraphParser.parse(collectResult);

            MavenGraphTransformer mavenGraphTransformer = new MavenGraphTransformer(externalIdFactory);
            DependencyGraph dependencyGraph = mavenGraphTransformer.transform(parseResult);

            // Prepare code locations list. Create a CodeLocation for the root project first.
            List<CodeLocation> codeLocations = new ArrayList<>();
            try {
                com.blackduck.integration.bdio.model.externalid.ExternalId rootExternalId = externalIdFactory.createMavenExternalId(
                    mavenProject.getCoordinates().getGroupId(),
                    mavenProject.getCoordinates().getArtifactId(),
                    mavenProject.getCoordinates().getVersion()
                );
                File rootSourcePath = pomFile.getParentFile();
                codeLocations.add(new CodeLocation(dependencyGraph, rootExternalId, rootSourcePath));
            } catch (Exception e) {
                logger.debug("Failed to create root external id for code location: {}", e.getMessage());
                codeLocations.add(new CodeLocation(dependencyGraph));
            }

            // 4b. Process modules (if any) and create separate code locations for each module
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
                        CollectResult moduleCollectResult = dependencyResolver.resolveDependencies(modulePom, moduleProject, localRepoPath.toFile());
                        MavenParseResult moduleParseResult = mavenGraphParser.parse(moduleCollectResult);
                        DependencyGraph moduleGraph = mavenGraphTransformer.transform(moduleParseResult);

                        // Create a code location for the module and add to the list (do not merge into root)
                        try {
                            com.blackduck.integration.bdio.model.externalid.ExternalId moduleExternalId = externalIdFactory.createMavenExternalId(
                                moduleProject.getCoordinates().getGroupId(),
                                moduleProject.getCoordinates().getArtifactId(),
                                moduleProject.getCoordinates().getVersion()
                            );
                            File moduleSourcePath = modulePom.getParentFile();
                            codeLocations.add(new CodeLocation(moduleGraph, moduleExternalId, moduleSourcePath));
                        } catch (Exception e) {
                            logger.debug("Failed to create module external id for module '{}': {}", modulePathStr, e.getMessage());
                            codeLocations.add(new CodeLocation(moduleGraph));
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
