package com.blackduck.integration.detectable.detectables.bun.lockfile;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.blackduck.integration.detectable.detectables.bun.BunPackageNameUtils;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockDependency;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockPackage;
import com.blackduck.integration.detectable.detectables.bun.lockfile.model.BunLockfileData;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class BunLockJsonParser {
    private static final String PACKAGES_KEY = "packages";
    private static final String WORKSPACES_KEY = "workspaces";
    private static final String DEPENDENCIES_KEY = "dependencies";
    private static final String DEV_DEPENDENCIES_KEY = "devDependencies";
    private static final String OPTIONAL_DEPENDENCIES_KEY = "optionalDependencies";
    private static final Pattern TRAILING_COMMA = Pattern.compile(",([\\s\\r\\n]*[}\\]])");

    public BunLockfileData parseBunLock(File bunLockFile) {
        ParsedLockfile parsed = readLockfile(bunLockFile);
        Map<String, Map<String, String>> rangeToVersion = buildRangeToVersion(parsed);
        List<BunLockPackage> packages = buildPackages(parsed);
        return new BunLockfileData(packages, rangeToVersion);
    }

    // Reads bun.lock and streams it into the raw key/dep structures.
    private ParsedLockfile readLockfile(File bunLockFile) {
        String content;
        try {
            String raw = FileUtils.readFileToString(bunLockFile, StandardCharsets.UTF_8);
            // bun.lock is JSONC; Gson setLenient handles comments but NOT trailing commas.
            // Version specifiers and SHA hashes never contain ", }" so this replacement is safe.
            content = TRAILING_COMMA.matcher(raw).replaceAll("$1");
        } catch (Exception e) {
            throw new RuntimeException("Failed to read bun.lock: " + bunLockFile.getAbsolutePath(), e);
        }

        ParsedLockfile parsed = new ParsedLockfile();
        try (JsonReader reader = new JsonReader(new StringReader(content))) {
            reader.setLenient(true);
            reader.beginObject();
            while (reader.hasNext()) {
                String topKey = reader.nextName();
                if (WORKSPACES_KEY.equals(topKey)) {
                    readWorkspaceDeps(reader, parsed.getWorkspaceDeps());
                } else if (PACKAGES_KEY.equals(topKey)) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        readPackageEntry(reader, reader.nextName(), parsed.getKeyToVersion(), parsed.getRawEntryDeps());
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
        return parsed;
    }

    // Builds name to (range or version to resolvedVersion), letting the transformer
    // resolve dep ranges without Yarn machinery.
    private Map<String, Map<String, String>> buildRangeToVersion(ParsedLockfile parsed) {
        Map<String, Map<String, String>> rangeToVersion = new HashMap<>();

        // Workspace deps always map to the top-level (flat) entry
        for (BunLockDependency dep : parsed.getWorkspaceDeps()) {
            NameVersion nv = parsed.getKeyToVersion().get(dep.getName());
            if (nv != null) {
                rangeToVersion.computeIfAbsent(nv.getName(), k -> new HashMap<>()).put(dep.getRange(), nv.getVersion());
            }
        }

        // Ancestor-walk each entry's deps to the correct resolved version.
        for (Map.Entry<String, List<BunLockDependency>> e : parsed.getRawEntryDeps().entrySet()) {
            String parentKey = e.getKey();
            for (BunLockDependency dep : e.getValue()) {
                NameVersion nv = resolveInContext(parentKey, dep.getName(), parsed.getKeyToVersion());
                if (nv != null) {
                    rangeToVersion.computeIfAbsent(nv.getName(), k -> new HashMap<>()).put(dep.getRange(), nv.getVersion());
                }
            }
        }

        // Each exact version also resolves to itself
        for (NameVersion nv : parsed.getKeyToVersion().values()) {
            rangeToVersion.computeIfAbsent(nv.getName(), k -> new HashMap<>()).putIfAbsent(nv.getVersion(), nv.getVersion());
        }

        return rangeToVersion;
    }

    // Deduplicates entries by (name, version), merging deps across all keys for the same pair.
    private List<BunLockPackage> buildPackages(ParsedLockfile parsed) {
        Map<NameVersion, Map<String, BunLockDependency>> mergedEntryDeps = new LinkedHashMap<>();
        for (Map.Entry<String, NameVersion> e : parsed.getKeyToVersion().entrySet()) {
            Map<String, BunLockDependency> depMap = mergedEntryDeps.computeIfAbsent(e.getValue(), k -> new LinkedHashMap<>());
            for (BunLockDependency dep : parsed.getRawEntryDeps().get(e.getKey())) {
                depMap.putIfAbsent(dep.getName(), dep);
            }
        }

        List<BunLockPackage> packages = new ArrayList<>(mergedEntryDeps.size());
        for (Map.Entry<NameVersion, Map<String, BunLockDependency>> e : mergedEntryDeps.entrySet()) {
            NameVersion nv = e.getKey();
            List<BunLockDependency> deps = new ArrayList<>(e.getValue().values());
            packages.add(new BunLockPackage(nv.getName(), nv.getVersion(), deps));
        }
        return packages;
    }

    private void readPackageEntry(
            JsonReader reader,
            String entryKey,
            Map<String, NameVersion> keyToVersion,
            Map<String, List<BunLockDependency>> rawEntryDeps) throws Exception {
        reader.beginArray();
        String resolvedSpecifier = reader.nextString();
        NameVersion nv = parseResolvedSpecifier(resolvedSpecifier);

        List<BunLockDependency> deps = Collections.emptyList();
        if (reader.hasNext()) {
            if (reader.peek() == JsonToken.STRING) {
                reader.nextString(); // skip registry tag ("") present in lockfileVersion 1
            }
            if (reader.hasNext() && reader.peek() == JsonToken.BEGIN_OBJECT) {
                deps = readDeps(reader);
            }
        }
        while (reader.hasNext()) {
            reader.skipValue();
        }
        reader.endArray();

        keyToVersion.put(entryKey, nv);
        rawEntryDeps.put(entryKey, deps);
    }

    private List<BunLockDependency> readDeps(JsonReader reader) throws Exception {
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

    private void readWorkspaceDeps(JsonReader reader, List<BunLockDependency> workspaceDeps) throws Exception {
        reader.beginObject();
        while (reader.hasNext()) {
            reader.nextName(); // workspace path (e.g. "" for root, "packages/foo" for sub-workspace)
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (DEPENDENCIES_KEY.equals(key) || DEV_DEPENDENCIES_KEY.equals(key)) {
                    readDepMap(reader, workspaceDeps, false);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        }
        reader.endObject();
    }

    // Ancestor-walk: tries context/depName at each level up to the flat key.
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

    // Strips the rightmost logical segment (one component for plain names, two for @scope/name).
    // Returns null when no further parent context exists (at the flat-key level).
    private static String parentContext(String key) {
        int lastSlash = key.lastIndexOf('/');
        if (lastSlash < 0) {
            return null;
        }
        // If the segment before lastSlash starts with '@', the tail is the second half of
        // @scope/name -- strip both components together so we land on the true parent key.
        int prevSlash = key.lastIndexOf('/', lastSlash - 1);
        int cutAt = (prevSlash >= 0 && key.charAt(prevSlash + 1) == '@') ? prevSlash : lastSlash;
        if (cutAt <= 0) {
            return null;
        }
        String parent = key.substring(0, cutAt);
        // "@scope" alone (no '/') is a fragment, not a valid key -- stop the walk.
        return (parent.startsWith("@") && !parent.contains("/")) ? null : parent;
    }

    // Parses "name@version" or "@scope/name@version" by splitting on the last '@'.
    private static NameVersion parseResolvedSpecifier(String resolvedSpecifier) {
        NameVersion nv = BunPackageNameUtils.parseNameVersion(resolvedSpecifier);
        return nv != null ? nv : new NameVersion(resolvedSpecifier, "");
    }

}
