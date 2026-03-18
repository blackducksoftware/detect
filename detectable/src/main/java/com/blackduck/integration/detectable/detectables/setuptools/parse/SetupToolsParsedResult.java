package com.blackduck.integration.detectable.detectables.setuptools.parse;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.blackduck.integration.detectable.python.util.PythonDependency;

public class SetupToolsParsedResult {

    private String projectName;
    private String projectVersion;
    private List<PythonDependency> directDependencies;
    private Map<String, List<PythonDependency>> extrasTransitives;

    public SetupToolsParsedResult(String projectName, String projectVersion, List<PythonDependency> parsedDirectDependencies) {
        this(projectName, projectVersion, parsedDirectDependencies, Collections.emptyMap());
    }

    public SetupToolsParsedResult(String projectName, String projectVersion, List<PythonDependency> parsedDirectDependencies, Map<String, List<PythonDependency>> extrasTransitives) {
        this.projectName = projectName;
        this.projectVersion = projectVersion;
        this.directDependencies = parsedDirectDependencies;
        this.extrasTransitives = extrasTransitives;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    public List<PythonDependency> getDirectDependencies() {
        return directDependencies;
    }

    public Map<String, List<PythonDependency>> getExtrasTransitives() {
        return extrasTransitives;
    }
}
