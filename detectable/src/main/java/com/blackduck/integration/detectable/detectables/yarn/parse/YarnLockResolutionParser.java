package com.blackduck.integration.detectable.detectables.yarn.parse;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.detectable.detectables.yarn.parse.entry.YarnLockEntry;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.YarnLockEntryId;
import com.blackduck.integration.util.NameVersion;

/**
 * The real npm package behind each yarn.lock entry and descriptor ("alias@npm:real@1.0.0"), taken from the entry's "resolution:".
 * Entries without a registry resolution (file:, workspace:, git, ... or Yarn 1) have no identity, so callers keep their existing behavior.
 */
public class YarnLockResolutionParser {
    private static final String NPM_PROTOCOL = "npm:";
    private static final String PATCH_PROTOCOL = "patch:";

    private final Map<String, NameVersion> identityByDescriptor;
    private final Map<YarnLockEntry, NameVersion> identityByEntry;

    private YarnLockResolutionParser(Map<String, NameVersion> identityByDescriptor, Map<YarnLockEntry, NameVersion> identityByEntry) {
        this.identityByDescriptor = identityByDescriptor;
        this.identityByEntry = identityByEntry;
    }

    public static YarnLockResolutionParser from(YarnLock yarnLock) {
        Map<String, NameVersion> identityByDescriptor = new HashMap<>();
        Map<YarnLockEntry, NameVersion> identityByEntry = new IdentityHashMap<>();
        for (YarnLockEntry entry : yarnLock.getEntries()) {
            indexEntry(entry, identityByDescriptor, identityByEntry);
        }
        return new YarnLockResolutionParser(identityByDescriptor, identityByEntry);
    }

    private static void indexEntry(YarnLockEntry entry, Map<String, NameVersion> identityByDescriptor, Map<YarnLockEntry, NameVersion> identityByEntry) {
        parseRegistryIdentity(entry.getResolution(), entry.getVersion()).ifPresent(identity -> {
            identityByEntry.put(entry, identity);
            for (YarnLockEntryId id : entry.getIds()) {
                if (id.getRawDescriptor() != null) {
                    identityByDescriptor.put(stripQualifiers(id.getRawDescriptor()), identity);
                }
            }
        });
    }

    public Optional<NameVersion> forEntry(YarnLockEntry entry) {
        return Optional.ofNullable(identityByEntry.get(entry));
    }

    /**
     * @return the real package for a dependency "name: range" from a package.json or an entry's dependencies block
     */
    public Optional<NameVersion> forDependency(String name, String range) {
        NameVersion identity = identityByDescriptor.get(stripQualifiers(name + "@" + range));
        if (identity == null && !range.contains(":")) {
            // package.json ranges lack the protocol yarn.lock adds ("^1.0.0" -> "npm:^1.0.0")
            identity = identityByDescriptor.get(name + "@" + NPM_PROTOCOL + range);
        }
        return Optional.ofNullable(identity);
    }

    /**
     * Real npm package of a "resolution:" value, e.g. "string-width@npm:4.2.3" or "ms-alias@patch:ms@npm%3A2.1.3#./x.patch"
     * (the patched package). Non-registry resolutions yield empty.
     */
    public static Optional<NameVersion> parseRegistryIdentity(@Nullable String resolution, @Nullable String entryVersion) {
        if (resolution == null || StringUtils.isBlank(entryVersion)) {
            return Optional.empty();
        }
        String locator = stripQualifiers(resolution.trim());
        int nameEnd = locator.indexOf('@', 1); // the name may be scoped ("@scope/pkg@npm:1.0.0")
        if (nameEnd < 0) {
            return Optional.empty();
        }
        String name = locator.substring(0, nameEnd);
        String protocolAndRest = locator.substring(nameEnd + 1);
        if (protocolAndRest.startsWith(NPM_PROTOCOL)) {
            return Optional.of(new NameVersion(name, entryVersion.trim()));
        }
        if (protocolAndRest.startsWith(PATCH_PROTOCOL)) {
            // the patched package is the locator before '#', percent-encoded: "ms@npm%3A2.1.3#./x.patch" -> "ms@npm:2.1.3"
            String patched = StringUtils.substringBefore(protocolAndRest.substring(PATCH_PROTOCOL.length()), "#");
            return parseRegistryIdentity(patched.replace("%3A", ":").replace("%40", "@").replace("%2F", "/"), entryVersion);
        }
        return Optional.empty();
    }

    // yarn.lock keys for patch:/file: carry "::version=...&hash=..." / "::locator=..." qualifiers that dependencies do not
    private static String stripQualifiers(String descriptor) {
        return StringUtils.substringBefore(descriptor, "::");
    }
}
