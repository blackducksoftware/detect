package com.blackduck.integration.detectable.detectables.nuget.future;

public class ParsedProject {
    private final String path;
    private final String guid;
    private final String name;

    public ParsedProject(String path, String guid, String name) {
        this.path = path;
        this.guid = guid;
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }
    //public String guid; //Available, not used

    //public List<String> projectDependencies; //Available, not used
}
