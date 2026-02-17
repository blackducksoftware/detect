package com.blackduck.integration.detectable.detectables.conda.tree;

import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.util.ToolVersionLogger;

import java.io.File;

public class CondaTreeExtractor {

    private final DetectableExecutableRunner executableRunner;
    private final ToolVersionLogger toolVersionLogger;

    public CondaTreeExtractor(DetectableExecutableRunner executableRunner, ToolVersionLogger toolVersionLogger) {
        this.executableRunner = executableRunner;
        this.toolVersionLogger = toolVersionLogger;
    }

    public Extraction extract(File directory, ExecutableTarget condaTreeExe, File workingDirectory, String condaEnvironmentName) {
        return null;
    }
}
