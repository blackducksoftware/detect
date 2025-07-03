package com.blackduck.integration.detectable.detectables.npm.packagejson;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

public class CombinedPackageJson {
    
    private String name;
    private String version;
    private List<String> relativeWorkspaces = new ArrayList<>();
    
    private MultiValuedMap<String, String> dependencies;
    private MultiValuedMap<String, String> devDependencies;
    private MultiValuedMap<String, String> peerDependencies;
    private MultiValuedMap<String, String> optionalDependencies;
    
    public CombinedPackageJson() {
        dependencies = new HashSetValuedHashMap<>();
        devDependencies = new HashSetValuedHashMap<>();
        peerDependencies = new HashSetValuedHashMap<>();
        optionalDependencies = new HashSetValuedHashMap<>();
    }

    public MultiValuedMap<String, String> getDependencies() {
        return dependencies;
    }

    public MultiValuedMap<String, String> getDevDependencies() {
        return devDependencies;
    }

    public MultiValuedMap<String, String> getPeerDependencies() {
        return peerDependencies;
    }
    
    public MultiValuedMap<String, String> getOptionalDependencies() {
        return optionalDependencies;
    }
    
    public List<String> getRelativeWorkspaces() {        
        return relativeWorkspaces;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
