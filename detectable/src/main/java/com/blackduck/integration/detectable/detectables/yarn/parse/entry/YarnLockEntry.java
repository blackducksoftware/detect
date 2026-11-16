package com.blackduck.integration.detectable.detectables.yarn.parse.entry;

import java.util.List;

import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockDependency;
import com.blackduck.integration.util.Stringable;

public class YarnLockEntry extends Stringable {
    private final boolean metadataEntry;
    private final List<YarnLockEntryId> ids;
    private final String version;
    private final List<YarnLockDependency> dependencies;
    private final String resolution;

    public YarnLockEntry(boolean metadataEntry, List<YarnLockEntryId> ids, String version, List<YarnLockDependency> dependencies) {
        this(metadataEntry, ids, version, dependencies, null);
    }

    public YarnLockEntry(boolean metadataEntry, List<YarnLockEntryId> ids, String version, List<YarnLockDependency> dependencies, String resolution) {
        this.metadataEntry = metadataEntry;
        this.ids = ids;
        this.version = version;
        this.dependencies = dependencies;
        this.resolution = resolution;
    }

    public boolean isMetadataEntry() {
        return metadataEntry;
    }

    public List<YarnLockEntryId> getIds() {
        return ids;
    }

    public List<YarnLockDependency> getDependencies() {
        return dependencies;
    }

    public String getVersion() {
        return version;
    }

    public String getResolution() {
        return resolution;
    }
}
