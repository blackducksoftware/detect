package com.blackduck.integration.detectable.detectables.uv.buildexe;

import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import com.blackduck.integration.detectable.detectables.uv.buildexe.transform.UVTreeDependencyGraphTransformer;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.executable.ExecutableRunnerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UVBuildExtractor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DetectableExecutableRunner executableRunner;
    private final File sourceDirectory;
    private final UVTreeDependencyGraphTransformer uvTreeDependencyGraphTransformer;


    public UVBuildExtractor(DetectableExecutableRunner executableRunner, File sourceDirectory, UVTreeDependencyGraphTransformer uvTreeDependencyGraphTransformer) {
        this.executableRunner = executableRunner;
        this.sourceDirectory = sourceDirectory;
        this.uvTreeDependencyGraphTransformer = uvTreeDependencyGraphTransformer;
    }

    public Extraction extract(ExecutableTarget uvExe, File outputDirectory, UVDetectorOptions uvDetectorOptions, File uvTomlFile) throws ExecutableRunnerException {
        try {
            List<String> arguments = new ArrayList<>();
            arguments.add("tree");
            arguments.add("--no-dedupe");

            ExecutableOutput executableOutput = executableRunner.execute(ExecutableUtils.createFromTarget(sourceDirectory, uvExe, arguments));
            List<String> uvTreeOutput = executableOutput.getStandardOutputAsList();




            return null;
        } catch (Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }
}
