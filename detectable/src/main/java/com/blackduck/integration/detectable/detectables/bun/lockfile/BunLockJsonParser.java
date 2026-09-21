package com.blackduck.integration.detectable.detectables.bun.lockfile;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunDependencyType;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockfileData;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunPackage;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunPackageDependency;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.DirectDependency;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.WorkspaceDependency;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class BunLockJsonParser {
    private static final String PACKAGES_KEY = "packages";
    private static final String WORKSPACES_KEY = "workspaces";
    private static final String DEPENDENCIES_KEY = "dependencies";
    private static final String DEV_DEPENDENCIES_KEY = "devDependencies";
    private static final String PEER_DEPENDENCIES_KEY = "peerDependencies";
    private static final String OPTIONAL_DEPENDENCIES_KEY = "optionalDependencies";
    private static final String OPTIONAL_PEERS_KEY = "optionalPeers";
    private static final Pattern TRAILING_COMMA = Pattern.compile(",([\\s\\r\\n]*[}\\]])");

    public BunLockfileData parseBunLock(File bunLockFile) {
        String content = readContent(bunLockFile);

        List<WorkspaceDependency> workspaceDependencies = new ArrayList<>();
        List<BunPackage> packages = new ArrayList<>();

        try (JsonReader reader = new JsonReader(new StringReader(content))) {
            reader.setLenient(true);
            reader.beginObject();
            while (reader.hasNext()) {
                String topKey = reader.nextName();
                if (WORKSPACES_KEY.equals(topKey)) {
                    readWorkspaceDependencies(reader, workspaceDependencies);
                } else if (PACKAGES_KEY.equals(topKey)) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String entryKey = reader.nextName();
                        packages.add(readPackage(reader, entryKey));
                    }
                    reader.endObject();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse bun.lock: " + bunLockFile.getAbsolutePath(), e);
        }

        List<DirectDependency> directDependencies = resolveDirectDependencies(workspaceDependencies, packages);
        return new BunLockfileData(directDependencies, packages);
    }

    private String readContent(File bunLockFile) {
        try {
            String raw = FileUtils.readFileToString(bunLockFile, StandardCharsets.UTF_8);
            // bun.lock is JSONC; Gson setLenient handles comments but NOT trailing commas.
            return TRAILING_COMMA.matcher(raw).replaceAll("$1");
        } catch (Exception e) {
            throw new RuntimeException("Failed to read bun.lock: " + bunLockFile.getAbsolutePath(), e);
        }
    }

    private List<DirectDependency> resolveDirectDependencies(List<WorkspaceDependency> workspaceDependencies, List<BunPackage> packages) {
        Map<String, BunPackage> packagesByKey = new LinkedHashMap<>();
        for (BunPackage pkg : packages) {
            packagesByKey.put(pkg.getKey(), pkg);
        }

        List<DirectDependency> result = new ArrayList<>();
        for (WorkspaceDependency dep : workspaceDependencies) {
            if (dep.getRange().startsWith("workspace:")) {
                continue; // local monorepo package, no npm registry entry
            }
            // Always resolve from the packages section; it holds the pinned version regardless
            // of how the dep was referenced in the workspace (semver, catalog:, file:, etc.).
            BunPackage pkg = packagesByKey.get(dep.getName());
            if (pkg != null) {
                result.add(new DirectDependency(dep.getName(), pkg.getVersion(), dep.getType()));
            }
        }
        return result;
    }

    private void readWorkspaceDependencies(JsonReader reader, List<WorkspaceDependency> workspaceDependencies) throws Exception {
        reader.beginObject();
        while (reader.hasNext()) {
            reader.nextName();
            readWorkspaceEntry(reader, workspaceDependencies);
        }
        reader.endObject();
    }

    private void readWorkspaceEntry(JsonReader reader, List<WorkspaceDependency> workspaceDependencies) throws Exception {
        List<WorkspaceDependency> peerDependencies = new ArrayList<>();
        Set<String> optionalPeers = new HashSet<>();

        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            if (DEPENDENCIES_KEY.equals(key)) {
                readWorkspaceDependencyMap(reader, workspaceDependencies, BunDependencyType.NORMAL);
            } else if (DEV_DEPENDENCIES_KEY.equals(key)) {
                readWorkspaceDependencyMap(reader, workspaceDependencies, BunDependencyType.DEV);
            } else if (PEER_DEPENDENCIES_KEY.equals(key)) {
                readWorkspaceDependencyMap(reader, peerDependencies, BunDependencyType.PEER);
            } else if (OPTIONAL_DEPENDENCIES_KEY.equals(key)) {
                readWorkspaceDependencyMap(reader, workspaceDependencies, BunDependencyType.OPTIONAL);
            } else if (OPTIONAL_PEERS_KEY.equals(key)) {
                readOptionalPeersList(reader, optionalPeers);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        // optionalPeers lists which peerDependencies entries are optional; apply that flag now.
        for (WorkspaceDependency peer : peerDependencies) {
            BunDependencyType type = optionalPeers.contains(peer.getName()) ? BunDependencyType.OPTIONAL_PEER : BunDependencyType.PEER;
            workspaceDependencies.add(new WorkspaceDependency(peer.getName(), peer.getRange(), type));
        }
    }

    private void readWorkspaceDependencyMap(JsonReader reader, List<WorkspaceDependency> list, BunDependencyType type) throws Exception {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (reader.peek() == JsonToken.STRING) {
                list.add(new WorkspaceDependency(name, reader.nextString(), type));
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private void readOptionalPeersList(JsonReader reader, Set<String> optionalPeers) throws Exception {
        reader.beginArray();
        while (reader.hasNext()) {
            optionalPeers.add(reader.nextString());
        }
        reader.endArray();
    }

    private BunPackage readPackage(JsonReader reader, String entryKey) throws Exception {
        reader.beginArray();
        String resolvedSpecifier = reader.nextString();
        NameVersion nameVersion = parseResolvedSpecifier(resolvedSpecifier);

        List<BunPackageDependency> dependencies = Collections.emptyList();
        if (reader.hasNext()) {
            if (reader.peek() == JsonToken.STRING) {
                reader.nextString(); // skip registry tag ("") in lockfileVersion 1
            }
            if (reader.hasNext() && reader.peek() == JsonToken.BEGIN_OBJECT) {
                dependencies = readPackageDependencies(reader);
            }
        }
        while (reader.hasNext()) {
            reader.skipValue();
        }
        reader.endArray();

        return new BunPackage(entryKey, nameVersion.getName(), nameVersion.getVersion(), dependencies);
    }

    private List<BunPackageDependency> readPackageDependencies(JsonReader reader) throws Exception {
        List<BunPackageDependency> dependencies = new ArrayList<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            if (DEPENDENCIES_KEY.equals(key)) {
                readPackageDependencyMap(reader, dependencies, BunDependencyType.NORMAL);
            } else if (OPTIONAL_DEPENDENCIES_KEY.equals(key)) {
                readPackageDependencyMap(reader, dependencies, BunDependencyType.OPTIONAL);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return dependencies;
    }

    private void readPackageDependencyMap(JsonReader reader, List<BunPackageDependency> dependencies, BunDependencyType type) throws Exception {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (reader.peek() == JsonToken.STRING) {
                dependencies.add(new BunPackageDependency(name, reader.nextString(), type));
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private static NameVersion parseResolvedSpecifier(String resolvedSpecifier) {
        NameVersion nameVersion = parseNameVersion(resolvedSpecifier);
        return nameVersion != null ? nameVersion : new NameVersion(resolvedSpecifier, "");
    }

    public static NameVersion parseNameVersion(String s) {
        int lastAt = s.lastIndexOf('@');
        if (lastAt <= 0) {
            return null;
        }
        return new NameVersion(s.substring(0, lastAt), s.substring(lastAt + 1));
    }
}
