package com.blackduck.integration.detectable.detectables.yarn.parse.entry;

import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.util.Stringable;

public class YarnLockEntryId extends Stringable {
    private final String name;
    private final String version;
    private final String rawDescriptor;

    public YarnLockEntryId(String name, String version) {
        this(name, version, null);
    }

    /**
     * @param rawDescriptor the descriptor exactly as written in the yarn.lock entry header (e.g. "string-width-alias@npm:string-width@4.2.3"), if known
     */
    public YarnLockEntryId(String name, String version, @Nullable String rawDescriptor) {
        this.name = name;
        this.version = version;
        this.rawDescriptor = rawDescriptor;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    @Nullable
    public String getRawDescriptor() {
        return rawDescriptor;
    }
}
