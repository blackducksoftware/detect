package com.blackduck.integration.detectable.detectables.uv.lockfile;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.pip.parser.RequirementsFileDependencyTransformer;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import com.blackduck.integration.detectable.detectables.uv.parse.UVTomlParser;
import com.blackduck.integration.detectable.detectables.uv.transform.UVLockParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.python.util.PythonDependency;
import com.blackduck.integration.detectable.python.util.PythonDependencyTransformer;
import com.blackduck.integration.executable.ExecutableRunnerException;
import com.blackduck.integration.util.NameVersion;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UVLockfileExtractor {

    private final UVLockParser uvLockParser;
    private final PythonDependencyTransformer requirementsFileTransformer;
    private final RequirementsFileDependencyTransformer requirementsFileDependencyTransformer;

    public UVLockfileExtractor(UVLockParser uvLockParser, PythonDependencyTransformer requirementsFileTransformer, RequirementsFileDependencyTransformer requirementsFileDependencyTransformer) {
        this.uvLockParser = uvLockParser;
        this.requirementsFileTransformer = requirementsFileTransformer;
        this.requirementsFileDependencyTransformer = requirementsFileDependencyTransformer;
    }

    public Extraction extract(UVDetectorOptions uvDetectorOptions, UVTomlParser uvTomlParser, File uvLockFile, File requirementsTxtFile) throws ExecutableRunnerException, IOException {
        try {
            Optional<NameVersion> projectNameVersion = uvTomlParser.parseNameVersion();
            String projectName = uvTomlParser.getProjectName(); // get just project name in case version doesn't exist

            List<CodeLocation> codeLocations = new ArrayList<>();
            if (uvLockFile != null) {
                String uvLockContents = FileUtils.readFileToString(uvLockFile, StandardCharsets.UTF_8);
                codeLocations = uvLockParser.parseLockFile(uvLockContents, projectName, uvDetectorOptions);
            }

            if (requirementsTxtFile != null) {
                List<PythonDependency> dependencies = requirementsFileTransformer.transform(requirementsTxtFile);
                DependencyGraph dependencyGraph = requirementsFileDependencyTransformer.transform(dependencies);
                CodeLocation codeLocation = new CodeLocation(dependencyGraph);
                codeLocations.add(codeLocation);
            }


            return new Extraction.Builder()
                    .success(codeLocations)
                    .nameVersionIfPresent(projectNameVersion)
                    .build();
        } catch (Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }

}
