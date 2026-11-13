package com.blackduck.integration.detectable.detectables.yarn;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.util.NameVersion;

/**
 * Real npm package of a Yarn 2+ yarn.lock entry, from its "resolution:" value, e.g.
 * "string-width@npm:4.2.3" or "ms-alias@patch:ms@npm%3A2.1.3#./x.patch" (the patched package).
 * Non-registry resolutions (file:, link:, workspace:, git, ...) yield empty.
 */
public class YarnResolutionParser {
    static final String NPM_PROTOCOL = "npm:";
    private static final String PATCH_PROTOCOL = "patch:";
    private static final int MAX_PATCH_NESTING = 3;

    private YarnResolutionParser() {}

    /**
     * @param resolution   the entry's resolution value, may be null
     * @param entryVersion the entry's resolved "version:" value, used as the version of the identity
     */
    public static Optional<NameVersion> parseRegistryIdentity(@Nullable String resolution, @Nullable String entryVersion) {
        if (resolution == null || entryVersion == null || entryVersion.trim().isEmpty()) {
            return Optional.empty();
        }
        return parse(resolution.trim(), entryVersion.trim(), 0);
    }

    /**
     * Drops yarn's trailing qualifiers from a locator or descriptor, e.g. "::version=2.1.3&hash=abc" or "::locator=...".
     */
    static String stripQualifiers(String locatorOrDescriptor) {
        int qualifiers = locatorOrDescriptor.indexOf("::");
        return qualifiers >= 0 ? locatorOrDescriptor.substring(0, qualifiers) : locatorOrDescriptor;
    }

    private static Optional<NameVersion> parse(String locator, String entryVersion, int depth) {
        String core = stripQualifiers(locator);

        // name@protocol:rest, where the name may be scoped ("@scope/pkg@npm:1.0.0")
        int nameEnd = core.indexOf('@', 1);
        if (nameEnd <= 0) {
            return Optional.empty();
        }
        String name = core.substring(0, nameEnd);
        String rest = core.substring(nameEnd + 1);

        if (rest.startsWith(NPM_PROTOCOL)) {
            return Optional.of(new NameVersion(name, entryVersion));
        }
        if (rest.startsWith(PATCH_PROTOCOL) && depth < MAX_PATCH_NESTING) {
            String inner = rest.substring(PATCH_PROTOCOL.length());
            int patchFile = inner.indexOf('#');
            if (patchFile >= 0) {
                inner = inner.substring(0, patchFile);
            }
            // Only follow the patch when what it patches is itself a registry package.
            return parse(decode(inner), entryVersion, depth + 1);
        }
        return Optional.empty();
    }

    // Yarn percent-encodes ':' '@' '/' inside the nested locator of a patch. A full URL decode is avoided on purpose ('+' in versions).
    private static String decode(String value) {
        return value.replace("%3A", ":").replace("%3a", ":")
            .replace("%40", "@")
            .replace("%2F", "/").replace("%2f", "/");
    }
}
