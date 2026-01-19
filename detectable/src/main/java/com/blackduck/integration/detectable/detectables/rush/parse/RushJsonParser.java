package com.blackduck.integration.detectable.detectables.rush.parse;

import com.blackduck.integration.detectable.detectables.rush.RushProjectType;
import com.blackduck.integration.detectable.detectables.rush.model.RushJsonParseResult;
import com.blackduck.integration.detectable.detectables.rush.model.RushProject;
import com.blackduck.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class RushJsonParser {
    private static final String PROJECTS_KEY = "projects";
    private static final String PNPM_VERSION_KEY = "pnpmVersion";
    private static final String NPM_VERSION_KEY = "npmVersion";
    private static final String YARN_VERSION_KEY = "yarnVersion";
    private final Gson gson;

    public RushJsonParser(Gson gson) {
        this.gson = gson;
    }

    public RushJsonParseResult parseRushJsonFile(File rushJsonFile) {
        RushProjectType projectType = null;
        List<RushProject> rushProjects = new ArrayList<>();
        try (JsonReader reader = new JsonReader(new FileReader(rushJsonFile))) {
            reader.setLenient(true);
            reader.beginObject();
            while (reader.hasNext()) {
                String modulesObject = reader.nextName();
                if (modulesObject != null && modulesObject.equals(PNPM_VERSION_KEY)) {
                    projectType = RushProjectType.PNPM;
                    reader.skipValue();
                } else if (modulesObject != null && modulesObject.equals(NPM_VERSION_KEY)) {
                    projectType = RushProjectType.NPM;
                    reader.skipValue();
                } else if (modulesObject != null && modulesObject.equals(YARN_VERSION_KEY)) {
                    projectType = RushProjectType.YARN;
                    reader.skipValue();
                } else if (modulesObject != null && modulesObject.equals(PROJECTS_KEY)) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        JsonObject module = JsonParser.parseReader(reader).getAsJsonObject();
                        RushProject rushProject = gson.fromJson(module, RushProject.class);
                        rushProjects.add(rushProject);
                    }
                    reader.endArray();
                }  else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new RushJsonParseResult(projectType, rushProjects);

    }

}
