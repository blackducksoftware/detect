package com.blackduck.integration.detectable.detectables.yarn;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLock;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.YarnLockEntry;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.YarnLockEntryId;
import com.blackduck.integration.util.NameVersion;

/**
 * Maps a yarn.lock descriptor ("alias@npm:real@1.0.0", "ms@npm:^2.0.0", ...) to the real package identity recorded in the
 * "resolution:" of the entry that descriptor belongs to. Descriptors whose resolution has no registry identity
 * (file:, workspace:, git, ...) are simply absent, so callers fall back to their existing behavior for them.
 */
public class YarnResolvedIdentities {
    private final Map<String, NameVersion> identityByDescriptor;
    private final Map<YarnLockEntry, NameVersion> identityByEntry;

    private YarnResolvedIdentities(Map<String, NameVersion> identityByDescriptor, Map<YarnLockEntry, NameVersion> identityByEntry) {
        this.identityByDescriptor = identityByDescriptor;
        this.identityByEntry = identityByEntry;
    }

    public static YarnResolvedIdentities from(YarnLock yarnLock) {
        Map<String, NameVersion> identityByDescriptor = new HashMap<>();
        Map<YarnLockEntry, NameVersion> identityByEntry = new IdentityHashMap<>();
        for (YarnLockEntry entry : yarnLock.getEntries()) {
            Optional<NameVersion> identity = YarnResolutionParser.parseRegistryIdentity(entry.getResolution(), entry.getVersion());
            if (identity.isPresent()) {
                identityByEntry.put(entry, identity.get());
                for (YarnLockEntryId id : entry.getIds()) {
                    if (id.getRawDescriptor() != null) {
                        identityByDescriptor.put(YarnResolutionParser.stripQualifiers(id.getRawDescriptor()), identity.get());
                    }
                }
            }
        }
        return new YarnResolvedIdentities(identityByDescriptor, identityByEntry);
    }

    /**
     * The real package an entry resolves to (every descriptor in the entry's header shares it), if its resolution is registry-backed.
     * The entry must be one of the entries this index was built from.
     */
    public Optional<NameVersion> forEntry(YarnLockEntry entry) {
        return Optional.ofNullable(identityByEntry.get(entry));
    }

    /**
     * The real package a dependency "name: range" (from a package.json or an entry's dependencies block) resolves to.
     */
    public Optional<NameVersion> forDependency(String name, String range) {
        NameVersion identity = identityByDescriptor.get(YarnResolutionParser.stripQualifiers(name + "@" + range));
        if (identity == null && !range.contains(":")) {
            // package.json ranges are written without the protocol that yarn.lock adds ("^1.0.0" -> "npm:^1.0.0")
            identity = identityByDescriptor.get(YarnResolutionParser.stripQualifiers(name + "@" + YarnResolutionParser.NPM_PROTOCOL + range));
        }
        return Optional.ofNullable(identity);
    }
}
