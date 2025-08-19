package com.blackduck.integration.detectable.detectables.yarn.packagejson;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.blackduck.integration.detectable.util.JsonSanitizer;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectables.npm.packagejson.model.YarnPackageJson;

public class PackageJsonReader {
    public static final String WORKSPACES_OBJECT_KEY = "workspaces";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Gson gson;

    public PackageJsonReader(Gson gson) {
        this.gson = gson;
    }

    public NullSafePackageJson read(String packageJsonText) {
        YarnPackageJson rawPackageJson = gson.fromJson(JsonSanitizer.sanitize(packageJsonText), YarnPackageJson.class);
        return new NullSafePackageJson(rawPackageJson);
    }

    public List<String> extractWorkspaceDirPatterns(String packageJsonText) {
        Map<String, Object> packageJsonMap = gson.fromJson(JsonSanitizer.sanitize(packageJsonText), Map.class);
        // Possible alt. approach: pass it a TypeAdapter
        Object workspacesObject = packageJsonMap.get(WORKSPACES_OBJECT_KEY);
        List<String> workspaceSubdirPatterns = new LinkedList<>();
        if (workspacesObject != null) {
            logger.trace("workspacesObject type: {}", workspacesObject.getClass().getName());
            if (workspacesObject instanceof Map) {
                logger.trace("workspacesObject is a Map");
                YarnPackageJsonWorkspacesAsObject rootPackageJsonNewSyntax = gson.fromJson(JsonSanitizer.sanitize(packageJsonText), YarnPackageJsonWorkspacesAsObject.class);
                workspaceSubdirPatterns.addAll(rootPackageJsonNewSyntax.workspaces.workspaceSubdirPatterns);
            } else if (workspacesObject instanceof List) {
                logger.trace("workspacesObject is a List");
                YarnPackageJsonWorkspacesAsList rootPackageJsonOldSyntax = gson.fromJson(JsonSanitizer.sanitize(packageJsonText), YarnPackageJsonWorkspacesAsList.class);
                workspaceSubdirPatterns.addAll(rootPackageJsonOldSyntax.workspaceSubdirPatterns);
            } else {
                logger.warn("package.json 'workspaces' object is an unrecognized format; workspace declarations will be ignored");
            }
        }
        return workspaceSubdirPatterns;
    }
}
