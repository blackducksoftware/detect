package com.blackduck.integration.detect.tool.detector.inspector;

import java.io.File;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detect.workflow.airgap.AirGapInspectorPaths;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.inspector.GradleInspectorResolver;
import com.blackduck.integration.detectable.detectables.gradle.inspection.inspector.GradleInspectorScriptCreator;
import com.blackduck.integration.detectable.detectables.gradle.inspection.inspector.GradleInspectorScriptOptions;

import freemarker.template.Configuration;

public class ArtifactoryGradleInspectorResolver implements GradleInspectorResolver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String GRADLE_DIR_NAME = "gradle";
    private static final String GENERATED_GRADLE_SCRIPT_NAME = "init-detect.gradle";

    private final Configuration configuration;
    private final GradleInspectorScriptOptions gradleInspectorScriptOptions;
    private final AirGapInspectorPaths airGapInspectorPaths;
    private final DirectoryManager directoryManager;

    private File generatedGradleScriptPath = null;
    private boolean hasResolvedInspector = false;

    public ArtifactoryGradleInspectorResolver(
        Configuration configuration,
        GradleInspectorScriptOptions gradleInspectorScriptOptions,
        AirGapInspectorPaths airGapInspectorPaths,
        DirectoryManager directoryManager
    ) {
        this.configuration = configuration;
        this.gradleInspectorScriptOptions = gradleInspectorScriptOptions;
        this.airGapInspectorPaths = airGapInspectorPaths;
        this.directoryManager = directoryManager;
    }

    @Override
    public File resolveGradleInspector() throws DetectableException {
        if (!hasResolvedInspector) {
            hasResolvedInspector = true;
            try {
                File generatedGradleScriptFile = directoryManager.getSharedFile(GRADLE_DIR_NAME, GENERATED_GRADLE_SCRIPT_NAME);
                GradleInspectorScriptCreator gradleInspectorScriptCreator = new GradleInspectorScriptCreator(configuration);
                generatedGradleScriptPath = gradleInspectorScriptCreator.createGradleInspector(generatedGradleScriptFile, gradleInspectorScriptOptions);
            } catch (Exception e) {
                throw new DetectableException(e);
            }

            if (generatedGradleScriptPath == null) {
                throw new DetectableException("Unable to initialize the gradle inspector.");
            } else {
                logger.trace(String.format("Derived generated gradle script path: %s", generatedGradleScriptPath));
            }
        } else {
            logger.debug("Already attempted to resolve the gradle inspector script, will not attempt again.");
        }
        if (generatedGradleScriptPath == null) {
            throw new DetectableException("Unable to find or create the gradle inspector script.");
        }

        return generatedGradleScriptPath;
    }
}
