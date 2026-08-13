package com.blackduck.integration.detectable.detectables.docker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.blackduck.integration.bdio.BdioReader;
import com.blackduck.integration.bdio.BdioTransformer;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.BdioId;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.SimpleBdioDocument;
import com.blackduck.integration.bdio.model.dependency.ProjectDependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectables.docker.model.DockerInspectorResults;
import com.blackduck.integration.detectable.detectables.docker.parser.DockerInspectorResultsFileParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionMetadata;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.executable.Executable;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.executable.ExecutableRunnerException;

public class DockerExtractor {
    public static final ExtractionMetadata<File> DOCKER_TAR_META_DATA = new ExtractionMetadata<>("dockerTar", File.class);
    public static final ExtractionMetadata<String> DOCKER_IMAGE_NAME_META_DATA = new ExtractionMetadata<>("dockerImage", String.class);
    public static final ExtractionMetadata<File> SQUASHED_IMAGE_META_DATA = new ExtractionMetadata<>("squashedImage", File.class);
    public static final ExtractionMetadata<File> CONTAINER_FILESYSTEM_META_DATA = new ExtractionMetadata<>("containerFilesystem", File.class);

    public static final String CONTAINER_FILESYSTEM_FILENAME_PATTERN = "*_containerfilesystem.tar.gz";
    public static final String SQUASHED_IMAGE_FILENAME_PATTERN = "*_squashedimage.tar.gz";
    public static final String RESULTS_FILENAME_PATTERN = "results.json";
    public static final String DEPENDENCIES_PATTERN = "*bdio.jsonld";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final FileFinder fileFinder;
    private final DetectableExecutableRunner executableRunner;
    private final BdioTransformer bdioTransformer;
    private final ExternalIdFactory externalIdFactory;
    private final Gson gson;
    private final DockerInspectorResultsFileParser dockerInspectorResultsFileParser;
    private final ImageIdentifierGenerator imageIdentifierGenerator;

    public DockerExtractor(
        FileFinder fileFinder,
        DetectableExecutableRunner executableRunner,
        BdioTransformer bdioTransformer,
        ExternalIdFactory externalIdFactory,
        Gson gson,
        DockerInspectorResultsFileParser dockerInspectorResultsFileParser,
        ImageIdentifierGenerator imageIdentifierGenerator
    ) {
        this.fileFinder = fileFinder;
        this.executableRunner = executableRunner;
        this.bdioTransformer = bdioTransformer;
        this.externalIdFactory = externalIdFactory;
        this.gson = gson;
        this.dockerInspectorResultsFileParser = dockerInspectorResultsFileParser;
        this.imageIdentifierGenerator = imageIdentifierGenerator;
    }

    public Extraction extract(
        File directory,
        File outputDirectory,
        ExecutableTarget dockerExe,
        ExecutableTarget javaExe,
        String image,
        String imageId,
        String tar,
        DockerInspectorInfo dockerInspectorInfo,
        DockerProperties dockerProperties
    ) throws IOException, ExecutableRunnerException {
        // Refuse to forward image identifiers that contain SpEL templates ("#{...}"). These values are
        // passed to Docker Inspector on the fork command line as --docker.image / --docker.tar /
        // --docker.image.id and are bound to @Value("${...}") fields in the inspector's Config.java,
        // which triggers SpEL evaluation during Spring context refresh. See CVE-2026-41849.
        try {
            SpelInjectionGuard.rejectIfContainsSpelTemplate("docker.image", image);
            SpelInjectionGuard.rejectIfContainsSpelTemplate("docker.tar", tar);
            SpelInjectionGuard.rejectIfContainsSpelTemplate("docker.image.id", imageId);
        } catch (IllegalArgumentException spelRejection) {
            return new Extraction.Builder().failure(spelRejection.getMessage()).build();
        }

        String imageArgument = null;
        String imagePiece = null;
        ImageIdentifierType imageIdentifierType = ImageIdentifierType.IMAGE_NAME;
        if (StringUtils.isNotBlank(tar)) {
            File dockerTarFile = new File(tar);
            imageArgument = String.format("--docker.tar=%s", dockerTarFile.getCanonicalPath());
            imagePiece = dockerTarFile.getName();
            imageIdentifierType = ImageIdentifierType.TAR;
        } else if (StringUtils.isNotBlank(image)) {
            imagePiece = image;
            imageArgument = String.format("--docker.image=%s", image);
            imageIdentifierType = ImageIdentifierType.IMAGE_NAME;
        } else if (StringUtils.isNotBlank(imageId)) {
            imagePiece = imageId;
            imageArgument = String.format("--docker.image.id=%s", imageId);
            imageIdentifierType = ImageIdentifierType.IMAGE_ID;
        }

        if (StringUtils.isBlank(imageArgument) || StringUtils.isBlank(imagePiece)) {
            return new Extraction.Builder().failure("No docker image found.").build();
        } else {
            return executeDocker(outputDirectory, imageIdentifierType, imageArgument, imagePiece, tar, directory, javaExe, dockerExe, dockerInspectorInfo, dockerProperties);
        }
    }

    private void importTars(List<File> importTars, File directory, Map<String, String> environmentVariables, ExecutableTarget dockerExe) {
        try {
            for (File imageToImport : importTars) {
                loadDockerImage(directory, environmentVariables, dockerExe, imageToImport);
            }
        } catch (Exception e) {
            logger.debug(String.format("Exception encountered when resolving paths for docker air gap: %s", e.getMessage()));
            logger.debug("Running in online mode instead");
        }
    }

    private void loadDockerImage(File directory, Map<String, String> environmentVariables, ExecutableTarget dockerExe, File imageToImport)
        throws IOException, ExecutableRunnerException, IntegrationException {
        List<String> dockerImportArguments = Arrays.asList(
            "load",
            "-i",
            imageToImport.getCanonicalPath()
        );
        Executable dockerImportImageExecutable = ExecutableUtils.createFromTarget(directory, environmentVariables, dockerExe, dockerImportArguments);
        ExecutableOutput exeOut = executableRunner.execute(dockerImportImageExecutable);
        if (exeOut.getReturnCode() != 0) {
            throw new IntegrationException(String.format("Command %s %s returned %d: %s",
                dockerExe.toCommand(), dockerImportArguments,
                exeOut.getReturnCode(), exeOut.getErrorOutput()
            ));
        }
    }

    private Extraction executeDocker(
        File outputDirectory,
        ImageIdentifierType imageIdentifierType,
        String imageArgument,
        String suppliedImagePiece,
        String dockerTarFilePath,
        File directory,
        ExecutableTarget javaExe,
        ExecutableTarget dockerExe,
        DockerInspectorInfo dockerInspectorInfo,
        DockerProperties dockerProperties
    )
        throws IOException, ExecutableRunnerException {

        File dockerPropertiesFile = new File(outputDirectory, "application.properties");
        dockerProperties.populatePropertiesFile(dockerPropertiesFile, outputDirectory);
        Map<String, String> environmentVariables = new HashMap<>(0);
        List<String> dockerArguments = new ArrayList<>();
        dockerArguments.add("-jar");
        dockerArguments.add(dockerInspectorInfo.getDockerInspectorJar().getAbsolutePath());
        dockerArguments.add("--spring.config.location=file:" + dockerPropertiesFile.getCanonicalPath());
        dockerArguments.add(imageArgument);
        if (dockerInspectorInfo.hasAirGapImageFiles()) {
            importTars(dockerInspectorInfo.getAirGapInspectorImageTarFiles(), outputDirectory, environmentVariables, dockerExe);
        }
        Executable dockerExecutable = ExecutableUtils.createFromTarget(outputDirectory, environmentVariables, javaExe, dockerArguments);
        executableRunner.execute(dockerExecutable);
        Optional<DockerInspectorResults> dockerResults = Optional.empty();
        File producedResultFile = fileFinder.findFile(outputDirectory, RESULTS_FILENAME_PATTERN);
        if (producedResultFile != null) {
            String resultsFileContents = FileUtils.readFileToString(producedResultFile, StandardCharsets.UTF_8);
            dockerResults = dockerInspectorResultsFileParser.parse(resultsFileContents);
        }

        File producedSquashedImageFile = fileFinder.findFile(outputDirectory, SQUASHED_IMAGE_FILENAME_PATTERN);
        if (producedSquashedImageFile != null) {
            logger.debug("Returning squashed image: {}", producedSquashedImageFile.getAbsolutePath());
        }
        File producedContainerFileSystemFile = fileFinder.findFile(outputDirectory, CONTAINER_FILESYSTEM_FILENAME_PATTERN);
        if (producedContainerFileSystemFile != null) {
            logger.debug("Returning container filesystem: {}", producedContainerFileSystemFile.getAbsolutePath());
        }

        Extraction.Builder extractionBuilder = findCodeLocations(outputDirectory, directory, dockerResults.map(DockerInspectorResults::getMessage).orElse(null));
        String imageIdentifier = imageIdentifierGenerator.generate(imageIdentifierType, suppliedImagePiece, dockerResults.orElse(null));
        // The value of DOCKER_IMAGE_NAME_META_DATA is built into the codelocation name, so changing how its value is derived is likely to
        // change how codelocation names are generated. Currently either an image repo, repo:tag, or tarfile path gets written there.
        // It's tempting to always store the image repo:tag in that field, but that would change code location naming with consequences for users.
        extractionBuilder
            .metaData(SQUASHED_IMAGE_META_DATA, producedSquashedImageFile)
            .metaData(CONTAINER_FILESYSTEM_META_DATA, producedContainerFileSystemFile)
            .metaData(DOCKER_IMAGE_NAME_META_DATA, imageIdentifier);
        if (StringUtils.isNotBlank(dockerTarFilePath)) {
            File givenDockerTarfile = new File(dockerTarFilePath);
            logger.debug("Returning given docker tarfile: {}", givenDockerTarfile.getAbsolutePath());
            extractionBuilder.metaData(DOCKER_TAR_META_DATA, givenDockerTarfile);
        }
        return extractionBuilder.build();
    }

    private Extraction.Builder findCodeLocations(File directoryToSearch, File directory, @Nullable String dockerInspectorMessage) {
        File bdioFile = fileFinder.findFile(directoryToSearch, DEPENDENCIES_PATTERN);
        if (bdioFile != null) {
            SimpleBdioDocument simpleBdioDocument = null;

            try (InputStream dockerOutputInputStream = new FileInputStream(bdioFile); BdioReader bdioReader = new BdioReader(gson, dockerOutputInputStream)) {
                simpleBdioDocument = bdioReader.readSimpleBdioDocument();
            } catch (Exception e) {
                return new Extraction.Builder().exception(e);
            }

            if (simpleBdioDocument != null) {
                String projectName = simpleBdioDocument.getProject().name;
                String projectVersionName = simpleBdioDocument.getProject().version;

                // TODO ejk - update this when project external id is not req'd anymore
                Forge dockerForge = new Forge(BdioId.BDIO_ID_SEPARATOR, simpleBdioDocument.getProject().bdioExternalIdentifier.forge);
                String externalIdPath = simpleBdioDocument.getProject().bdioExternalIdentifier.externalId;
                ExternalId projectExternalId = externalIdFactory.createPathExternalId(dockerForge, externalIdPath);

                ProjectDependency projectDependency = new ProjectDependency(projectName, projectVersionName, projectExternalId);

                DependencyGraph dependencyGraph = bdioTransformer.transformToDependencyGraph(
                    projectDependency,
                    simpleBdioDocument.getProject(),
                    simpleBdioDocument.getComponents()
                );

                CodeLocation detectCodeLocation = new CodeLocation(dependencyGraph, projectExternalId);

                return new Extraction.Builder().success(detectCodeLocation).projectName(projectName).projectVersion(projectVersionName);
            }
        }
        logger.error("Docker Inspector returned no BDIO files");
        String dockerInspectorMsgSuffix = Optional.ofNullable(dockerInspectorMessage)
            .filter(StringUtils::isNotBlank)
            .map(s -> "; Docker Inspector message: " + s)
            .orElse("");
        return new Extraction.Builder().failure(
            "No files found matching pattern [" + DEPENDENCIES_PATTERN + "]. Expected docker-inspector to produce file in " + directory.toString() + dockerInspectorMsgSuffix);
    }
}
