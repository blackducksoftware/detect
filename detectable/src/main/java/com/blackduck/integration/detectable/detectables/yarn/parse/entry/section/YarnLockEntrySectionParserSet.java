package com.blackduck.integration.detectable.detectables.yarn.parse.entry.section;

import java.util.ArrayList;
import java.util.List;

import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockLineAnalyzer;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.YarnLockEntryBuilder;

public class YarnLockEntrySectionParserSet {
    private final List<YarnLockEntrySectionParser> yarnLockEntrySectionParsers = new ArrayList<>();

    public YarnLockEntrySectionParserSet(YarnLockLineAnalyzer yarnLockLineAnalyzer, YarnLockDependencySpecParser yarnLockDependencySpecParser) {
        yarnLockEntrySectionParsers.add(new YarnLockHeaderSectionParser(yarnLockLineAnalyzer));
        yarnLockEntrySectionParsers.add(new YarnLockDependencyListSectionParser(yarnLockLineAnalyzer, yarnLockDependencySpecParser, "dependencies", false));
        yarnLockEntrySectionParsers.add(new YarnLockDependencyListSectionParser(yarnLockLineAnalyzer, yarnLockDependencySpecParser, "optionalDependencies", true));
        yarnLockEntrySectionParsers.add(new YarnLockDependencyMetaListSectionParser(yarnLockLineAnalyzer));
        yarnLockEntrySectionParsers.add(new YarnLockKeyValuePairSectionParser(yarnLockLineAnalyzer, "version", YarnLockEntryBuilder::setVersion));
        yarnLockEntrySectionParsers.add(new YarnLockKeyValuePairSectionParser(yarnLockLineAnalyzer, "resolution", YarnLockEntryBuilder::setResolution));
    }

    public int parseSection(YarnLockEntryBuilder entryBuilder, List<String> yarnLockLines, int lineIndexOfStartOfSection) {
        String line = yarnLockLines.get(lineIndexOfStartOfSection);
        if (line.startsWith("#") || line.trim().isEmpty()) {
            return lineIndexOfStartOfSection;
        }
        return yarnLockEntrySectionParsers.stream()
            .filter(ep -> ep.applies(line))
            .findFirst()
            .map(ep -> ep.parseSection(entryBuilder, yarnLockLines, lineIndexOfStartOfSection))
            .orElse(lineIndexOfStartOfSection);
    }
}
