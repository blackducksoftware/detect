package com.blackduck.integration.detectable.detectables.yarn.parse.entry;

import com.blackduck.integration.util.Stringable;

public class YarnLockEntryId extends Stringable {
    private final String name;
    private final String version;
    private final String rawDescriptor; // as written in the entry header, e.g. "alias@npm:real@1.0.0"; null if unknown

    public YarnLockEntryId(String name, String version) {
        this(name, version, null);
    }

    public YarnLockEntryId(String name, String version, String rawDescriptor) {
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

    public String getRawDescriptor() {
        return rawDescriptor;
    }
}
