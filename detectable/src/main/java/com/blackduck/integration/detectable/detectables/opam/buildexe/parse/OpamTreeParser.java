package com.blackduck.integration.detectable.detectables.opam.buildexe.parse;


import com.blackduck.integration.detectable.detectables.opam.parse.OpamParsedResult;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

public class OpamTreeParser {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private List<OpamParsedResult> codeLocations = new ArrayList<>();
    private DependencyGraph currentGraph;
    private final File sourceDirectory;
    private final ExternalIdFactory externalIdFactory;
    private final Gson gson;

    public OpamTreeParser(Gson gson, File sourceDirectory, ExternalIdFactory externalIdFactory) {
        this.gson = gson;
        this.sourceDirectory = sourceDirectory;
        this.externalIdFactory = externalIdFactory;
    }

    public List<OpamParsedResult> parseJsonTreeFile(File outputFile) throws IOException {
        //Check if there is any issue with the file
        if(outputFile == null || !outputFile.exists() || !outputFile.isFile()) {
            logger.warn("The opamTreeOutput.json file does not exist, the file was deleted, or an issue occurred while running the opam tree command.");
            return codeLocations;
        }

        //initialize json reader object
        try (JsonReader jsonReader = new JsonReader(new FileReader(outputFile))) {
            //convert the whole JSON file into OpamJsonFileModule PoJo
            OpamJsonFileModule opamTreeModule = gson.fromJson(jsonReader, OpamJsonFileModule.class);
            //After getting PoJo, we will loop over the projects which are found in the tree
            for(OpamTreeProjectModule opamTreeProjectModule: opamTreeModule.opamProjects) {
                processTreeJsonObject(opamTreeProjectModule);
            }
        } catch (IOException e) {
            throw new IOException("There was an error while parsing the JSON file.",e);
        }

        return codeLocations;
    }

    public void processTreeJsonObject(OpamTreeProjectModule opamTreeProjectModule){
        if(opamTreeProjectModule != null) {
            //this PoJo is OpamTreeProjectModule where it will have name, version and dependencies of one project
            String projectName = opamTreeProjectModule.name;
            String projectVersion = opamTreeProjectModule.version;

            //initialze the project
            initializeProject(projectName, projectVersion);

            if(!opamTreeProjectModule.dependencies.isEmpty()) {
                collectCodeLocations(opamTreeProjectModule.dependencies, null); //passing as null to distinguish between direct and transitive
            }
        }
    }

    private void collectCodeLocations(List<OpamTreeDependencyModule> dependencyModules, Dependency parentDependency) {
        // recursively loop over the dependencies for each project
        for(OpamTreeDependencyModule dependencyModule: dependencyModules) {
            if(dependencyModule != null) {
                String packageName = dependencyModule.name;
                String packageVersion = dependencyModule.version;

                Dependency dependency = createDependencyExternalId(packageName, packageVersion);

                //direct dependency
                if(parentDependency == null) {
                    currentGraph.addDirectDependency(dependency);
                } else { //transitive dependency
                    currentGraph.addChildWithParent(dependency, parentDependency);
                }

                //recursively call this method again if there are any transitives found with current dependency as parent
                if(!dependencyModule.dependencies.isEmpty()) {
                    collectCodeLocations(dependencyModule.dependencies, dependency);
                }
            }
        }
    }

    private void initializeProject(String projectName, String projectVersion) {
        Dependency projectDependency = createDependencyExternalId(projectName, projectVersion); // create a dependency for the project
        currentGraph = new BasicDependencyGraph();
        String codeLocationPath = sourceDirectory.getPath();
        if(!codeLocationPath.endsWith(projectDependency.getName())) { // get the source code for the project using specific opam file
            codeLocationPath = "/" + projectDependency.getName();
        }
        CodeLocation codeLocation = new CodeLocation(currentGraph, projectDependency.getExternalId(), new File(codeLocationPath));
        codeLocations.add(new OpamParsedResult(projectName, projectVersion, codeLocation)); // create a new opam parsed result for the project being initialized
    }



    private Dependency createDependencyExternalId(String name, String version) {
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.OPAM, name, version);
        return new Dependency(externalId);
    }
}
