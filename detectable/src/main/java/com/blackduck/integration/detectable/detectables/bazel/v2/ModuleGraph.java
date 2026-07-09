package com.blackduck.integration.detectable.detectables.bazel.v2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Carries the full parent-child edge structure of the BCR module graph as returned
 * by {@code bazel mod graph --output json}.
 *
 * <ul>
 *   <li>{@code directModuleKeys} — the root's immediate children (modules declared via
 *       {@code bazel_dep} in the project's own {@code MODULE.bazel}).</li>
 *   <li>{@code childrenByModuleKey} — for every non-root module that has children,
 *       maps its key to the ordered list of child keys. Only modules that actually have
 *       children appear as keys in this map; leaf modules are absent.</li>
 * </ul>
 *
 * <p>Use {@link #getAllModuleKeys()} to get the union of all unique module keys in the graph.
 * Produced by {@link BzlmodGraphJsonParser#parseModuleGraph(String)}.
 */
public class ModuleGraph {
    /** Module keys that are direct children of the root (declared in MODULE.bazel). */
    public final Set<String> directModuleKeys;
    /** parent module key → ordered list of child module keys (populated only for non-leaf modules). */
    public final Map<String, List<String>> childrenByModuleKey;

    ModuleGraph(Set<String> directModuleKeys, Map<String, List<String>> childrenByModuleKey) {
        this.directModuleKeys = Collections.unmodifiableSet(new LinkedHashSet<>(directModuleKeys));
        this.childrenByModuleKey = Collections.unmodifiableMap(new LinkedHashMap<>(childrenByModuleKey));
    }

    /**
     * Returns the union of all unique module keys present in the graph: direct deps
     * plus every module that appears as a child of any other module.
     */
    public Set<String> getAllModuleKeys() {
        Set<String> all = new LinkedHashSet<>(directModuleKeys);
        for (List<String> children : childrenByModuleKey.values()) {
            all.addAll(children);
        }
        return all;
    }
}
