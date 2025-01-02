package com.blackduck.integration.detectable.detectables.opam.parse;


import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.io.File;

public class OpamParsedResult {

    private final String projectName;
    private String projectVersion;
    private  List<String> parsedDirectDependencies = new ArrayList<>();
    private Map<String, String> lockFileDependencies;
    private File sourceCode = null;
    private CodeLocation codeLocation = null;

    public OpamParsedResult(String projectName, String projectVersion, List<String> parsedDirectDependencies, File sourceCode) {
        this.projectName = projectName;
        this.parsedDirectDependencies = parsedDirectDependencies;
        this.projectVersion = projectVersion;
        this.sourceCode = sourceCode;
    }

    public OpamParsedResult(String projectName, String projectVersion, CodeLocation codeLocation) {
        this.projectName = projectName;
        this.codeLocation = codeLocation;
        this.projectVersion = projectVersion;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    public List<String> getParsedDirectDependencies() {
        return parsedDirectDependencies;
    }

    public String getProjectName() {
        return projectName;
    }

    public Map<String, String> getLockFileDependencies() {
        return lockFileDependencies;
    }
    public File getSourceCode() {
        return sourceCode;
    }
    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }
    public void setLockFileDependencies(Map<String, String> lockFileDependencies) {
        this.lockFileDependencies = lockFileDependencies;
    }

    public CodeLocation getCodeLocation() {
        return codeLocation;
    }
}
