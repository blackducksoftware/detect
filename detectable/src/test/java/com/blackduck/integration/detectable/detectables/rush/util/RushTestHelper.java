package com.blackduck.integration.detectable.detectables.rush.util;

import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockFileProjectIdTransformer;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfileGraphTransformer;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfilePackager;
import com.blackduck.integration.detectable.detectables.yarn.YarnDependencyType;
import com.blackduck.integration.detectable.detectables.yarn.YarnPackager;
import com.blackduck.integration.detectable.detectables.yarn.YarnTransformer;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockLineAnalyzer;
import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockParser;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.YarnLockEntryParser;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.section.YarnLockDependencySpecParser;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.section.YarnLockEntrySectionParserSet;
import com.google.gson.Gson;

public class RushTestHelper {

    public static YarnLockParser createYarnLockParser() {
        YarnLockLineAnalyzer yarnLockLineAnalyzer = new YarnLockLineAnalyzer();
        YarnLockDependencySpecParser yarnLockDependencySpecParser = new YarnLockDependencySpecParser(yarnLockLineAnalyzer);
        YarnLockEntrySectionParserSet yarnLockEntryElementParser = new YarnLockEntrySectionParserSet(yarnLockLineAnalyzer, yarnLockDependencySpecParser);
        YarnLockEntryParser yarnLockEntryParser = new YarnLockEntryParser(yarnLockLineAnalyzer, yarnLockEntryElementParser);
        return new YarnLockParser(yarnLockEntryParser);
    }

    public static YarnPackager createYarnPackager(ExternalIdFactory externalIdFactory) {
        YarnTransformer yarnTransformer = new YarnTransformer(externalIdFactory, EnumListFilter.<YarnDependencyType>excludeNone());
        return new YarnPackager(yarnTransformer);
    }

    public static NpmLockfilePackager createNpmPackager(ExternalIdFactory externalIdFactory, Gson gson) {
        NpmLockfileGraphTransformer npmGraphTransformer = new NpmLockfileGraphTransformer(EnumListFilter.excludeNone());
        NpmLockFileProjectIdTransformer npmProjectIdTransformer = new NpmLockFileProjectIdTransformer(gson, externalIdFactory);
        return new NpmLockfilePackager(gson, externalIdFactory, npmProjectIdTransformer, npmGraphTransformer);
    }

}
