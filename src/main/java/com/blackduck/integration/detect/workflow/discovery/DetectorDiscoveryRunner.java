package com.blackduck.integration.detect.workflow.discovery;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.common.util.finder.SimpleFileFinder;
import com.blackduck.integration.configuration.property.Property;
import com.blackduck.integration.detect.configuration.DetectConfigurationFactory;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.configuration.DetectableOptionFactory;
import com.blackduck.integration.detect.tool.detector.DetectorRuleFactory;
import com.blackduck.integration.detect.tool.detector.factory.DetectDetectableFactory;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;
import com.blackduck.integration.detectable.factory.DetectableFactory;
import com.blackduck.integration.detector.base.DetectorType;
import com.blackduck.integration.detector.rule.DetectorRuleSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Top-level orchestrator for discovery mode.
 *
 * Builds the detector rule set (reusing Detect's own {@code DetectorRuleFactory}), runs the
 * applicability-only scan, enriches each applicable detector with its live flag catalog from
 * {@code DetectProperties} and (optionally) its documentation markdown, then writes the result JSON.
 *
 * The {@code DetectDetectableFactory} is built with null inspector resolvers on purpose: resolvers are
 * only used during extractable()/extract(), never during applicable(), which is all discovery runs.
 */
public class DetectorDiscoveryRunner {
    private static final String OUTPUT_FILE_NAME = "detect-discovery-output.json";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Gson gson;

    public DetectorDiscoveryRunner(Gson gson) {
        this.gson = gson;
    }

    public File run(
        String detectVersion,
        DetectableOptionFactory detectableOptionFactory,
        DetectConfigurationFactory detectConfigurationFactory,
        DirectoryManager directoryManager,
        boolean includeDocs,
        File outputDirectoryOverride
    ) throws IOException {
        File sourceDirectory = directoryManager.getSourceDirectory();
        FileFinder fileFinder = new SimpleFileFinder();

        // Build the detectable factory with null resolvers — safe because applicable() never uses them.
        DetectableFactory detectableFactory = new DetectableFactory(fileFinder, null, new ExternalIdFactory(), gson);
        DetectDetectableFactory detectDetectableFactory = new DetectDetectableFactory(
            detectableFactory,
            detectableOptionFactory,
            null, // DetectExecutableResolver
            null, // DockerInspectorResolver
            null, // GradleInspectorResolver
            null, // NugetInspectorResolver
            null, // PipInspectorResolver
            null  // ProjectInspectorResolver
        );

        DetectorRuleSet ruleSet = new DetectorRuleFactory().createRules(detectDetectableFactory);

        DetectorApplicabilityScanner scanner = new DetectorApplicabilityScanner();
        DetectorApplicabilityScanner.ScanResult scanResult = scanner.scan(
            sourceDirectory,
            ruleSet,
            detectConfigurationFactory.createDetectorFinderOptions(),
            detectConfigurationFactory.createDetectorSearchOptions(),
            fileFinder
        );

        List<DiscoveryDetectorEntry> applicableDetectors = new ArrayList<>();
        for (DetectorApplicabilityScanner.ApplicableHit hit : scanResult.hits) {
            DiscoveryDetectorEntry entry = new DiscoveryDetectorEntry();
            entry.detectorType = hit.detectorType.name();
            entry.detectableName = hit.detectableName;
            entry.triggeredBy = hit.triggeredBy;
            entry.triggeredByPath = hit.triggeredByPath;
            entry.foundInDirectory = hit.directory.getAbsolutePath();
            entry.flags = buildFlagEntries(hit.detectorType);
            entry.docContent = includeDocs ? loadDoc(hit.detectorType) : null;
            applicableDetectors.add(entry);
        }

        File outputDirectory = outputDirectoryOverride != null ? outputDirectoryOverride : directoryManager.getRunHomeDirectory();
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        File outputFile = new File(outputDirectory, OUTPUT_FILE_NAME);

        DetectorDiscoveryOutput output = new DetectorDiscoveryOutput();
        output.detectVersion = detectVersion;
        output.sourcePath = sourceDirectory.getAbsolutePath();
        output.outputFilePath = outputFile.getAbsolutePath();
        output.timestamp = Instant.now().toString();
        output.includedDocs = includeDocs;
        output.applicableDetectors = applicableDetectors;
        output.notApplicableDetectors = scanResult.notApplicable;

        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        Files.write(outputFile.toPath(), prettyGson.toJson(output).getBytes(StandardCharsets.UTF_8));

        logger.info("Discovery found {} applicable detector(s).", applicableDetectors.size());
        return outputFile;
    }

    /**
     * Filters the live property catalog for flags belonging to this detector's group.
     * Data comes straight from the running JAR, so it always matches the current Detect version.
     */
    private List<DiscoveryFlagEntry> buildFlagEntries(DetectorType detectorType) {
        List<DiscoveryFlagEntry> flags = new ArrayList<>();
        String groupName = groupNameFor(detectorType);
        if (groupName == null) {
            return flags;
        }

        for (Property property : DetectProperties.allProperties().getProperties()) {
            if (property.getPropertyGroupInfo() == null || property.getPropertyGroupInfo().getPrimaryGroup() == null) {
                continue;
            }
            String primaryGroup = property.getPropertyGroupInfo().getPrimaryGroup().getName();
            if (groupName.equalsIgnoreCase(primaryGroup)) {
                String description = property.getPropertyHelpInfo() != null ? property.getPropertyHelpInfo().getShortText() : null;
                String helpText = property.getPropertyHelpInfo() != null ? property.getPropertyHelpInfo().getLongText() : null;
                flags.add(new DiscoveryFlagEntry(
                    property.getKey(),
                    property.getName(),
                    property.describeType() == null ? "None" : property.describeType(),
                    property.describeDefault(),
                    description,
                    helpText
                ));
            }
        }
        return flags;
    }

    /** Maps a DetectorType to its DetectGroup name. Most match by lowercase name; a few need aliasing. */
    private String groupNameFor(DetectorType detectorType) {
        switch (detectorType) {
            case GO_MOD:
            case GO_DEP:
            case GO_VNDR:
            case GO_VENDOR:
            case GO_GRADLE:
                return "go";
            case RUBYGEMS:
                return "ruby";
            case SETUPTOOLS:
                return "python";
            // Detectors with no dedicated property group — no flags to surface.
            case CARTHAGE:
            case COCOAPODS:
            case CRAN:
            case GIT:
            case CLANG:
            case XCODE:
                return null;
            default:
                return detectorType.name().toLowerCase();
        }
    }

    private String loadDoc(DetectorType detectorType) {
        String docFile = docFileFor(detectorType);
        if (docFile == null) {
            return null;
        }
        try (InputStream is = getClass().getResourceAsStream("/discovery/docs/" + docFile)) {
            if (is == null) {
                return null;
            }
            byte[] bytes = readAll(is);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.debug("Could not load discovery doc {}: {}", docFile, e.getMessage());
            return null;
        }
    }

    private String docFileFor(DetectorType detectorType) {
        switch (detectorType) {
            case GO_MOD:
            case GO_DEP:
            case GO_VNDR:
            case GO_VENDOR:
            case GO_GRADLE:
                return "golang.md";
            case PIP:
            case POETRY:
            case SETUPTOOLS:
            case UV:
                return "python.md";
            case RUBYGEMS:
            case COCOAPODS:
            case CRAN:
            case XCODE:
            case CARTHAGE:
            case GIT:
            case CLANG:
                return null;
            default:
                return detectorType.name().toLowerCase() + ".md";
        }
    }

    private byte[] readAll(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }
}


