package com.blackduck.integration.detectable.detectables.bun.lockfile;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockDependency;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockPackage;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockResult;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockfileData;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class BunLockJsonParser {
    private static final String PACKAGES_KEY = "packages";
    private static final String WORKSPACES_KEY = "workspaces";
    private static final String DEPENDENCIES_KEY = "dependencies";
    private static final String DEV_DEPENDENCIES_KEY = "devDependencies";
    private static final String OPTIONAL_DEPENDENCIES_KEY = "optionalDependencies";
    private static final char NV_KEY_SEP = '§'; // § separator — never appears in npm names or semver

    @SuppressWarnings("unused")
    private final Gson gson;

    public BunLockJsonParser(Gson gson) {
        this.gson = gson;
    }

    public BunLockResult parseBunLock(File bunLockFile) {
        String content;
        try {
            String raw = new String(Files.readAllBytes(bunLockFile.toPath()), StandardCharsets.UTF_8);
            // bun.lock is JSONC; Gson setLenient handles comments but NOT trailing commas.
            // Version specifiers and SHA hashes never contain ", }" so this replacement is safe.
            content = raw.replaceAll(",([\\s\\r\\n]*[}\\]])", "$1");
        } catch (Exception e) {
            throw new RuntimeException("Failed to read bun.lock: " + bunLockFile.getAbsolutePath(), e);
        }

        // key → (name, resolvedVersion) for every packages entry, insertion-ordered
        Map<String, NameVersion> keyToVersion = new LinkedHashMap<>();
        // key → dep list (dependencies + optionalDependencies) for that entry
        Map<String, List<BunLockDependency>> rawEntryDeps = new LinkedHashMap<>();
        // dep ranges from the workspaces section
        List<BunLockDependency> workspaceDeps = new ArrayList<>();

        try (JsonReader reader = new JsonReader(new StringReader(content))) {
            reader.setLenient(true);
            reader.beginObject();
            while (reader.hasNext()) {
                String topKey = reader.nextName();
                if (WORKSPACES_KEY.equals(topKey)) {
                    parseWorkspaceDeps(reader, workspaceDeps);
                } else if (PACKAGES_KEY.equals(topKey)) {
                    parseAllPackages(reader, keyToVersion, rawEntryDeps);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse bun.lock: " + bunLockFile.getAbsolutePath(), e);
        }

        // Build range-to-version mapping using bun.lock's path-qualified key structure.
        // For each dep (D, range R) in entry with key K: look up "K/D" in keyToVersion.
        // If found, R resolves to that nested version; otherwise fall back to the flat entry.
        // This correctly routes each range to its target version for multi-version packages.
        //
        // nvKey (name§version) → set of version ranges that resolve to this exact version
        Map<String, Set<String>> versionRanges = new LinkedHashMap<>();

        // Workspace deps always map to the top-level (flat) entry
        for (BunLockDependency dep : workspaceDeps) {
            NameVersion nv = keyToVersion.get(dep.getName());
            if (nv != null) {
                versionRanges.computeIfAbsent(nvKey(nv), k -> new LinkedHashSet<>()).add(dep.getRange());
            }
        }

        // Each entry's deps: use hierarchical ancestor walk to route ranges to the right version.
        // bun resolves "npm/chalk"'s dep "ansi-styles" by trying npm/chalk/ansi-styles → npm/ansi-styles → ansi-styles.
        for (Map.Entry<String, List<BunLockDependency>> e : rawEntryDeps.entrySet()) {
            String parentKey = e.getKey();
            for (BunLockDependency dep : e.getValue()) {
                NameVersion targetNV = resolveInContext(parentKey, dep.getName(), keyToVersion);
                if (targetNV != null) {
                    versionRanges.computeIfAbsent(nvKey(targetNV), k -> new LinkedHashSet<>()).add(dep.getRange());
                }
            }
        }

        // Build rangeToVersion: name → { range → resolvedVersion }
        // This lets the transformer resolve any dep range to its exact version without Yarn machinery.
        Map<String, Map<String, String>> rangeToVersion = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : versionRanges.entrySet()) {
            String key = e.getKey();
            int sep = key.lastIndexOf(NV_KEY_SEP);
            String name = key.substring(0, sep);
            String version = key.substring(sep + 1);
            Map<String, String> rangeMap = rangeToVersion.computeIfAbsent(name, k -> new HashMap<>());
            rangeMap.put(version, version); // exact version resolves to itself
            for (String range : e.getValue()) {
                rangeMap.put(range, version);
            }
        }

        // Deduplicate entries by (name, version), merging deps across all keys for the same pair
        Map<String, Map<String, BunLockDependency>> mergedEntryDeps = new LinkedHashMap<>();
        Map<String, NameVersion> uniqueNvKeys = new LinkedHashMap<>();
        for (Map.Entry<String, NameVersion> e : keyToVersion.entrySet()) {
            String key = nvKey(e.getValue());
            uniqueNvKeys.putIfAbsent(key, e.getValue());
            Map<String, BunLockDependency> depMap = mergedEntryDeps.computeIfAbsent(key, k -> new LinkedHashMap<>());
            for (BunLockDependency dep : rawEntryDeps.getOrDefault(e.getKey(), Collections.emptyList())) {
                depMap.putIfAbsent(dep.getName(), dep);
            }
        }

        // Build the final package list
        List<BunLockPackage> packages = new ArrayList<>(uniqueNvKeys.size());
        for (Map.Entry<String, NameVersion> e : uniqueNvKeys.entrySet()) {
            NameVersion nv = e.getValue();
            List<BunLockDependency> deps = new ArrayList<>(mergedEntryDeps.getOrDefault(e.getKey(), Collections.emptyMap()).values());
            packages.add(new BunLockPackage(nv.getName(), nv.getVersion(), deps));
        }

        return new BunLockResult(new BunLockfileData(packages, rangeToVersion));
    }

    private void parseAllPackages(
            JsonReader reader,
            Map<String, NameVersion> keyToVersion,
            Map<String, List<BunLockDependency>> rawEntryDeps) throws Exception {
        reader.beginObject();
        while (reader.hasNext()) {
            String entryKey = reader.nextName();
            parseTupleInto(reader, entryKey, keyToVersion, rawEntryDeps);
        }
        reader.endObject();
    }

    private void parseTupleInto(
            JsonReader reader,
            String entryKey,
            Map<String, NameVersion> keyToVersion,
            Map<String, List<BunLockDependency>> rawEntryDeps) throws Exception {
        reader.beginArray();
        String resolvedSpecifier = reader.nextString();
        NameVersion nv = parseResolvedSpecifier(resolvedSpecifier);

        List<BunLockDependency> deps = Collections.emptyList();
        if (reader.hasNext()) {
            JsonToken next = reader.peek();
            if (next == JsonToken.STRING) {
                // lockfileVersion 1: ["specifier", "", {metadata}, "sha512-..."]
                reader.nextString(); // skip registry tag ("")
                if (reader.hasNext() && reader.peek() == JsonToken.BEGIN_OBJECT) {
                    deps = parseDeps(reader);
                }
            } else if (next == JsonToken.BEGIN_OBJECT) {
                // lockfileVersion 0: ["specifier", {metadata}, "cacheKey", ...]
                deps = parseDeps(reader);
            }
        }
        while (reader.hasNext()) {
            reader.skipValue();
        }
        reader.endArray();

        keyToVersion.put(entryKey, nv);
        rawEntryDeps.put(entryKey, deps);
    }

    private List<BunLockDependency> parseDeps(JsonReader reader) throws Exception {
        List<BunLockDependency> deps = new ArrayList<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            if (DEPENDENCIES_KEY.equals(key)) {
                readDepMap(reader, deps, false);
            } else if (OPTIONAL_DEPENDENCIES_KEY.equals(key)) {
                readDepMap(reader, deps, true);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return deps;
    }

    private void readDepMap(JsonReader reader, List<BunLockDependency> deps, boolean optional) throws Exception {
        reader.beginObject();
        while (reader.hasNext()) {
            String depName = reader.nextName();
            String range = reader.nextString();
            deps.add(new BunLockDependency(depName, range, optional));
        }
        reader.endObject();
    }

    private void parseWorkspaceDeps(JsonReader reader, List<BunLockDependency> workspaceDeps) throws Exception {
        reader.beginObject();
        while (reader.hasNext()) {
            reader.nextName(); // workspace path (e.g. "" for root, "packages/foo" for sub-workspace)
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (DEPENDENCIES_KEY.equals(key) || DEV_DEPENDENCIES_KEY.equals(key)) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String depName = reader.nextName();
                        String range = reader.nextString();
                        workspaceDeps.add(new BunLockDependency(depName, range, false));
                    }
                    reader.endObject();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        }
        reader.endObject();
    }

    // Walk from context toward the root, trying context/depName at each level, then the flat entry.
    // Example: context="npm/chalk", dep="ansi-styles" tries npm/chalk/ansi-styles → npm/ansi-styles → ansi-styles.
    private NameVersion resolveInContext(String context, String depName, Map<String, NameVersion> keyToVersion) {
        String ctx = context;
        while (ctx != null) {
            NameVersion nv = keyToVersion.get(ctx + "/" + depName);
            if (nv != null) {
                return nv;
            }
            ctx = parentContext(ctx);
        }
        return keyToVersion.get(depName);
    }

    // Strip the rightmost path segment. Returns null when already at flat level.
    // Scoped packages (@scope/name) are atomic — the single separating slash must not be stripped.
    private static String parentContext(String key) {
        int lastSlash = key.lastIndexOf('/');
        if (lastSlash < 0) {
            return null;
        }
        String parent = key.substring(0, lastSlash);
        if (parent.startsWith("@") && parent.indexOf('/') == parent.lastIndexOf('/')) {
            return null;
        }
        return parent;
    }

    // Splits "react@18.3.1" → ("react", "18.3.1") and "@babel/core@7.0.0" → ("@babel/core", "7.0.0").
    // lastIndexOf('@') preserves the leading '@' in scoped package names.
    private static NameVersion parseResolvedSpecifier(String resolvedSpecifier) {
        int lastAt = resolvedSpecifier.lastIndexOf('@');
        if (lastAt <= 0) {
            return new NameVersion(resolvedSpecifier, "");
        }
        return new NameVersion(resolvedSpecifier.substring(0, lastAt), resolvedSpecifier.substring(lastAt + 1));
    }

    private static String nvKey(NameVersion nv) {
        return nv.getName() + NV_KEY_SEP + nv.getVersion();
    }
}
